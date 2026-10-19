package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.in.Page;
import cm.gimac.des.monitoring.application.port.in.SupervisionUseCase;
import cm.gimac.des.monitoring.application.port.in.Vues.AcquittementVue;
import cm.gimac.des.monitoring.application.port.in.Vues.AlerteVue;
import cm.gimac.des.monitoring.application.port.in.Vues.LiaisonCourte;
import cm.gimac.des.monitoring.application.port.in.Vues.LiaisonVue;
import cm.gimac.des.monitoring.application.port.in.Vues.PolitiqueVue;
import cm.gimac.des.monitoring.application.port.in.Vues.RacineVue;
import cm.gimac.des.monitoring.application.port.in.Vues.Reference;
import cm.gimac.des.monitoring.domain.Alert;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.AlertState;
import cm.gimac.des.monitoring.domain.enumeration.LinkState;
import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.domain.exploitation.PolitiqueAlerte;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.AlertesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.EscaladeAlerte;
import cm.gimac.des.monitoring.infrastructure.persistance.EscaladesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.LiaisonsRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Liaisons supervisées et alertes (docs/lot1/astreintes-monitoring.md §3 et §4) : lecture triée et paginée,
 * acquittement dans le délai de la gravité réservé à la vacation en cours (MON-04), ticket obligatoire pour une
 * alerte critique racine (MON-06), entrée automatique dans la main courante.
 */
@Service
@Transactional
public class SupervisionService implements SupervisionUseCase {

    static final int TAILLE_DEFAUT = 20;
    static final int TAILLE_MAX = 100;

    private final LiaisonsRepository liaisons;
    private final AlertesRepository alertes;
    private final EscaladesRepository escalades;
    private final Quarts quarts;
    private final MainCourante mainCourante;
    private final OutboxPublisher outbox;
    private final JournalRefus refus;
    private final TransactionTemplate tx;

    public SupervisionService(
        LiaisonsRepository liaisons,
        AlertesRepository alertes,
        EscaladesRepository escalades,
        Quarts quarts,
        MainCourante mainCourante,
        OutboxPublisher outbox,
        JournalRefus refus,
        PlatformTransactionManager transactions
    ) {
        this.tx = new TransactionTemplate(transactions);
        this.liaisons = liaisons;
        this.alertes = alertes;
        this.escalades = escalades;
        this.quarts = quarts;
        this.mainCourante = mainCourante;
        this.outbox = outbox;
        this.refus = refus;
    }

    static PageRequest pagination(Integer page, Integer taille) {
        int p = page == null ? 0 : page;
        int t = taille == null ? TAILLE_DEFAUT : taille;
        if (p < 0 || t < 1 || t > TAILLE_MAX) {
            throw MonitoringException.invalide("page", "Pagination invalide");
        }
        return PageRequest.of(p, t);
    }

    static <E extends Enum<E>> E enumeration(Class<E> type, String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, valeur.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw MonitoringException.invalide(champ, "Valeur inconnue : " + champ);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LiaisonVue> liaisons(Appelant appelant, String etat, String recherche, Integer page, Integer taille) {
        LinkState e = enumeration(LinkState.class, etat, "etat");
        String r = recherche == null || recherche.isBlank() ? null : "%" + recherche.trim().toLowerCase(Locale.ROOT) + "%";
        var res = liaisons.rechercher(e, r, pagination(page, taille));
        Map<UUID, Long> ouvertes = ouvertesParLiaison();
        return new Page<>(res.getContent().stream().map(l -> vue(l, ouvertes)).toList(), res.getTotalElements(), res.getNumber(), res.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public LiaisonVue liaison(Appelant appelant, UUID id) {
        SupervisedLink l = liaisons.findById(id).orElseThrow(() -> MonitoringException.introuvable("MON_LIAISON_INTROUVABLE", id));
        return vue(l, ouvertesParLiaison());
    }

    private Map<UUID, Long> ouvertesParLiaison() {
        Map<UUID, Long> m = new HashMap<>();
        for (Object[] r : alertes.ouvertesParLiaison()) {
            m.put((UUID) r[0], (Long) r[1]);
        }
        return m;
    }

    static LiaisonVue vue(SupervisedLink l, Map<UUID, Long> ouvertes) {
        SupervisedLink p = l.getParentPlatform();
        Duration periode = l.getMeasurePeriod();
        return new LiaisonVue(
            l.getId(),
            l.getCode(),
            l.getLabel(),
            l.getScope() == null ? null : l.getScope().name(),
            l.getInstitutionId(),
            l.getType(),
            l.getState() == null ? null : l.getState().name(),
            l.getStateSince(),
            l.getLastMeasureAt(),
            periode == null ? null : periode.toSeconds(),
            l.getMaintenanceUntil(),
            p == null ? null : new Reference(p.getId(), p.getCode()),
            ouvertes.getOrDefault(l.getId(), 0L)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlerteVue> alertes(Appelant appelant, String etat, String gravite, UUID liaison, Integer page, Integer taille) {
        AlertState e = enumeration(AlertState.class, etat, "etat");
        AlertSeverity g = enumeration(AlertSeverity.class, gravite, "gravite");
        var res = alertes.rechercher(e, g, liaison, pagination(page, taille));
        List<AlerteVue> vues = vues(res.getContent());
        return new Page<>(vues, res.getTotalElements(), res.getNumber(), res.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AlerteVue alerte(Appelant appelant, UUID id) {
        return vues(List.of(lire(id))).get(0);
    }

    private Alert lire(UUID id) {
        return alertes.lire(id).orElseThrow(() -> MonitoringException.introuvable("MON_ALERTE_INTROUVABLE", id));
    }

    List<AlerteVue> vues(List<Alert> l) {
        Map<UUID, EscaladeAlerte> esc = escalades
            .findAllById(l.stream().map(Alert::getId).toList())
            .stream()
            .collect(Collectors.toMap(EscaladeAlerte::getAlerteId, Function.identity()));
        Set<String> ids = new HashSet<>();
        l.forEach(a -> {
            if (a.getAcknowledgedBy() != null) {
                ids.add(a.getAcknowledgedBy().toString());
            }
        });
        Map<String, String> noms = ids.isEmpty() ? Map.of() : quarts.noms(ids);
        return l.stream().map(a -> vue(a, esc.get(a.getId()), noms)).toList();
    }

    private AlerteVue vue(Alert a, EscaladeAlerte e, Map<String, String> noms) {
        PolitiqueAlerte pol = quarts.politique(a.getSeverity());
        Alert racine = a.getCorrelationRoot();
        boolean horsDelaiAck = a.getAcknowledgedAt() != null && a.getAckDeadline() != null && a.getAcknowledgedAt().isAfter(a.getAckDeadline());
        AcquittementVue ack = a.getAcknowledgedAt() == null
            ? null
            : new AcquittementVue(Quarts.personne(a.getAcknowledgedBy(), noms), a.getAcknowledgedAt(), a.getAckComment(), Boolean.TRUE.equals(a.getTicketRequested()), horsDelaiAck);
        boolean horsDelai = Boolean.TRUE.equals(a.getOverdue()) ||
        (a.getState() == AlertState.OUVERTE && a.getAckDeadline() != null && quarts.maintenant().isAfter(a.getAckDeadline())) ||
        horsDelaiAck;
        SupervisedLink l = a.getLink();
        return new AlerteVue(
            a.getId(),
            a.getReference(),
            l == null ? null : new LiaisonCourte(l.getId(), l.getCode(), l.getLabel()),
            a.getSource(),
            a.getRule(),
            a.getSeverity().name(),
            a.getState().name(),
            a.getFirstSeenAt(),
            a.getLastSeenAt(),
            a.getOccurrences() == null ? 1 : a.getOccurrences(),
            pol.delai().toMinutes(),
            a.getAckDeadline(),
            horsDelai,
            e == null ? 0 : e.getPalier(),
            e == null ? null : e.getEscaladeLe(),
            racine == null ? null : new RacineVue(racine.getId(), racine.getReference()),
            racine == null ? alertes.nombreDeSymptomes(a.getId()) : 0,
            ack,
            a.getTicketId(),
            a.getResolvedAt()
        );
    }

    /**
     * Acquittement : la transaction est ouverte ici, pour que le refus « hors vacation » soit journalisé une fois la
     * transaction de l'acte annulée et sa connexion rendue.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AlerteVue acquitter(Appelant appelant, UUID id, String commentaire, Boolean ticket) {
        try {
            return tx.execute(st -> acquitterDansLaTransaction(appelant, id, commentaire, ticket));
        } catch (HorsVacation e) {
            refus.acquittementRefuse(appelant, e.alerte);
            throw MonitoringException.interdit("MON_HORS_VACATION", "Réservé aux membres de la vacation en cours");
        }
    }

    /** Refus hors vacation, levé dans la transaction et journalisé après elle. */
    private static final class HorsVacation extends RuntimeException {

        private final transient Alert alerte;

        HorsVacation(Alert alerte) {
            super("hors vacation", null, false, false);
            this.alerte = alerte;
        }
    }

    private AlerteVue acquitterDansLaTransaction(Appelant appelant, UUID id, String commentaire, Boolean ticket) {
        String c = commentaire == null ? "" : commentaire.trim();
        if (c.length() < 3 || c.length() > 500) {
            throw MonitoringException.invalide("commentaire", "Commentaire de 3 à 500 caractères");
        }
        UUID auteur = appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
        Alert a = lire(id);
        if (a.getState() == AlertState.RESOLUE) {
            throw MonitoringException.conflit("MON_ALERTE_RESOLUE", "Alerte déjà résolue");
        }
        if (a.getState() == AlertState.ACQUITTEE) {
            throw MonitoringException.conflit("MON_ALERTE_DEJA_ACQUITTEE", "Alerte déjà acquittée");
        }
        if (a.getCorrelationRoot() != null) {
            throw MonitoringException.conflit("MON_ALERTE_SYMPTOME", "Acquittez l'alerte racine, qui couvre ses symptômes");
        }
        Shift vacation = quarts.enCours().filter(v -> Quarts.membre(v, appelant.id())).orElse(null);
        if (vacation == null) {
            throw new HorsVacation(a);
        }
        boolean demandeTicket = Boolean.TRUE.equals(ticket);
        if (a.getSeverity() == AlertSeverity.CRITIQUE && !demandeTicket) {
            throw MonitoringException.regle("MON_TICKET_OBLIGATOIRE", "Une alerte critique racine s'acquitte avec une demande de ticket");
        }
        Instant maintenant = quarts.maintenant();
        boolean horsDelai = a.getAckDeadline() != null && maintenant.isAfter(a.getAckDeadline());
        for (Alert x : concernees(a)) {
            x.setState(AlertState.ACQUITTEE);
            x.setAcknowledgedBy(auteur);
            x.setAcknowledgedAt(maintenant);
            x.setTicketRequested(demandeTicket);
            x.setAckComment(c);
            if (horsDelai) {
                x.setOverdue(true);
            }
        }
        mainCourante.ajouter(
            vacation,
            auteur,
            horsDelai ? ShiftLogNature.ACQUITTEMENT_HORS_DELAI : ShiftLogNature.ALERTE,
            "Acquittement de " + a.getReference() + " (" + a.getSeverity().name() + ")" + (demandeTicket ? ", ticket demandé" : "") + " : " + c,
            a.getReference(),
            null,
            maintenant
        );
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("alerteId", a.getId());
        evt.put("reference", a.getReference());
        evt.put("liaison", a.getLink().getCode());
        evt.put("institutionId", a.getLink().getInstitutionId());
        evt.put("gravite", a.getSeverity().name());
        evt.put("ticketDemande", demandeTicket);
        evt.put("delaiEcouleSecondes", Duration.between(a.getFirstSeenAt(), maintenant).toSeconds());
        evt.put("horsDelai", horsDelai);
        evt.put("par", appelant.id());
        outbox.publish("Alert", a.getId(), "monitoring.alert-acknowledged.v1", evt);
        return vues(List.of(a)).get(0);
    }

    /** L'alerte et ses symptômes non résolus. */
    private List<Alert> concernees(Alert racine) {
        List<Alert> l = new java.util.ArrayList<>();
        l.add(racine);
        alertes.findByCorrelationRootId(racine.getId()).stream().filter(s -> s.getState() != AlertState.RESOLUE).forEach(l::add);
        return l;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolitiqueVue> politiques() {
        return quarts
            .politiques()
            .stream()
            .map(p -> new PolitiqueVue(p.gravite().name(), p.delai().toMinutes(), p.niveau().name(), p.canalUrgent(), p.ticket().name()))
            .toList();
    }
}
