package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Vues.AffectationVue;
import cm.gimac.des.monitoring.application.port.in.Vues.ChargeVue;
import cm.gimac.des.monitoring.application.port.in.Vues.GrilleVue;
import cm.gimac.des.monitoring.application.port.in.Vues.NonCouverteVue;
import cm.gimac.des.monitoring.application.port.in.Vues.ViolationVue;
import cm.gimac.des.monitoring.application.port.out.AnnuairePort;
import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.OperatorUnavailability;
import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import cm.gimac.des.monitoring.domain.exploitation.JsonCanonique;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux.Occurrence;
import cm.gimac.des.monitoring.domain.exploitation.Plages;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Affectation;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Indisponibilite;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Operateur;
import cm.gimac.des.monitoring.domain.exploitation.VerificateurGrille;
import cm.gimac.des.monitoring.infrastructure.persistance.AffectationsRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.GrillesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.IndisponibilitesRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Appui du programme (D-117) : données du problème d'une semaine sur le modèle des créneaux, placement des
 * affectations enregistrées, enregistrement d'une grille et de son modèle, vue servie.
 */
@Component
class ProgrammeGrilles {

    private final GrillesRepository grilles;
    private final AffectationsRepository affectations;
    private final IndisponibilitesRepository indisponibilites;
    private final AnnuairePort annuaire;
    private final Quarts quarts;
    private final CreneauxProgramme creneaux;
    private final ContraintesGrille contraintes;

    ProgrammeGrilles(
        GrillesRepository grilles,
        AffectationsRepository affectations,
        IndisponibilitesRepository indisponibilites,
        AnnuairePort annuaire,
        Quarts quarts,
        CreneauxProgramme creneaux,
        ContraintesGrille contraintes
    ) {
        this.grilles = grilles;
        this.affectations = affectations;
        this.indisponibilites = indisponibilites;
        this.annuaire = annuaire;
        this.quarts = quarts;
        this.creneaux = creneaux;
        this.contraintes = contraintes;
    }

    ZoneId zone() {
        return quarts.reglages().fuseau();
    }

    static LocalDate lundi(OnCallRoster g) {
        return Plages.lundi(Plages.semaineAffichee(g.getIsoWeek())).orElseThrow();
    }

    /** Affectations placées d'une grille, avec le modèle qui l'a produite. */
    List<CreneauxProgramme.Placee> placees(OnCallRoster g) {
        return creneaux.placer(creneaux.modeleDe(g.getId()), affectations.findByRosterIdOrderByDutyDateAscSlotAscLevelAsc(g.getId()));
    }

    /** Affectations des grilles publiées dont l'occurrence est d'un jour du {@code du} au {@code au}. */
    List<CreneauxProgramme.Placee> publiees(LocalDate du, LocalDate au) {
        List<CreneauxProgramme.Placee> l = new ArrayList<>();
        for (LocalDate lundi = du.with(java.time.DayOfWeek.MONDAY); !lundi.isAfter(au); lundi = lundi.plusDays(7)) {
            grilles
                .findFirstByIsoWeekAndStateOrderByRevisionDesc(Plages.semaineStockee(lundi), RosterState.PUBLIEE)
                .ifPresent(g -> placees(g).stream().filter(p -> !p.affectation().jour().isBefore(du) && !p.affectation().jour().isAfter(au)).forEach(l::add));
        }
        return l;
    }

    /**
     * Personnes à qui revient le rapport de 8 h de chaque journée (D-117 §11.4) : celles affectées, dans le programme
     * publié, aux créneaux de ce jour qui portent le rapport (la nuit, par défaut).
     */
    Map<LocalDate, List<String>> equipesRapport(LocalDate du, LocalDate au) {
        Map<LocalDate, List<String>> m = new java.util.TreeMap<>();
        for (CreneauxProgramme.Placee p : publiees(du, au)) {
            if (p.creneau() != null && p.creneau().rapportQuotidien()) {
                List<String> l = m.computeIfAbsent(p.affectation().jour(), k -> new ArrayList<>());
                String id = p.affectation().operateur().toString();
                if (!l.contains(id)) {
                    l.add(id);
                }
            }
        }
        return m;
    }

    /** Personnes de niveau 2 de service à l'instant, selon le programme publié. */
    List<String> astreinteA(Instant t) {
        LocalDate jour = t.atZone(zone()).toLocalDate();
        return publiees(jour.minusDays(7), jour)
            .stream()
            .map(CreneauxProgramme.Placee::affectation)
            .filter(a -> a.niveau() == 2 && !a.debut().isAfter(t) && a.fin().isAfter(t))
            .map(a -> a.operateur().toString())
            .distinct()
            .toList();
    }

    List<Affectation> affectations(OnCallRoster g) {
        return placees(g).stream().map(CreneauxProgramme.Placee::affectation).toList();
    }

    /**
     * Données du problème : occurrences du modèle, opérateurs porteurs des permissions requises, absences, semaine
     * précédente (repos et nuits), occurrences commencées figées en révision d'une grille publiée.
     */
    ProblemeGrille probleme(LocalDate lundi, ModeleCreneaux modele, OnCallRoster publiee, Instant maintenant) {
        List<Occurrence> occ = modele.occurrences(lundi, zone());
        Set<String> permissions = new LinkedHashSet<>();
        occ.forEach(o -> permissions.addAll(o.creneau().permissionsRequises()));
        permissions.add(ProblemeGrille.PERMISSION_CHEF);
        Map<String, Set<String>> portees = new LinkedHashMap<>();
        for (String p : permissions) {
            annuaire.porteurs(p).forEach(x -> portees.computeIfAbsent(x.id(), k -> new HashSet<>()).add(p));
        }
        List<Operateur> ops = new ArrayList<>();
        portees.forEach((id, p) -> {
            try {
                ops.add(new Operateur(UUID.fromString(id), p));
            } catch (IllegalArgumentException e) {
                // compte sans identifiant UUID : ignoré
            }
        });
        Instant du = lundi.minusDays(1).atStartOfDay(zone()).toInstant();
        Instant au = lundi.plusDays(9).atStartOfDay(zone()).toInstant();
        List<Indisponibilite> indispo = new ArrayList<>();
        for (OperatorUnavailability u : indisponibilites.findByCancelledFalseAndStartsAtLessThanAndEndsAtGreaterThan(au, du)) {
            indispo.add(new Indisponibilite(u.getOperatorId(), u.getStartsAt(), u.getEndsAt()));
        }
        var par = contraintes.lire().parametres();
        LocalDate depuis = lundi.minusDays(par.nuitsMax() + 1L);
        List<Affectation> veille = grilles
            .findFirstByIsoWeekAndStateOrderByRevisionDesc(Plages.semaineStockee(lundi.minusDays(1)), RosterState.PUBLIEE)
            .map(g -> affectations(g).stream().filter(a -> !a.jour().isBefore(depuis)).toList())
            .orElse(List.of());
        Set<String> figes = new HashSet<>();
        List<Affectation> imposees = new ArrayList<>();
        if (publiee != null) {
            occ.stream().filter(o -> !o.debut().isAfter(maintenant)).forEach(o -> figes.add(o.cle()));
            affectations(publiee).stream().filter(a -> figes.contains(a.cle())).forEach(imposees::add);
        }
        return new ProblemeGrille(lundi, ops, occ, indispo, veille, par, figes, imposees);
    }

    /** Enregistre les affectations d'une grille, chacune avec son créneau, et la version du modèle utilisée. */
    void enregistrer(OnCallRoster g, List<Affectation> l, int version) {
        List<OnCallAssignment> entites = new ArrayList<>();
        for (Affectation a : l) {
            entites.add(
                new OnCallAssignment()
                    .dutyDate(a.jour())
                    .slot(Plages.creneau(a.debut(), zone()).plage())
                    .level(a.niveau())
                    .operatorId(a.operateur())
                    .shiftLead(a.chef())
                    .roster(g)
            );
        }
        // Grille et affectations écrites avant leurs compléments (clés étrangères des tables de D-117).
        affectations.saveAll(entites);
        affectations.flush();
        creneaux.figer(g.getId(), version);
        for (int i = 0; i < l.size(); i++) {
            creneaux.enregistrer(entites.get(i), l.get(i));
        }
    }

    static Map<String, Object> rapportJson(VerificateurGrille.Rapport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(
            "nonCouvertes",
            r
                .nonCouvertes()
                .stream()
                .map(n -> {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("date", n.jour().toString());
                    x.put("creneau", n.creneau());
                    x.put("besoin", n.besoin());
                    x.put("manque", n.manque());
                    x.put("causes", n.causes());
                    return x;
                })
                .toList()
        );
        m.put(
            "violations",
            r.violations().stream().map(v -> new ViolationVue(v.code(), v.operateur() == null ? null : v.operateur().toString(), v.jour(), v.creneau())).toList()
        );
        m.put("charges", r.charges());
        return m;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> nonCouvertes(OnCallRoster g) {
        Object l = JsonCanonique.lire(g.getUncoveredSlots()).get("nonCouvertes");
        return l instanceof List<?> x ? (List<Map<String, Object>>) x : List.of();
    }

    /** Créneau d'un élément enregistré : « creneau » (D-117), sinon « plage » et besoin de D-114. */
    private static String creneauDe(Map<String, Object> m) {
        Object c = m.get("creneau");
        if (c != null) {
            return String.valueOf(c);
        }
        Object plage = m.get("plage");
        return plage == null ? null : ModeleCreneaux.codeHistorique(String.valueOf(plage), "NIVEAU_2".equals(m.get("besoin")) ? 2 : 1);
    }

    @SuppressWarnings("unchecked")
    GrilleVue vue(OnCallRoster g) {
        ModeleCreneaux modele = creneaux.modeleDe(g.getId());
        List<CreneauxProgramme.Placee> l = creneaux.placer(modele, affectations.findByRosterIdOrderByDutyDateAscSlotAscLevelAsc(g.getId()));
        Map<String, Object> rapport = JsonCanonique.lire(g.getUncoveredSlots());
        Set<String> ids = new LinkedHashSet<>();
        l.forEach(a -> ids.add(a.affectation().operateur().toString()));
        if (g.getGeneratedBy() != null) {
            ids.add(g.getGeneratedBy().toString());
        }
        if (g.getValidatedBy() != null) {
            ids.add(g.getValidatedBy().toString());
        }
        Map<String, String> noms = quarts.noms(ids);
        LocalDate lundi = Plages.lundi(Plages.semaineAffichee(g.getIsoWeek())).orElse(null);
        Map<String, Occurrence> occ = new HashMap<>();
        if (lundi != null) {
            modele.occurrences(lundi, zone()).forEach(o -> occ.put(o.cle(), o));
        }
        List<NonCouverteVue> nc = ((List<Map<String, Object>>) rapport.getOrDefault("nonCouvertes", List.of())).stream()
            .map(m -> {
                LocalDate date = LocalDate.parse(String.valueOf(m.get("date")));
                String code = creneauDe(m);
                Occurrence o = occ.get(code + "|" + date);
                String besoin = String.valueOf(m.get("besoin"));
                return new NonCouverteVue(
                    date,
                    code,
                    o == null ? null : o.debut(),
                    o == null ? null : o.fin(),
                    besoin.startsWith("NIVEAU_") ? "EFFECTIF" : besoin,
                    ((Number) m.get("manque")).intValue(),
                    (List<String>) m.getOrDefault("causes", List.of())
                );
            })
            .toList();
        List<ViolationVue> vi = ((List<Map<String, Object>>) rapport.getOrDefault("violations", List.of())).stream()
            .map(m ->
                new ViolationVue(
                    String.valueOf(m.get("code")),
                    (String) m.get("operateurId"),
                    m.get("date") == null ? null : LocalDate.parse(String.valueOf(m.get("date"))),
                    creneauDe(m)
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
            modele.creneaux().stream().map(CreneauxProgramme::vue).toList(),
            l
                .stream()
                .map(p -> {
                    Affectation a = p.affectation();
                    return new AffectationVue(p.entite().getId(), a.jour(), a.code(), a.debut(), a.fin(), a.semaine(), a.niveau(), Quarts.personne(a.operateur(), noms), a.chef());
                })
                .sorted(java.util.Comparator.comparing(AffectationVue::debut).thenComparing(AffectationVue::creneau).thenComparing(v -> v.operateur().nom()))
                .toList(),
            nc,
            vi,
            ch
        );
    }
}
