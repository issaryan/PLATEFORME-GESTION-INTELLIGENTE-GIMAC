package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Contrôle de cohérence du référentiel (HOM-02 A2, RG-26) : unicité des codes, existence des
 * prédécesseurs, compatibilité des segments, acyclicité. Toutes les anomalies sont recensées.
 * Règle de segments : le rang d'un prédécesseur (amont 0, branche 1, aval 2) ne dépasse jamais
 * celui de l'opération ; le tronc commun amont reste ainsi identique pour tous les types de projet.
 * Contrôles du modèle (D-77, docs/lot1/modelisateur-processus.md §2), sur les nœuds qui portent leur modèle :
 * responsable et validateur présents et distincts, au moins une opération sans prédécesseur, déclencheur connu
 * de chaque événement externe, jalons uniques et ordonnés le long du graphe, secret sans valeur de référence,
 * codes de livrables uniques. Les anomalies du modèle suivent celles du graphe.
 */
public final class ReferentialGraphValidator {

    public enum TypeAnomalie {
        CODE_DUPLIQUE,
        PREDECESSEUR_INCONNU,
        SEGMENT_INCOHERENT,
        CYCLE,
        RESPONSABLE_ABSENT,
        VALIDATEUR_ABSENT,
        VALIDATEUR_EGAL_RESPONSABLE,
        DECLENCHEUR_INCONNU,
        JALON_DUPLIQUE,
        JALON_DESORDONNE,
        SANS_ENTREE,
        LIVRABLE_DUPLIQUE,
        SECRET_AVEC_VALEUR,
    }

    public record Anomalie(TypeAnomalie type, List<String> codes, String motif) {}

    public record Rapport(List<Anomalie> anomalies, List<String> ordre, Map<String, Integer> couches) {
        @com.fasterxml.jackson.annotation.JsonProperty("conforme")
        public boolean conforme() {
            return anomalies.isEmpty();
        }
    }

    private final TopologicalSorter sorter;

    public ReferentialGraphValidator(TopologicalSorter sorter) {
        this.sorter = sorter;
    }

    public ReferentialGraphValidator() {
        this(new TopologicalSorter());
    }

    public Rapport controler(List<NoeudReferentiel> noeuds) {
        List<Anomalie> anomalies = new ArrayList<>();
        Map<String, NoeudReferentiel> parCode = new TreeMap<>();
        for (NoeudReferentiel n : noeuds) {
            if (parCode.putIfAbsent(n.code(), n) != null) {
                anomalies.add(new Anomalie(TypeAnomalie.CODE_DUPLIQUE, List.of(n.code()), "Code présent plusieurs fois"));
            }
        }
        Map<String, List<String>> arcsValides = new TreeMap<>();
        for (NoeudReferentiel n : parCode.values()) {
            List<String> valides = new ArrayList<>();
            for (String p : n.predecesseurs()) {
                NoeudReferentiel pred = parCode.get(p);
                if (pred == null) {
                    anomalies.add(new Anomalie(TypeAnomalie.PREDECESSEUR_INCONNU, List.of(n.code(), p), "Prédécesseur absent de la version"));
                    continue;
                }
                if (pred.segment() != null && n.segment() != null && pred.segment().ordinal() > n.segment().ordinal()) {
                    anomalies.add(
                        new Anomalie(
                            TypeAnomalie.SEGMENT_INCOHERENT,
                            List.of(n.code(), p),
                            "Une opération du segment " + n.segment() + " ne peut succéder à une opération du segment " + pred.segment()
                        )
                    );
                }
                valides.add(p);
            }
            arcsValides.put(n.code(), valides);
        }
        TopologicalSorter.Result tri = sorter.sort(arcsValides);
        if (!tri.acyclique()) {
            anomalies.add(new Anomalie(TypeAnomalie.CYCLE, tri.cycle(), "Cycle dans le graphe des prédécesseurs"));
        }
        if (parCode.values().stream().anyMatch(n -> n.modele() != null)) {
            controlerModele(parCode, arcsValides, tri.acyclique(), anomalies);
        }
        return new Rapport(List.copyOf(anomalies), tri.ordre(), tri.couches());
    }

    private static void controlerModele(Map<String, NoeudReferentiel> parCode, Map<String, List<String>> arcs, boolean acyclique, List<Anomalie> anomalies) {
        Map<Jalon, List<String>> jalons = new EnumMap<>(Jalon.class);
        for (NoeudReferentiel n : parCode.values()) {
            NoeudReferentiel.Modele m = n.modele();
            if (m == null) {
                continue;
            }
            if (m.responsable() == null) {
                anomalies.add(new Anomalie(TypeAnomalie.RESPONSABLE_ABSENT, List.of(n.code()), "Aucun responsable (R) désigné"));
            }
            if (m.validateur() == null) {
                anomalies.add(new Anomalie(TypeAnomalie.VALIDATEUR_ABSENT, List.of(n.code()), "Aucun validateur (V) désigné"));
            } else if (m.validateur() == m.responsable()) {
                anomalies.add(new Anomalie(TypeAnomalie.VALIDATEUR_EGAL_RESPONSABLE, List.of(n.code()), "Le validateur doit être distinct du responsable"));
            }
            if ((m.geste() == Geste.EVENEMENT_EXTERNE && m.declencheur() == null) || (m.declencheur() != null && !Declencheurs.connu(m.declencheur()))) {
                anomalies.add(
                    new Anomalie(
                        TypeAnomalie.DECLENCHEUR_INCONNU,
                        m.declencheur() == null ? List.of(n.code()) : List.of(n.code(), m.declencheur()),
                        "Un événement externe doit désigner un sujet connu"
                    )
                );
            }
            if (m.secret() && m.valeurReference() != null) {
                anomalies.add(new Anomalie(TypeAnomalie.SECRET_AVEC_VALEUR, List.of(n.code()), "Une opération secrète ne relève aucune valeur (D-59)"));
            }
            Set<String> vus = new HashSet<>();
            Set<String> doublons = new LinkedHashSet<>();
            m.livrables().forEach(c -> {
                if (!vus.add(c)) {
                    doublons.add(c);
                }
            });
            if (!doublons.isEmpty()) {
                List<String> codes = new ArrayList<>();
                codes.add(n.code());
                codes.addAll(doublons);
                anomalies.add(new Anomalie(TypeAnomalie.LIVRABLE_DUPLIQUE, List.copyOf(codes), "Code de livrable présent plusieurs fois"));
            }
            if (m.jalon() != null) {
                jalons.computeIfAbsent(m.jalon(), j -> new ArrayList<>()).add(n.code());
            }
        }
        if (!parCode.isEmpty() && parCode.values().stream().noneMatch(n -> n.predecesseurs().isEmpty())) {
            anomalies.add(new Anomalie(TypeAnomalie.SANS_ENTREE, List.of(), "Aucune opération sans prédécesseur : le parcours n'a pas d'entrée"));
        }
        jalons.forEach((j, codes) -> {
            if (codes.size() > 1) {
                List<String> l = new ArrayList<>();
                l.add(j.name());
                l.addAll(codes);
                anomalies.add(new Anomalie(TypeAnomalie.JALON_DUPLIQUE, List.copyOf(l), "Le jalon " + j.name() + " est porté par plusieurs opérations"));
            }
        });
        if (acyclique) {
            for (var tardif : jalons.entrySet()) {
                for (var precoce : jalons.entrySet()) {
                    if (precoce.getKey().ordinal() >= tardif.getKey().ordinal()) {
                        continue;
                    }
                    for (String t : tardif.getValue()) {
                        for (String p : precoce.getValue()) {
                            if (ancetre(t, p, arcs)) {
                                anomalies.add(
                                    new Anomalie(
                                        TypeAnomalie.JALON_DESORDONNE,
                                        List.of(t, p),
                                        "Le jalon " + tardif.getKey().name() + " précède le jalon antérieur " + precoce.getKey().name()
                                    )
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    /** {@code a} est-il un ancêtre (prédécesseur transitif) de {@code b} ? */
    private static boolean ancetre(String a, String b, Map<String, List<String>> arcs) {
        Deque<String> pile = new ArrayDeque<>(arcs.getOrDefault(b, List.of()));
        Set<String> vus = new HashSet<>();
        while (!pile.isEmpty()) {
            String x = pile.pop();
            if (x.equals(a)) {
                return true;
            }
            if (vus.add(x)) {
                pile.addAll(arcs.getOrDefault(x, List.of()));
            }
        }
        return false;
    }

    /** Port interne invoqué par la publication (HOM-01) : renvoie l'ordre de référence ou refuse. */
    public List<String> validate(List<NoeudReferentiel> noeuds) {
        Rapport r = controler(noeuds);
        if (!r.conforme()) {
            throw HomologationException.conflict(
                "HOM_REFERENTIAL_GRAPH_INVALID",
                "Publication refusée : graphe des prédécesseurs non conforme",
                Map.of("anomalies", r.anomalies())
            );
        }
        return r.ordre();
    }
}
