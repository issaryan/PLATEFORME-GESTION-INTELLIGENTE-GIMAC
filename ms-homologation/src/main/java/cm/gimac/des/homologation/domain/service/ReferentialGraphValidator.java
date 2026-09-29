package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.HomologationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Contrôle de cohérence du référentiel (HOM-02 A2, RG-26) : unicité des codes, existence des
 * prédécesseurs, compatibilité des segments, acyclicité. Toutes les anomalies sont recensées.
 * Règle de segments : le rang d'un prédécesseur (amont 0, branche 1, aval 2) ne dépasse jamais
 * celui de l'opération ; le tronc commun amont reste ainsi identique pour tous les types de projet.
 */
public final class ReferentialGraphValidator {

    public enum TypeAnomalie {
        CODE_DUPLIQUE,
        PREDECESSEUR_INCONNU,
        SEGMENT_INCOHERENT,
        CYCLE,
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
        return new Rapport(List.copyOf(anomalies), tri.ordre(), tri.couches());
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
