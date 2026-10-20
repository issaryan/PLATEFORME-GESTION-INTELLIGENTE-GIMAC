package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.in.GrilleUseCase;
import cm.gimac.des.monitoring.application.port.in.Vues.AffectationCourte;
import cm.gimac.des.monitoring.application.port.in.Vues.AffectationVue;
import cm.gimac.des.monitoring.application.port.in.Vues.ChargeVue;
import cm.gimac.des.monitoring.application.port.in.Vues.EchangeVue;
import cm.gimac.des.monitoring.application.port.in.Vues.GrilleResume;
import cm.gimac.des.monitoring.application.port.in.Vues.GrilleVue;
import cm.gimac.des.monitoring.application.port.in.Vues.NonCouverteVue;
import cm.gimac.des.monitoring.application.port.in.Vues.ViolationVue;
import cm.gimac.des.monitoring.application.port.out.AnnuairePort;
import cm.gimac.des.monitoring.application.port.out.DisponibilitesPort;
import cm.gimac.des.monitoring.application.port.out.IntegerProgramSolver;
import cm.gimac.des.monitoring.application.port.out.ReauthentificationPort;
import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.OperatorUnavailability;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import cm.gimac.des.monitoring.domain.enumeration.SolverStatus;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.domain.exploitation.FormulationGrille;
import cm.gimac.des.monitoring.domain.exploitation.JsonCanonique;
import cm.gimac.des.monitoring.domain.exploitation.Plages;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Affectation;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Indisponibilite;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Operateur;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Qualification;
import cm.gimac.des.monitoring.domain.exploitation.VerificateurGrille;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.AffectationsRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.EchangeAstreinte;
import cm.gimac.des.monitoring.infrastructure.persistance.EchangesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.GrillesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.IndisponibilitesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.VacationsRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final VacationsRepository vacations;
    private final EchangesRepository echanges;
    private final AnnuairePort annuaire;
    private final DisponibilitesPort disponibilites;
    private final IntegerProgramSolver solveur;
    private final ReauthentificationPort reauth;
    private final MainCourante mainCourante;
    private final Quarts quarts;
    private final OutboxPublisher outbox;
    private final ContraintesGrille contraintes;

    public GrilleService(
        GrillesRepository grilles,
        AffectationsRepository affectations,
        IndisponibilitesRepository indisponibilites,
        VacationsRepository vacations,
        EchangesRepository echanges,
        AnnuairePort annuaire,
        DisponibilitesPort disponibilites,
        IntegerProgramSolver solveur,
        ReauthentificationPort reauth,
        MainCourante mainCourante,
        Quarts quarts,
        OutboxPublisher outbox,
        ContraintesGrille contraintes
    ) {
        this.grilles = grilles;
        this.affectations = affectations;
        this.indisponibilites = indisponibilites;
        this.vacations = vacations;
        this.echanges = echanges;
        this.annuaire = annuaire;
        this.disponibilites = disponibilites;
        this.solveur = solveur;
        this.reauth = reauth;
        this.mainCourante = mainCourante;
        this.quarts = quarts;
        this.outbox = outbox;
        this.contraintes = contraintes;
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
        if (!Plages.debut(lundi.plusDays(7), ShiftSlot.MATIN, zone()).isAfter(maintenant)) {
            throw MonitoringException.regle("MON_SEMAINE_PASSEE", "La semaine est passée");
        }
        UUID auteur = appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
        projeterAbsences(lundi);
        OnCallRoster publiee = grilles.findFirstByIsoWeekAndStateOrderByRevisionDesc(Plages.semaineStockee(lundi), RosterState.PUBLIEE).orElse(null);
        ProblemeGrille p = probleme(lundi, publiee, maintenant);
        if (p.operateurs().stream().noneMatch(o -> o.peut(1))) {
            throw MonitoringException.regle("MON_AUCUN_OPERATEUR", "Aucun opérateur qualifié (MON_SHIFT_OPERATOR)");
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
            .uncoveredSlots(JsonCanonique.ecrire(rapportJson(rapport)))
            .generatedAt(maintenant)
            .generatedBy(auteur)
            .previousRevision(derniere);
        grilles.save(g);
        enregistrer(g, l);
        return vue(g);
    }

    /** Absences validées de MS-IAM projetées localement (idempotent par identifiant de disponibilité). */
    private void projeterAbsences(LocalDate lundi) {
        Instant du = Plages.debut(lundi.minusDays(1), ShiftSlot.MATIN, zone());
        Instant au = Plages.fin(lundi.plusDays(6), ShiftSlot.NUIT, zone());
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

    /** Données du problème : opérateurs qualifiés, absences, veille figée, plages commencées figées en révision. */
    ProblemeGrille probleme(LocalDate lundi, OnCallRoster publiee, Instant maintenant) {
        Map<String, Set<Qualification>> quals = new LinkedHashMap<>();
        annuaire.porteurs(Appelant.OPERATEUR).forEach(p -> quals.computeIfAbsent(p.id(), k -> EnumSet.noneOf(Qualification.class)).add(Qualification.SALLE));
        annuaire.porteurs(Appelant.ASTREINTE).forEach(p -> quals.computeIfAbsent(p.id(), k -> EnumSet.noneOf(Qualification.class)).add(Qualification.ASTREINTE));
        annuaire.porteurs(Appelant.CHEF_DE_QUART).forEach(p -> quals.computeIfAbsent(p.id(), k -> EnumSet.noneOf(Qualification.class)).add(Qualification.CHEF));
        List<Operateur> ops = new ArrayList<>();
        quals.forEach((id, q) -> {
            try {
                ops.add(new Operateur(UUID.fromString(id), q));
            } catch (IllegalArgumentException e) {
                // compte sans identifiant UUID : ignoré
            }
        });
        Set<Indisponibilite> indispo = new HashSet<>();
        Instant du = Plages.debut(lundi, ShiftSlot.MATIN, zone());
        Instant au = Plages.fin(lundi.plusDays(6), ShiftSlot.NUIT, zone());
        for (OperatorUnavailability u : indisponibilites.findByCancelledFalseAndStartsAtLessThanAndEndsAtGreaterThan(au, du)) {
            for (LocalDate j : Plages.jours(lundi)) {
                for (ShiftSlot s : ShiftSlot.values()) {
                    if (u.getStartsAt().isBefore(Plages.fin(j, s, zone())) && u.getEndsAt().isAfter(Plages.debut(j, s, zone()))) {
                        indispo.add(new Indisponibilite(u.getOperatorId(), j, s));
                    }
                }
            }
        }
        List<Affectation> veille = grilles
            .findFirstByIsoWeekAndStateOrderByRevisionDesc(Plages.semaineStockee(lundi.minusDays(1)), RosterState.PUBLIEE)
            .map(g -> affectations(g).stream().filter(a -> a.jour().equals(lundi.minusDays(1))).toList())
            .orElse(List.of());
        Set<Plages.Creneau> figes = new HashSet<>();
        List<Affectation> imposees = new ArrayList<>();
        if (publiee != null) {
            for (LocalDate j : Plages.jours(lundi)) {
                for (ShiftSlot s : ShiftSlot.values()) {
                    if (!Plages.debut(j, s, zone()).isAfter(maintenant)) {
                        figes.add(new Plages.Creneau(j, s));
                    }
                }
            }
            affectations(publiee).stream().filter(a -> figes.contains(new Plages.Creneau(a.jour(), a.plage()))).forEach(imposees::add);
        }
        return new ProblemeGrille(lundi, ops, indispo, veille, contraintes.lire().parametres(), figes, imposees);
    }

    private List<Affectation> affectations(OnCallRoster g) {
        return affectations
            .findByRosterIdOrderByDutyDateAscSlotAscLevelAsc(g.getId())
            .stream()
            .map(a -> new Affectation(a.getOperatorId(), a.getDutyDate(), a.getSlot(), a.getLevel(), Boolean.TRUE.equals(a.getShiftLead())))
            .toList();
    }

    private void enregistrer(OnCallRoster g, List<Affectation> l) {
        for (Affectation a : l) {
            affectations.save(
                new OnCallAssignment().dutyDate(a.jour()).slot(a.plage()).level(a.niveau()).operatorId(a.operateur()).shiftLead(a.chef()).roster(g)
            );
        }
    }

    private static Map<String, Object> rapportJson(VerificateurGrille.Rapport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(
            "nonCouvertes",
            r.nonCouvertes().stream().map(n -> new NonCouverteVue(n.jour(), n.plage().name(), n.besoin(), n.manque(), n.causes())).toList()
        );
        m.put(
            "violations",
            r
                .violations()
                .stream()
                .map(v -> new ViolationVue(v.code(), v.operateur() == null ? null : v.operateur().toString(), v.jour(), v.plage() == null ? null : v.plage().name()))
                .toList()
        );
        m.put("charges", r.charges());
        return m;
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
                    nonCouvertes(g).size(),
                    g.getGeneratedAt(),
                    g.getPublishedAt()
                )
            )
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GrilleVue grille(Appelant appelant, UUID id) {
        return vue(lire(id));
    }

    @Override
    @Transactional(readOnly = true)
    public GrilleVue publiee(Appelant appelant, String semaine) {
        LocalDate lundi = lundi(semaine);
        return vue(
            grilles
                .findFirstByIsoWeekAndStateOrderByRevisionDesc(Plages.semaineStockee(lundi), RosterState.PUBLIEE)
                .orElseThrow(() -> MonitoringException.introuvable("MON_GRILLE_INTROUVABLE", semaine))
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> nonCouvertes(OnCallRoster g) {
        Object l = JsonCanonique.lire(g.getUncoveredSlots()).get("nonCouvertes");
        return l instanceof List<?> x ? (List<Map<String, Object>>) x : List.of();
    }

    @SuppressWarnings("unchecked")
    GrilleVue vue(OnCallRoster g) {
        List<OnCallAssignment> l = affectations.findByRosterIdOrderByDutyDateAscSlotAscLevelAsc(g.getId());
        Map<String, Object> rapport = JsonCanonique.lire(g.getUncoveredSlots());
        Set<String> ids = new LinkedHashSet<>();
        l.forEach(a -> ids.add(a.getOperatorId().toString()));
        if (g.getGeneratedBy() != null) {
            ids.add(g.getGeneratedBy().toString());
        }
        if (g.getValidatedBy() != null) {
            ids.add(g.getValidatedBy().toString());
        }
        Map<String, String> noms = quarts.noms(ids);
        List<NonCouverteVue> nc = ((List<Map<String, Object>>) rapport.getOrDefault("nonCouvertes", List.of())).stream()
            .map(m ->
                new NonCouverteVue(
                    LocalDate.parse(String.valueOf(m.get("date"))),
                    String.valueOf(m.get("plage")),
                    String.valueOf(m.get("besoin")),
                    ((Number) m.get("manque")).intValue(),
                    (List<String>) m.getOrDefault("causes", List.of())
                )
            )
            .toList();
        List<ViolationVue> vi = ((List<Map<String, Object>>) rapport.getOrDefault("violations", List.of())).stream()
            .map(m ->
                new ViolationVue(
                    String.valueOf(m.get("code")),
                    (String) m.get("operateurId"),
                    m.get("date") == null ? null : LocalDate.parse(String.valueOf(m.get("date"))),
                    (String) m.get("plage")
                )
            )
            .toList();
        List<ChargeVue> ch = ((List<Map<String, Object>>) rapport.getOrDefault("charges", List.of())).stream()
            .map(m ->
                new ChargeVue(
                    Quarts.personne(m.get("operateur"), noms),
                    ((Number) m.get("heures")).doubleValue(),
                    ((Number) m.get("nuits")).intValue(),
                    ((Number) m.get("finsDeSemaine")).intValue()
                )
            )
            .toList();
        LocalDate lundi = Plages.lundi(Plages.semaineAffichee(g.getIsoWeek())).orElse(null);
        return new GrilleVue(
            g.getId(),
            Plages.semaineAffichee(g.getIsoWeek()),
            g.getRevision(),
            g.getState().name(),
            g.getSolverStatus().name(),
            g.getObjectiveValue() == null ? null : g.getObjectiveValue().longValue(),
            g.getGap() == null ? null : g.getGap().doubleValue(),
            g.getSolveDuration() == null ? null : g.getSolveDuration().toMillis(),
            g.getGeneratedAt(),
            Quarts.personne(g.getGeneratedBy(), noms),
            g.getPublishedAt(),
            Quarts.personne(g.getValidatedBy(), noms),
            lundi == null ? List.of() : Plages.jours(lundi),
            l
                .stream()
                .map(a -> new AffectationVue(a.getId(), a.getDutyDate(), a.getSlot().name(), a.getLevel(), Quarts.personne(a.getOperatorId(), noms), Boolean.TRUE.equals(a.getShiftLead())))
                .toList(),
            nc,
            vi,
            ch
        );
    }

    // ---------------------------------------------------------------- publication

    @Override
    public GrilleVue publier(Appelant appelant, UUID id, Boolean accepterNonCouvertes, String motif, String assertion) {
        OnCallRoster g = lire(id);
        if (g.getState() != RosterState.GENEREE) {
            throw MonitoringException.conflit("MON_GRILLE_NON_PUBLIABLE", "Seule une grille générée se publie");
        }
        int manques = nonCouvertes(g).size();
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
        rendrePublique(g, auteur, m);
        return vue(g);
    }

    private void rendrePublique(OnCallRoster g, UUID auteur, String motif) {
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
        List<Affectation> l = affectations(g);
        instancierVacations(g, l, auteur, maintenant);
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("grilleId", g.getId());
        evt.put("semaine", Plages.semaineAffichee(g.getIsoWeek()));
        evt.put("revision", g.getRevision());
        evt.put(
            "affectations",
            l
                .stream()
                .map(a -> Map.of("operateurId", a.operateur().toString(), "date", a.jour().toString(), "plage", a.plage().name(), "niveau", a.niveau(), "chefDeQuart", a.chef()))
                .toList()
        );
        evt.put("nonCouvertes", nonCouvertes(g).size());
        evt.put("motif", motif);
        outbox.publish("OnCallRoster", g.getId(), g.getRevision() > 1 ? "monitoring.oncall-revised.v1" : "monitoring.oncall-published.v1", evt);
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("destinataires", l.stream().map(a -> a.operateur().toString()).distinct().toList());
        n.put("niveau", "AVERTISSEMENT");
        n.put("objet", "GRILLE_PUBLIEE");
        n.put("reference", Plages.semaineAffichee(g.getIsoWeek()));
        n.put("lien", "/des/exploitation/astreintes?semaine=" + Plages.semaineAffichee(g.getIsoWeek()));
        outbox.publish("OnCallRoster", g.getId(), "monitoring.notification-requested.v1", n);
    }

    /**
     * Vacations des plages non commencées (re)instanciées depuis la grille : opérateurs en salle et chef de quart ;
     * une plage sans chef de quart désigné prend le chef du DES qui publie, anomalie consignée (MON-13).
     */
    private void instancierVacations(OnCallRoster g, List<Affectation> l, UUID publieur, Instant maintenant) {
        LocalDate lundi = Plages.lundi(Plages.semaineAffichee(g.getIsoWeek())).orElseThrow();
        for (LocalDate j : Plages.jours(lundi)) {
            for (ShiftSlot s : ShiftSlot.values()) {
                Instant debut = Plages.debut(j, s, zone());
                if (!debut.isAfter(maintenant)) {
                    continue;
                }
                List<Affectation> ici = l.stream().filter(a -> a.jour().equals(j) && a.plage() == s && a.niveau() == 1).toList();
                UUID chef = ici.stream().filter(Affectation::chef).map(Affectation::operateur).findFirst().orElse(null);
                String ref = Plages.referenceVacation(j, s);
                Shift v = vacations.findByReference(ref).orElseGet(Shift::new);
                boolean sansChef = chef == null;
                v.setReference(ref);
                v.setShiftDate(j);
                v.setSlot(s);
                v.setStartsAt(debut);
                v.setEndsAt(Plages.fin(j, s, zone()));
                v.setOperatorIds(Quarts.joindre(ici.stream().map(Affectation::operateur).filter(o -> !o.equals(chef)).toList()));
                v.setShiftLeadId(sansChef ? publieur : chef);
                v.setRoster(g);
                vacations.save(v);
                if (sansChef) {
                    mainCourante.ajouter(
                        v,
                        MainCourante.SYSTEME,
                        ShiftLogNature.EVENEMENT,
                        "Aucun chef de quart désigné par la grille (révision " + g.getRevision() + ") : le chef du DES qui l'a publiée tient la vacation",
                        g.getIsoWeek(),
                        null,
                        maintenant
                    );
                }
            }
        }
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

    /** Affectations de la grille après échange des deux plages. */
    private List<Affectation> apresEchange(OnCallRoster g, OnCallAssignment a, OnCallAssignment b) {
        List<Affectation> l = new ArrayList<>();
        for (OnCallAssignment x : affectations.findByRosterIdOrderByDutyDateAscSlotAscLevelAsc(g.getId())) {
            UUID op = x.getOperatorId();
            if (x.getId().equals(a.getId())) {
                op = b.getOperatorId();
            } else if (x.getId().equals(b.getId())) {
                op = a.getOperatorId();
            }
            l.add(new Affectation(op, x.getDutyDate(), x.getSlot(), x.getLevel(), Boolean.TRUE.equals(x.getShiftLead())));
        }
        return l;
    }

    /** Violations nouvelles créées par l'échange pour les deux opérateurs. */
    private List<ViolationVue> violationsEchange(OnCallRoster g, OnCallAssignment a, OnCallAssignment b) {
        LocalDate lundi = Plages.lundi(Plages.semaineAffichee(g.getIsoWeek())).orElseThrow();
        ProblemeGrille p = probleme(lundi, null, quarts.maintenant());
        Set<VerificateurGrille.Violation> avant = new HashSet<>(VerificateurGrille.verifier(p, affectations(g)).violations());
        Set<UUID> concernes = Set.of(a.getOperatorId(), b.getOperatorId());
        return VerificateurGrille.verifier(p, apresEchange(g, a, b))
            .violations()
            .stream()
            .filter(v -> !avant.contains(v) && (v.operateur() == null || concernes.contains(v.operateur())))
            .map(v -> new ViolationVue(v.code(), v.operateur() == null ? null : v.operateur().toString(), v.jour(), v.plage() == null ? null : v.plage().name()))
            .toList();
    }

    private void controler(OnCallRoster g, OnCallAssignment a, OnCallAssignment b) {
        Instant maintenant = quarts.maintenant();
        if (!Plages.debut(a.getDutyDate(), a.getSlot(), zone()).isAfter(maintenant) || !Plages.debut(b.getDutyDate(), b.getSlot(), zone()).isAfter(maintenant)) {
            throw MonitoringException.regle("MON_PLAGE_PASSEE", "Une plage commencée ne s'échange plus");
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
        LocalDate lundi = Plages.lundi(Plages.semaineAffichee(g.getIsoWeek())).orElseThrow();
        VerificateurGrille.Rapport rapport = VerificateurGrille.verifier(probleme(lundi, null, maintenant), l);
        OnCallRoster derniere = grilles.findFirstByIsoWeekOrderByRevisionDesc(g.getIsoWeek()).orElse(g);
        OnCallRoster n = new OnCallRoster()
            .isoWeek(g.getIsoWeek())
            .revision(derniere.getRevision() + 1)
            .state(RosterState.GENEREE)
            .solverStatus(g.getSolverStatus())
            .objectiveValue(g.getObjectiveValue())
            .gap(g.getGap())
            .solveDuration(g.getSolveDuration())
            .uncoveredSlots(JsonCanonique.ecrire(rapportJson(rapport)))
            .generatedAt(maintenant)
            .generatedBy(moi)
            .previousRevision(g);
        grilles.save(n);
        enregistrer(n, l);
        rendrePublique(n, moi, null);
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
        Map<UUID, OnCallAssignment> affs = new HashMap<>();
        for (EchangeAstreinte e : l) {
            ids.add(e.getDemandeurId().toString());
            ids.add(e.getCibleId().toString());
            if (e.getDecidePar() != null) {
                ids.add(e.getDecidePar().toString());
            }
            affectations.findById(e.getAffectationId()).ifPresent(a -> affs.put(a.getId(), a));
            affectations.findById(e.getAffectationCibleId()).ifPresent(a -> affs.put(a.getId(), a));
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
                    courte(affs.get(e.getAffectationId())),
                    courte(affs.get(e.getAffectationCibleId())),
                    e.getMotif(),
                    e.getCreeLe(),
                    e.getDecideLe(),
                    Quarts.personne(e.getDecidePar(), noms),
                    e.getNouvelleGrilleId()
                )
            )
            .toList();
    }

    private static AffectationCourte courte(OnCallAssignment a) {
        return a == null ? null : new AffectationCourte(a.getId(), a.getDutyDate(), a.getSlot().name(), a.getLevel());
    }
}
