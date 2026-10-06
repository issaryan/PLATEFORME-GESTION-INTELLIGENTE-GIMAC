package cm.gimac.des.pilotage.domain.service;

import cm.gimac.des.pilotage.domain.processus.Etape;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graphe des étapes d'un processus (§1) : ordre topologique par l'algorithme de Kahn (étapes restantes = cycle),
 * étapes antérieures (ancêtres) et successeurs directs. Les prédécesseurs inconnus sont ignorés ici (le
 * contrôle les signale).
 */
public final class GrapheProcessus {

    /** Ordre topologique et étapes prises dans un cycle (vide si le graphe est acyclique). */
    public record Ordre(List<String> ordre, List<String> enCycle) {}

    private GrapheProcessus() {}

    public static Ordre kahn(List<Etape> etapes) {
        Map<String, Etape> parCode = indexer(etapes);
        Map<String, Integer> entrants = new LinkedHashMap<>();
        Map<String, List<String>> sortants = new HashMap<>();
        parCode.keySet().forEach(c -> {
            entrants.put(c, 0);
            sortants.put(c, new ArrayList<>());
        });
        parCode
            .values()
            .forEach(e ->
                new LinkedHashSet<>(e.predecesseurs())
                    .stream()
                    .filter(parCode::containsKey)
                    .forEach(p -> {
                        entrants.merge(e.code(), 1, Integer::sum);
                        sortants.get(p).add(e.code());
                    })
            );
        Deque<String> file = new ArrayDeque<>();
        entrants.forEach((c, n) -> {
            if (n == 0) {
                file.add(c);
            }
        });
        List<String> ordre = new ArrayList<>();
        while (!file.isEmpty()) {
            String c = file.poll();
            ordre.add(c);
            for (String s : sortants.get(c)) {
                if (entrants.merge(s, -1, Integer::sum) == 0) {
                    file.add(s);
                }
            }
        }
        List<String> cycle = parCode.keySet().stream().filter(c -> !ordre.contains(c)).toList();
        return new Ordre(ordre, cycle);
    }

    /** Étapes antérieures de {@code code} (prédécesseurs transitifs), sans elle-même. */
    public static Set<String> anterieures(List<Etape> etapes, String code) {
        Map<String, Etape> parCode = indexer(etapes);
        Set<String> vues = new LinkedHashSet<>();
        Deque<String> file = new ArrayDeque<>();
        Etape depart = parCode.get(code);
        if (depart == null) {
            return Set.of();
        }
        file.addAll(depart.predecesseurs());
        while (!file.isEmpty()) {
            String c = file.poll();
            if (!c.equals(code) && vues.add(c) && parCode.containsKey(c)) {
                file.addAll(parCode.get(c).predecesseurs());
            }
        }
        vues.retainAll(parCode.keySet());
        return vues;
    }

    /** Successeurs directs de {@code code}. */
    public static List<String> successeurs(List<Etape> etapes, String code) {
        return etapes.stream().filter(e -> e.predecesseurs().contains(code)).map(Etape::code).toList();
    }

    /** Étapes sans prédécesseur. */
    public static List<String> initiales(List<Etape> etapes) {
        return etapes.stream().filter(e -> e.predecesseurs().isEmpty()).map(Etape::code).toList();
    }

    private static Map<String, Etape> indexer(List<Etape> etapes) {
        Map<String, Etape> m = new LinkedHashMap<>();
        etapes.forEach(e -> m.putIfAbsent(e.code(), e));
        return m;
    }
}
