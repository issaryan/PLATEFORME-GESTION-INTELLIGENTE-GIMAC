package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux.Occurrence;
import cm.gimac.des.monitoring.domain.exploitation.Plages;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Affectation;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.GrillesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.VacationsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Publication d'un programme (D-114, D-117 §11.6) : la grille remplace la précédente de la semaine, les vacations des
 * créneaux quotidiens de niveau 1 sont instanciées, l'événement de publication part et chaque personne programmée est
 * notifiée avec un lien vers son programme.
 */
@Component
class PublicationProgramme {

    private final GrillesRepository grilles;
    private final VacationsRepository vacations;
    private final MainCourante mainCourante;
    private final Quarts quarts;
    private final OutboxPublisher outbox;
    private final CreneauxProgramme creneaux;
    private final ProgrammeGrilles programme;

    PublicationProgramme(
        GrillesRepository grilles,
        VacationsRepository vacations,
        MainCourante mainCourante,
        Quarts quarts,
        OutboxPublisher outbox,
        CreneauxProgramme creneaux,
        ProgrammeGrilles programme
    ) {
        this.grilles = grilles;
        this.vacations = vacations;
        this.mainCourante = mainCourante;
        this.quarts = quarts;
        this.outbox = outbox;
        this.creneaux = creneaux;
        this.programme = programme;
    }

    private ZoneId zone() {
        return quarts.reglages().fuseau();
    }

    void rendrePublique(OnCallRoster g, UUID auteur, String motif) {
        Instant maintenant = quarts.maintenant();
        grilles
            .findByIsoWeekOrderByRevisionDesc(g.getIsoWeek())
            .stream()
            .filter(x -> x.getState() == RosterState.PUBLIEE && !x.getId().equals(g.getId()))
            .forEach(x -> x.setState(RosterState.REMPLACEE));
        g.setState(RosterState.PUBLIEE);
        g.setValidatedBy(auteur);
        g.setValidatedAt(maintenant);
        g.setPublishedAt(maintenant);
        grilles.flush();
        List<Affectation> l = programme.affectations(g);
        instancierVacations(g, l, auteur, maintenant);
        String semaine = Plages.semaineAffichee(g.getIsoWeek());
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("grilleId", g.getId());
        evt.put("semaine", semaine);
        evt.put("revision", g.getRevision());
        evt.put(
            "affectations",
            l
                .stream()
                .map(a -> {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("operateurId", a.operateur().toString());
                    x.put("date", a.jour().toString());
                    x.put("creneau", a.code());
                    x.put("debut", a.debut().toString());
                    x.put("fin", a.fin().toString());
                    x.put("niveau", a.niveau());
                    x.put("chefDeQuart", a.chef());
                    return x;
                })
                .toList()
        );
        evt.put("nonCouvertes", ProgrammeGrilles.nonCouvertes(g).size());
        evt.put("motif", motif);
        outbox.publish("OnCallRoster", g.getId(), g.getRevision() > 1 ? "monitoring.oncall-revised.v1" : "monitoring.oncall-published.v1", evt);
        // Chaque personne programmée est notifiée (D-117 §11.6) : lien vers son programme de la semaine.
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("destinataires", l.stream().map(a -> a.operateur().toString()).distinct().toList());
        n.put("niveau", "AVERTISSEMENT");
        n.put("objet", g.getRevision() > 1 ? "GRILLE_REVISEE" : "GRILLE_PUBLIEE");
        n.put("objetId", g.getId().toString());
        n.put("reference", semaine);
        n.put("acteurId", auteur.toString());
        n.put("le", maintenant.toString());
        n.put("lien", "/des/exploitation/astreintes?semaine=" + semaine + "&vue=moi");
        outbox.publish("OnCallRoster", g.getId(), "monitoring.notification-requested.v1", n);
    }

    /**
     * Vacations des occurrences quotidiennes de niveau 1 non commencées, (ré)instanciées depuis la grille ; chef de
     * quart désigné ; à défaut, la personne qui publie si le créneau exige un chef de quart (anomalie consignée, MON-13),
     * sinon la première personne affectée.
     */
    private void instancierVacations(OnCallRoster g, List<Affectation> l, UUID publieur, Instant maintenant) {
        LocalDate lundi = ProgrammeGrilles.lundi(g);
        for (Occurrence o : creneaux.modeleDe(g.getId()).occurrences(lundi, zone())) {
            if (o.niveau() != 1 || o.creneau().portee() != ModeleCreneaux.Portee.JOUR || !o.debut().isAfter(maintenant)) {
                continue;
            }
            List<Affectation> ici = l.stream().filter(a -> a.cle().equals(o.cle())).toList();
            UUID chef = ici.stream().filter(Affectation::chef).map(Affectation::operateur).findFirst().orElse(null);
            // MON-13 : créneau qui exige un chef de quart sans chef désigné, la personne qui publie tient la vacation.
            UUID tenant = chef != null ? chef : o.creneau().chefDeQuart() ? publieur : ici.stream().map(Affectation::operateur).findFirst().orElse(publieur);
            String ref = reference(o);
            Shift v = vacations.findByReference(ref).orElseGet(Shift::new);
            v.setReference(ref);
            v.setShiftDate(o.jour());
            v.setSlot(Plages.creneau(o.debut(), zone()).plage());
            v.setStartsAt(o.debut());
            v.setEndsAt(o.fin());
            UUID t = tenant;
            v.setOperatorIds(Quarts.joindre(ici.stream().map(Affectation::operateur).filter(x -> !x.equals(t)).toList()));
            v.setShiftLeadId(tenant);
            v.setRoster(g);
            vacations.saveAndFlush(v);
            creneaux.creneauVacation(v.getId(), o.code());
            if (chef == null && o.creneau().chefDeQuart()) {
                mainCourante.ajouter(
                    v,
                    MainCourante.SYSTEME,
                    ShiftLogNature.EVENEMENT,
                    "Aucun chef de quart désigné par la grille (révision " + g.getRevision() + ")",
                    g.getIsoWeek(),
                    null,
                    maintenant
                );
            }
        }
    }

    /** Référence d'une vacation : VAC-2026-1019-NUIT ; format de D-114 (VAC-2026-1019-N) pour les plages historiques. */
    static String reference(Occurrence o) {
        for (cm.gimac.des.monitoring.domain.enumeration.ShiftSlot s : cm.gimac.des.monitoring.domain.enumeration.ShiftSlot.values()) {
            if (s.name().equals(o.code())) {
                return Plages.referenceVacation(o.jour(), s);
            }
        }
        return "VAC-" + o.jour().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MMdd")) + "-" + o.code();
    }
}
