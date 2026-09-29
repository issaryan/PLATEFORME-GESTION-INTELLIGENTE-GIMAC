package cm.gimac.des.homologation.domain.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Tri de Kahn déterministe avec restitution du cycle (HOM-02 A1, algorithme 6.30).
 * À chaque pas, le plus petit code prêt est extrait : l'ordre est reproductible.
 * Complexité : O((V + E) log V).
 */
public final class TopologicalSorter {

    /** Ordre complet et couches si le graphe est acyclique ; sinon cycle restitué et ordre vide. */
    public record Result(List<String> ordre, Map<String, Integer> couches, List<String> cycle) {
        public boolean acyclique() {
            return cycle.isEmpty();
        }
    }

    /**
     * @param predecesseurs pour chaque code, ses prédécesseurs ; précondition : tout prédécesseur est une clé
     *                      (vérifiée par ReferentialGraphValidator avant l'appel).
     */
    public Result sort(Map<String, ? extends Collection<String>> predecesseurs) {
        Map<String, Integer> degre = new HashMap<>();
        Map<String, List<String>> successeurs = new HashMap<>();
        predecesseurs.forEach((code, preds) -> {
            degre.merge(code, 0, Integer::sum);
            for (String p : new LinkedHashSet<>(preds)) { // un prédécesseur répété ne compte qu'une fois
                degre.merge(code, 1, Integer::sum);
                successeurs.computeIfAbsent(p, k -> new ArrayList<>()).add(code);
            }
        });
        PriorityQueue<String> prets = new PriorityQueue<>();
        degre.forEach((code, d) -> {
            if (d == 0) {
                prets.add(code);
            }
        });

        List<String> ordre = new ArrayList<>(degre.size());
        Map<String, Integer> couches = new HashMap<>();
        while (!prets.isEmpty()) {
            String n = prets.poll();
            ordre.add(n);
            int couche = couches.computeIfAbsent(n, k -> 0);
            for (String s : successeurs.getOrDefault(n, List.of())) {
                couches.merge(s, couche + 1, Math::max);
                if (degre.merge(s, -1, Integer::sum) == 0) {
                    prets.add(s);
                }
            }
        }
        if (ordre.size() == degre.size()) {
            return new Result(List.copyOf(ordre), Map.copyOf(couches), List.of());
        }
        return new Result(List.of(), Map.of(), extraireCycle(predecesseurs, degre));
    }

    /**
     * Chaque nœud résiduel garde un prédécesseur résiduel : en remontant depuis le plus petit code
     * résiduel, on retombe sur un nœud déjà vu. Le cycle est restitué dans le sens « a pour
     * prédécesseur », à partir de son plus petit code, fermé par ce même code (A, B, C, A).
     */
    private List<String> extraireCycle(Map<String, ? extends Collection<String>> preds, Map<String, Integer> degre) {
        String courant = degre.entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey).min(Comparator.naturalOrder()).orElseThrow();
        Map<String, Integer> rang = new HashMap<>();
        List<String> chemin = new ArrayList<>();
        while (!rang.containsKey(courant)) {
            rang.put(courant, chemin.size());
            chemin.add(courant);
            courant = preds.get(courant).stream().filter(p -> degre.get(p) > 0).min(Comparator.naturalOrder()).orElseThrow();
        }
        List<String> cycle = new ArrayList<>(chemin.subList(rang.get(courant), chemin.size()));
        Collections.rotate(cycle, -cycle.indexOf(Collections.min(cycle)));
        cycle.add(cycle.get(0));
        return List.copyOf(cycle);
    }
}
