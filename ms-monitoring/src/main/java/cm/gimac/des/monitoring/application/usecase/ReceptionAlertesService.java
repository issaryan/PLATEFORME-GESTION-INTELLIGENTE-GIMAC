package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.in.ReceptionAlertesUseCase;
import cm.gimac.des.monitoring.application.port.in.Vues.Reception;
import cm.gimac.des.monitoring.application.port.out.AnnuairePort;
import cm.gimac.des.monitoring.domain.Alert;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.AlertState;
import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.domain.exploitation.Empreintes;
import cm.gimac.des.monitoring.domain.exploitation.PolitiqueAlerte;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.AlertesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.EscaladeAlerte;
import cm.gimac.des.monitoring.infrastructure.persistance.EscaladesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.LiaisonsRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Réception des alertes (algorithme 6.44, docs/lot1/astreintes-monitoring.md §4.2), résolution par la source et
 * escalade des racines non acquittées (§4.4, MON-05) vers le chef de quart, le responsable d'astreinte de la grille
 * publiée puis le chef du DES.
 */
@Service
@Transactional
public class ReceptionAlertesService implements ReceptionAlertesUseCase {

    private final AlertesRepository alertes;
    private final LiaisonsRepository liaisons;
    private final EscaladesRepository escalades;
    private final ProgrammeGrilles programme;
    private final AnnuairePort annuaire;
    private final Quarts quarts;
    private final MainCourante mainCourante;
    private final OutboxPublisher outbox;

    public ReceptionAlertesService(
        AlertesRepository alertes,
        LiaisonsRepository liaisons,
        EscaladesRepository escalades,
        ProgrammeGrilles programme,
        AnnuairePort annuaire,
        Quarts quarts,
        MainCourante mainCourante,
        OutboxPublisher outbox
    ) {
        this.alertes = alertes;
        this.liaisons = liaisons;
        this.escalades = escalades;
        this.programme = programme;
        this.annuaire = annuaire;
        this.quarts = quarts;
        this.mainCourante = mainCourante;
        this.outbox = outbox;
    }

    private static String texte(String v, String champ, int max) {
        String s = v == null ? "" : v.trim();
        if (s.isEmpty() || s.length() > max) {
            throw MonitoringException.invalide(champ, "Champ absent ou trop long : " + champ);
        }
        return s;
    }

    @Override
    public Reception recevoir(String source, String liaison, String regle, String gravite, Instant survenueLe) {
        String src = texte(source, "source", 100);
        String code = texte(liaison, "liaison", 50);
        String r = texte(regle, "regle", 255);
        AlertSeverity g = SupervisionService.enumeration(AlertSeverity.class, gravite, "gravite");
        if (g == null) {
            throw MonitoringException.invalide("gravite", "Gravité absente");
        }
        SupervisedLink l = liaisons.findByCode(code).orElseThrow(() -> MonitoringException.regle("MON_LIAISON_INCONNUE", "Liaison inconnue"));
        Instant t = survenueLe == null ? quarts.maintenant() : survenueLe;
        String empreinte = Empreintes.sha256(src, code, r);
        Optional<Alert> existante = alertes.ouvertesParEmpreinte(empreinte, t.minus(quarts.reglages().deduplication())).stream().findFirst();
        if (existante.isPresent()) {
            Alert a = existante.orElseThrow();
            a.setOccurrences((a.getOccurrences() == null ? 1 : a.getOccurrences()) + 1);
            if (t.isAfter(a.getLastSeenAt())) {
                a.setLastSeenAt(t);
            }
            AlertSeverity avant = a.getSeverity();
            a.setSeverity(PolitiqueAlerte.laPlusGrave(avant, g));
            if (a.getSeverity() != avant && a.getCorrelationRoot() == null) {
                if (a.getState() == AlertState.OUVERTE) {
                    a.setAckDeadline(a.getFirstSeenAt().plus(quarts.politique(a.getSeverity()).delai()));
                }
                leve(a);
            }
            return new Reception(a.getId(), a.getReference(), false);
        }
        Alert a = new Alert()
            .reference(reference(t))
            .source(src)
            .rule(r)
            .fingerprint(empreinte)
            .severity(g)
            .state(AlertState.OUVERTE)
            .firstSeenAt(t)
            .lastSeenAt(t)
            .occurrences(1)
            .overdue(false)
            .link(l);
        SupervisedLink parent = l.getParentPlatform();
        Alert racine = parent == null ? null : alertes.racinesOuvertes(parent.getId(), t.minus(quarts.reglages().correlation())).stream().findFirst().orElse(null);
        if (racine != null) {
            a.setCorrelationRoot(racine);
            alertes.save(a);
            return new Reception(a.getId(), a.getReference(), true);
        }
        a.setAckDeadline(t.plus(quarts.politique(g).delai()));
        alertes.save(a);
        leve(a);
        return new Reception(a.getId(), a.getReference(), true);
    }

    /** ALR-2026-00001 : année et rang dans l'année. */
    private String reference(Instant t) {
        String prefixe = "ALR-" + t.atZone(ZoneOffset.UTC).getYear() + "-";
        return prefixe + String.format(Locale.ROOT, "%05d", alertes.countByReferenceStartingWith(prefixe) + 1);
    }

    /** monitoring.alert-raised.v1 et demande de notification à la vacation en cours (tableau 6.48). */
    private void leve(Alert a) {
        PolitiqueAlerte pol = quarts.politique(a.getSeverity());
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("alerteId", a.getId());
        evt.put("reference", a.getReference());
        evt.put("liaison", a.getLink().getCode());
        evt.put("institutionId", a.getLink().getInstitutionId());
        evt.put("gravite", a.getSeverity().name());
        evt.put("echeance", a.getAckDeadline());
        outbox.publish("Alert", a.getId(), "monitoring.alert-raised.v1", evt);
        List<String> destinataires = quarts.enCours().map(v -> List.copyOf(Quarts.membres(v))).orElse(List.of());
        notifier(a, destinataires, pol.niveau().name(), "ALERTE_LEVEE");
    }

    private void notifier(Alert a, List<String> destinataires, String niveau, String objet) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("destinataires", destinataires);
        n.put("niveau", niveau);
        n.put("objet", objet);
        n.put("reference", a.getReference());
        n.put("lien", "/des/exploitation/supervision?alerte=" + a.getId());
        outbox.publish("Alert", a.getId(), "monitoring.notification-requested.v1", n);
    }

    @Override
    public int resoudre(String source, String liaison, String regle) {
        String empreinte = Empreintes.sha256(texte(source, "source", 100), texte(liaison, "liaison", 50), texte(regle, "regle", 255));
        Instant maintenant = quarts.maintenant();
        int n = 0;
        for (Alert a : alertes.nonResoluesParEmpreinte(empreinte)) {
            List<Alert> l = new ArrayList<>();
            l.add(a);
            l.addAll(alertes.findByCorrelationRootId(a.getId()).stream().filter(s -> s.getState() != AlertState.RESOLUE).toList());
            for (Alert x : l) {
                x.setState(AlertState.RESOLUE);
                x.setResolvedAt(maintenant);
                n++;
                Map<String, Object> evt = new LinkedHashMap<>();
                evt.put("alerteId", x.getId());
                evt.put("reference", x.getReference());
                evt.put("liaison", x.getLink().getCode());
                outbox.publish("Alert", x.getId(), "monitoring.alert-resolved.v1", evt);
            }
            quarts
                .enCours()
                .ifPresent(v ->
                    mainCourante.ajouter(v, MainCourante.SYSTEME, ShiftLogNature.RETABLISSEMENT, "Résolution par la source de " + a.getReference(), a.getReference(), null, maintenant)
                );
        }
        return n;
    }

    @Override
    public int escalader() {
        Instant maintenant = quarts.maintenant();
        int n = 0;
        for (Alert a : alertes.racinesEchues(maintenant)) {
            PolitiqueAlerte pol = quarts.politique(a.getSeverity());
            int du = pol.palierDu(a.getAckDeadline(), maintenant);
            EscaladeAlerte e = escalades.findById(a.getId()).orElseGet(() -> new EscaladeAlerte(a.getId()));
            if (du <= e.getPalier()) {
                continue;
            }
            Destinataires d = destinataires(du);
            e.setPalier(d.palier());
            e.setEscaladeLe(maintenant);
            e.setPlageNonCouverte(e.isPlageNonCouverte() || d.nonCouverte());
            escalades.save(e);
            a.setOverdue(true);
            n++;
            Map<String, Object> evt = new LinkedHashMap<>();
            evt.put("alerteId", a.getId());
            evt.put("reference", a.getReference());
            evt.put("palier", d.palier());
            evt.put("destinataires", d.ids());
            evt.put("planNonCouvert", d.nonCouverte());
            outbox.publish("Alert", a.getId(), "monitoring.alert-escalated.v1", evt);
            notifier(a, d.ids(), d.palier() >= 2 ? pol.niveau().name() : "URGENT", "ALERTE_ESCALADEE");
            quarts
                .enCours()
                .ifPresent(v ->
                    mainCourante.ajouter(
                        v,
                        MainCourante.SYSTEME,
                        ShiftLogNature.ALERTE,
                        "Escalade de " + a.getReference() + " au palier " + d.palier() + (d.nonCouverte() ? " (plage sans responsable d'astreinte publié)" : ""),
                        a.getReference(),
                        null,
                        maintenant
                    )
                );
        }
        return n;
    }

    record Destinataires(int palier, List<String> ids, boolean nonCouverte) {}

    /** Destinataires d'un palier ; palier sans destinataire : palier suivant, absence de couverture signalée. */
    Destinataires destinataires(int palier) {
        boolean nonCouverte = false;
        if (palier == 1) {
            Optional<Shift> v = quarts.enCours();
            if (v.isPresent() && v.orElseThrow().getShiftLeadId() != null) {
                return new Destinataires(1, List.of(v.orElseThrow().getShiftLeadId().toString()), false);
            }
            nonCouverte = true;
            palier = 2;
        }
        if (palier == 2) {
            List<String> astreinte = responsablesAstreinte();
            if (!astreinte.isEmpty()) {
                return new Destinataires(2, astreinte, nonCouverte);
            }
            nonCouverte = true;
        }
        List<String> chefs = annuaire.porteurs(Appelant.PUBLIER_GRILLE).stream().map(AnnuairePort.Personne::id).toList();
        return new Destinataires(3, chefs, nonCouverte);
    }

    /** Personnes de niveau 2 de service à l'instant, selon le programme publié (créneaux configurables, D-117). */
    private List<String> responsablesAstreinte() {
        return programme.astreinteA(quarts.maintenant());
    }
}
