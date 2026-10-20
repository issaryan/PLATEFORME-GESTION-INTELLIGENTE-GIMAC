package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.in.GrilleUseCase;
import cm.gimac.des.monitoring.application.port.in.Vues.AffectationCourte;
import cm.gimac.des.monitoring.application.port.in.Vues.EchangeVue;
import cm.gimac.des.monitoring.application.port.in.Vues.GrilleResume;
import cm.gimac.des.monitoring.application.port.in.Vues.GrilleVue;
import cm.gimac.des.monitoring.application.port.in.Vues.ViolationVue;
import cm.gimac.des.monitoring.application.port.out.DisponibilitesPort;
import cm.gimac.des.monitoring.application.port.out.IntegerProgramSolver;
import cm.gimac.des.monitoring.application.port.out.ReauthentificationPort;
import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.OperatorUnavailability;
import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import cm.gimac.des.monitoring.domain.enumeration.SolverStatus;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.domain.exploitation.FormulationGrille;
import cm.gimac.des.monitoring.domain.exploitation.JsonCanonique;
import cm.gimac.des.monitoring.domain.exploitation.Plages;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Affectation;
import cm.gimac.des.monitoring.domain.exploitation.VerificateurGrille;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.AffectationsRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.EchangeAstreinte;
import cm.gimac.des.monitoring.infrastructure.persistance.EchangesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.GrillesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.IndisponibilitesRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grille d'astreinte hebdomadaire (algorithme 6.45, D-15, docs/lot1/astreintes-monitoring.md §7) : génération par le
 * solveur en nombres entiers, contrôle par le vérificateur indépendant, publication signée par le chef du DES avec
 * instanciation des vacations, échanges entre opérateurs (proposition, accord, approbation).
 */
@Service
@Transactional
public class GrilleService implements GrilleUseCase {

    private final GrillesRepository grilles;
    private final AffectationsRepository affectations;
    private final IndisponibilitesRepository indisponibilites;
    private final EchangesRepository echanges;
    private final DisponibilitesPort disponibilites;
    private final IntegerProgramSolver solveur;
    private final ReauthentificationPort reauth;
    private final Quarts quarts;
    private final OutboxPublisher outbox;
    private final ContraintesGrille contraintes;
    private final CreneauxProgramme creneaux;
    private final ProgrammeGrilles programme;
    private final PublicationProgramme publication;

    public GrilleService(
        GrillesRepository grilles,
        AffectationsRepository affectations,
        IndisponibilitesRepository indisponibilites,
        EchangesRepository echanges,
        DisponibilitesPort disponibilites,
        IntegerProgramSolver solveur,
        ReauthentificationPort reauth,
        Quarts quarts,
        OutboxPublisher outbox,
        ContraintesGrille contraintes,
        CreneauxProgramme creneaux,
        ProgrammeGrilles programme,
        PublicationProgramme publication
    ) {
        this.grilles = grilles;
        this.affectations = affectations;
        this.indisponibilites = indisponibilites;
        this.echanges = echanges;
        this.disponibilites = disponibilites;
        this.solveur = solveur;
        this.reauth = reauth;
        this.quarts = quarts;
        this.outbox = outbox;
        this.contraintes = contraintes;
        this.creneaux = creneaux;
        this.programme = programme;
        this.publication = publication;
    }

    private ZoneId zone() {
        return quarts.reglages().fuseau();
    }

    private static LocalDate lundi(String semaine) {
        String s = semaine == null ? null : semaine.trim().replace("-S", "-W");
        return Plages.lundi(s).orElseThrow(() -> MonitoringException.invalide("semaine", "Semaine ISO attendue (AAAA-Www)"));
    }

    private OnCallRoster lire(UUID id) {
        return grilles.findById(id).orElseThrow(() -> MonitoringException.introuvable("MON_GRILLE_INTROUVABLE", id));
    }

    // ---------------------------------------------------------------- génération

    @Override
    public GrilleVue generer(Appelant appelant, String semaine) {
        LocalDate lundi = lundi(semaine);
        Instant maintenant = quarts.maintenant();
        if (!lundi.plusDays(7).atStartOfDay(zone()).toInstant().isAfter(maintenant)) {
            throw MonitoringException.regle("MON_SEMAINE_PASSEE", "La semaine est passée");
        }
        UUID auteur = appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
        projeterAbsences(lundi);
        OnCallRoster publiee = grilles.findFirstByIsoWeekAndStateOrderByRevisionDesc(Plages.semaineStockee(lundi), RosterState.PUBLIEE).orElse(null);
        CreneauxProgramme.EnVigueur modele = creneaux.enVigueur();
        ProblemeGrille p = programme.probleme(lundi, modele.modele(), publiee, maintenant);
        if (p.occurrences().isEmpty()) {
            throw MonitoringException.regle("MON_CRENEAUX_VIDES", "Aucun créneau actif dans le modèle du programme");
        }
        if (p.operateurs().stream().noneMatch(o -> p.occurrences().stream().anyMatch(o::peut))) {
            throw MonitoringException.regle("MON_AUCUN_OPERATEUR", "Aucune personne éligible aux créneaux du programme");
        }
        FormulationGrille f = FormulationGrille.construire(p);
        IntegerProgramSolver.Resultat r = solveur.resoudre(f.modele(), contraintes.lire().limite());
        if (!r.avecSolution()) {
            throw MonitoringException.regle("MON_GRILLE_SANS_SOLUTION", "Aucune grille trouvée dans le délai", Map.of("statut", r.statut().name()));
        }
        List<Affectation> l = f.affectations(r.valeurs());
        VerificateurGrille.Rapport rapport = VerificateurGrille.verifier(p, l);
        OnCallRoster derniere = grilles.findFirstByIsoWeekOrderByRevisionDesc(Plages.semaineStockee(lundi)).orElse(null);
        OnCallRoster g = new OnCallRoster()
            .isoWeek(Plages.semaineStockee(lundi))
            .revision(derniere == null ? 1 : derniere.getRevision() + 1)
            .state(RosterState.GENEREE)
            .solverStatus(r.statut() == IntegerProgramSolver.Statut.OPTIMALE ? SolverStatus.OPTIMALE : SolverStatus.REALISABLE)
            .objectiveValue(BigDecimal.valueOf(r.objectif()))
            .gap(BigDecimal.valueOf(r.ecart()).setScale(4, java.math.RoundingMode.HALF_UP))
            .solveDuration(Duration.ofMillis(r.dureeMs()))
            .uncoveredSlots(JsonCanonique.ecrire(ProgrammeGrilles.rapportJson(rapport)))
            .generatedAt(maintenant)
            .generatedBy(auteur)
            .previousRevision(derniere);
        grilles.save(g);
        programme.enregistrer(g, l, modele.version() == 0 ? creneaux.versionInitiale(auteur) : modele.version());
        return programme.vue(g);
    }

    /** Absences validées de MS-IAM projetées localement (idempotent par identifiant de disponibilité). */
    private void projeterAbsences(LocalDate lundi) {
        Instant du = lundi.minusDays(1).atStartOfDay(zone()).toInstant();
        Instant au = lundi.plusDays(9).atStartOfDay(zone()).toInstant();
        Instant maintenant = quarts.maintenant();
        for (DisponibilitesPort.Absence a : disponibilites.absencesValidees(du, au)) {
            UUID compte;
            try {
                compte = UUID.fromString(a.compteId());
            } catch (IllegalArgumentException e) {
                continue;
            }
            OperatorUnavailability u = indisponibilites.findByAvailabilityId(a.id()).orElseGet(() -> new OperatorUnavailability().availabilityId(a.id()));
            u.setOperatorId(compte);
            u.setStartsAt(a.debut());
            u.setEndsAt(a.fin());
            u.setValidatedAt(u.getValidatedAt() == null ? maintenant : u.getValidatedAt());
            u.setCancelled(a.annulee());
            indisponibilites.save(u);
        }
    }

    // ---------------------------------------------------------------- lecture

    @Override
    @Transactional(readOnly = true)
    public List<GrilleResume> grilles(Appelant appelant, String semaine) {
        LocalDate lundi = lundi(semaine);
        return grilles
            .findByIsoWeekOrderByRevisionDesc(Plages.semaineStockee(lundi))
            .stream()
            .map(g ->
                new GrilleResume(
                    g.getId(),
                    Plages.semaineAffichee(g.getIsoWeek()),
                    g.getRevision(),
                    g.getState().name(),
                    g.getSolverStatus().name(),
                    ProgrammeGrilles.nonCouvertes(g).size(),
                    g.getGeneratedAt(),
                    g.getPublishedAt()
                )
            )
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GrilleVue grille(Appelant appelant, UUID id) {
        return programme.vue(lire(id));
    }

    @Override
    @Transactional(readOnly = true)
    public GrilleVue publiee(Appelant appelant, String semaine) {
        LocalDate lundi = lundi(semaine);
        return programme.vue(
            grilles
                .findFirstByIsoWeekAndStateOrderByRevisionDesc(Plages.semaineStockee(lundi), RosterState.PUBLIEE)
                .orElseThrow(() -> MonitoringException.introuvable("MON_GRILLE_INTROUVABLE", semaine))
        );
    }

    // ---------------------------------------------------------------- publication

    @Override
    public GrilleVue publier(Appelant appelant, UUID id, Boolean accepterNonCouvertes, String motif, String assertion) {
        OnCallRoster g = lire(id);
        if (g.getState() != RosterState.GENEREE) {
            throw MonitoringException.conflit("MON_GRILLE_NON_PUBLIABLE", "Seule une grille générée se publie");
        }
        int manques = ProgrammeGrilles.nonCouvertes(g).size();
        String m = motif == null || motif.isBlank() ? null : motif.trim();
        if (manques > 0) {
            if (!Boolean.TRUE.equals(accepterNonCouvertes)) {
                throw MonitoringException.regle("MON_GRILLE_NON_COUVERTE", "Plages non couvertes", Map.of("nonCouvertes", manques));
            }
            if (m == null || m.length() < 10 || m.length() > 500) {
                throw MonitoringException.invalide("motif", "Motif de 10 à 500 caractères");
            }
        }
        UUID auteur = appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
        if (assertion == null || assertion.isBlank()) {
            throw MonitoringException.preconditionRequise("MON_REAUTH_EXIGEE", "Réauthentification forte exigée (X-Reauth-Assertion)");
        }
        if (!reauth.verifier(assertion, appelant.id(), ReauthentificationPort.PUBLICATION_GRILLE)) {
            throw MonitoringException.interdit("MON_REAUTH_REFUSEE", "Réauthentification refusée");
        }
        publication.rendrePublique(g, auteur, m);
        return programme.vue(g);
    }

    // ---------------------------------------------------------------- échanges

    @Override
    @Transactional(readOnly = true)
    public List<EchangeVue> echanges(Appelant appelant, UUID grille) {
        lire(grille);
        return vues(echanges.findByGrilleIdOrderByCreeLeDesc(grille));
    }

    private OnCallAssignment affectation(UUID id, OnCallRoster g) {
        return affectations
            .findById(id == null ? new UUID(0, 0) : id)
            .filter(a -> a.getRoster().getId().equals(g.getId()))
            .orElseThrow(() -> MonitoringException.invalide("affectationId", "Affectation inconnue dans cette grille"));
    }

    /** Affectations de la grille après échange des deux occurrences. */
    private List<Affectation> apresEchange(OnCallRoster g, OnCallAssignment a, OnCallAssignment b) {
        List<Affectation> l = new ArrayList<>();
        for (CreneauxProgramme.Placee p : programme.placees(g)) {
            Affectation x = p.affectation();
            UUID op = x.operateur();
            if (p.entite().getId().equals(a.getId())) {
                op = b.getOperatorId();
            } else if (p.entite().getId().equals(b.getId())) {
                op = a.getOperatorId();
            }
            l.add(new Affectation(op, x.code(), x.jour(), x.niveau(), x.chef(), x.debut(), x.fin(), x.semaine(), x.nuit()));
        }
        return l;
    }

    private ProblemeGrille problemeDe(OnCallRoster g) {
        return programme.probleme(ProgrammeGrilles.lundi(g), creneaux.modeleDe(g.getId()), null, quarts.maintenant());
    }

    /** Violations nouvelles créées par l'échange pour les deux opérateurs. */
    private List<ViolationVue> violationsEchange(OnCallRoster g, OnCallAssignment a, OnCallAssignment b) {
        ProblemeGrille p = problemeDe(g);
        Set<VerificateurGrille.Violation> avant = new HashSet<>(VerificateurGrille.verifier(p, programme.affectations(g)).violations());
        Set<UUID> concernes = Set.of(a.getOperatorId(), b.getOperatorId());
        return VerificateurGrille.verifier(p, apresEchange(g, a, b))
            .violations()
            .stream()
            .filter(v -> !avant.contains(v) && (v.operateur() == null || concernes.contains(v.operateur())))
            .map(v -> new ViolationVue(v.code(), v.operateur() == null ? null : v.operateur().toString(), v.jour(), v.creneau()))
            .toList();
    }

    private Instant debut(OnCallRoster g, OnCallAssignment x) {
        return programme.placees(g).stream().filter(p -> p.entite().getId().equals(x.getId())).findFirst().map(p -> p.affectation().debut()).orElseThrow();
    }

    private void controler(OnCallRoster g, OnCallAssignment a, OnCallAssignment b) {
        Instant maintenant = quarts.maintenant();
        if (!debut(g, a).isAfter(maintenant) || !debut(g, b).isAfter(maintenant)) {
            throw MonitoringException.regle("MON_PLAGE_PASSEE", "Un créneau commencé ne s'échange plus");
        }
        List<ViolationVue> v = violationsEchange(g, a, b);
        if (!v.isEmpty()) {
            throw MonitoringException.regle("MON_ECHANGE_CONTRAINTE", "L'échange violerait une contrainte de la grille", Map.of("violations", v));
        }
    }

    @Override
    public EchangeVue proposer(Appelant appelant, UUID grille, UUID affectationId, UUID affectationCibleId, String motif) {
        OnCallRoster g = lire(grille);
        if (g.getState() != RosterState.PUBLIEE) {
            throw MonitoringException.conflit("MON_GRILLE_NON_PUBLIEE", "Seule la grille publiée en vigueur accepte des échanges");
        }
        OnCallAssignment a = affectation(affectationId, g);
        OnCallAssignment b = affectation(affectationCibleId, g);
        if (!a.getOperatorId().toString().equals(appelant.id())) {
            throw MonitoringException.interdit("MON_ECHANGE_INTERDIT", "On ne propose que l'échange de ses propres plages");
        }
        if (a.getOperatorId().equals(b.getOperatorId())) {
            throw MonitoringException.regle("MON_ECHANGE_INVALIDE", "L'échange se fait avec un autre opérateur");
        }
        String m = motif == null || motif.isBlank() ? null : motif.trim();
        if (m != null && m.length() > 500) {
            throw MonitoringException.invalide("motif", "Motif de 500 caractères au plus");
        }
        controler(g, a, b);
        EchangeAstreinte e = new EchangeAstreinte(g.getId(), a.getId(), b.getId(), a.getOperatorId(), b.getOperatorId(), m, quarts.maintenant());
        echanges.save(e);
        notifier(e, List.of(b.getOperatorId().toString()), "ECHANGE_PROPOSE");
        return vues(List.of(e)).get(0);
    }

    private EchangeAstreinte echange(UUID id) {
        return echanges.findById(id).orElseThrow(() -> MonitoringException.introuvable("MON_ECHANGE_INTROUVABLE", id));
    }

    @Override
    public EchangeVue repondre(Appelant appelant, UUID id, boolean accord) {
        EchangeAstreinte e = echange(id);
        if (e.getEtat() != EchangeAstreinte.Etat.PROPOSE) {
            throw MonitoringException.conflit("MON_ECHANGE_ETAT", "Cet échange n'attend plus de réponse");
        }
        if (!e.getCibleId().toString().equals(appelant.id())) {
            throw MonitoringException.interdit("MON_ECHANGE_INTERDIT", "Réservé au titulaire de la plage demandée");
        }
        UUID moi = appelant.uuid().orElseThrow();
        e.decider(accord ? EchangeAstreinte.Etat.ACCEPTE : EchangeAstreinte.Etat.REFUSE, moi, quarts.maintenant());
        notifier(e, accord ? quarts.porteursChefDes() : List.of(e.getDemandeurId().toString()), accord ? "ECHANGE_A_APPROUVER" : "ECHANGE_REFUSE");
        return vues(List.of(e)).get(0);
    }

    @Override
    public EchangeVue decider(Appelant appelant, UUID id, boolean approbation) {
        EchangeAstreinte e = echange(id);
        if (e.getEtat() != EchangeAstreinte.Etat.ACCEPTE) {
            throw MonitoringException.conflit("MON_ECHANGE_ETAT", "Seul un échange accepté par le collègue se décide");
        }
        UUID moi = appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
        Instant maintenant = quarts.maintenant();
        if (!approbation) {
            e.decider(EchangeAstreinte.Etat.REJETE, moi, maintenant);
            notifier(e, List.of(e.getDemandeurId().toString(), e.getCibleId().toString()), "ECHANGE_REJETE");
            return vues(List.of(e)).get(0);
        }
        OnCallRoster g = lire(e.getGrilleId());
        if (g.getState() != RosterState.PUBLIEE) {
            throw MonitoringException.conflit("MON_GRILLE_NON_PUBLIEE", "La grille a été remplacée depuis la proposition");
        }
        OnCallAssignment a = affectation(e.getAffectationId(), g);
        OnCallAssignment b = affectation(e.getAffectationCibleId(), g);
        controler(g, a, b);
        List<Affectation> l = apresEchange(g, a, b);
        VerificateurGrille.Rapport rapport = VerificateurGrille.verifier(problemeDe(g), l);
        OnCallRoster derniere = grilles.findFirstByIsoWeekOrderByRevisionDesc(g.getIsoWeek()).orElse(g);
        OnCallRoster n = new OnCallRoster()
            .isoWeek(g.getIsoWeek())
            .revision(derniere.getRevision() + 1)
            .state(RosterState.GENEREE)
            .solverStatus(g.getSolverStatus())
            .objectiveValue(g.getObjectiveValue())
            .gap(g.getGap())
            .solveDuration(g.getSolveDuration())
            .uncoveredSlots(JsonCanonique.ecrire(ProgrammeGrilles.rapportJson(rapport)))
            .generatedAt(maintenant)
            .generatedBy(moi)
            .previousRevision(g);
        grilles.save(n);
        programme.enregistrer(n, l, creneaux.versionDe(g.getId()));
        publication.rendrePublique(n, moi, null);
        e.decider(EchangeAstreinte.Etat.APPROUVE, moi, maintenant);
        e.setNouvelleGrilleId(n.getId());
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("echangeId", e.getId());
        evt.put("grilleId", n.getId());
        evt.put("demandeurId", e.getDemandeurId());
        evt.put("cibleId", e.getCibleId());
        outbox.publish("OnCallRoster", n.getId(), "monitoring.oncall-swap-approved.v1", evt);
        return vues(List.of(e)).get(0);
    }

    private void notifier(EchangeAstreinte e, List<String> destinataires, String objet) {
        if (destinataires.isEmpty()) {
            return;
        }
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("destinataires", destinataires);
        n.put("niveau", "AVERTISSEMENT");
        n.put("objet", objet);
        n.put("reference", e.getId().toString());
        n.put("lien", "/des/exploitation/astreintes?echange=" + e.getId());
        outbox.publish("OnCallSwap", e.getId(), "monitoring.notification-requested.v1", n);
    }

    private List<EchangeVue> vues(List<EchangeAstreinte> l) {
        Set<String> ids = new HashSet<>();
        Map<UUID, AffectationCourte> affs = new HashMap<>();
        for (EchangeAstreinte e : l) {
            ids.add(e.getDemandeurId().toString());
            ids.add(e.getCibleId().toString());
            if (e.getDecidePar() != null) {
                ids.add(e.getDecidePar().toString());
            }
            if (!affs.containsKey(e.getAffectationId())) {
                grilles.findById(e.getGrilleId()).ifPresent(g ->
                    programme.placees(g).forEach(p -> {
                        Affectation a = p.affectation();
                        affs.put(p.entite().getId(), new AffectationCourte(p.entite().getId(), a.jour(), a.code(), a.debut(), a.fin(), a.niveau()));
                    })
                );
            }
        }
        Map<String, String> noms = quarts.noms(ids);
        return l
            .stream()
            .map(e ->
                new EchangeVue(
                    e.getId(),
                    e.getGrilleId(),
                    e.getEtat().name(),
                    Quarts.personne(e.getDemandeurId(), noms),
                    Quarts.personne(e.getCibleId(), noms),
                    affs.get(e.getAffectationId()),
                    affs.get(e.getAffectationCibleId()),
                    e.getMotif(),
                    e.getCreeLe(),
                    e.getDecideLe(),
                    Quarts.personne(e.getDecidePar(), noms),
                    e.getNouvelleGrilleId()
                )
            )
            .toList();
    }
}
