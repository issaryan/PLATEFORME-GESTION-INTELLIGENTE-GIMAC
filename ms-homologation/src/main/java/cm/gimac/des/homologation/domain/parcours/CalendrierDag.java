package cm.gimac.des.homologation.domain.parcours;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calendrier au plus tôt d'un graphe d'opérations (docs/lot1/modelisateur-processus.md §3) : la fin au plus tôt
 * d'une opération est le maximum des fins de ses prédécesseurs, plus sa durée, depuis le jour 0 (date de
 * démarrage) ; jours calendaires. Les prédécesseurs absents du graphe sont ignorés (réputés faits). Le chemin
 * critique est la plus longue chaîne : il remonte, depuis l'opération qui finit le plus tard, par le prédécesseur
 * qui finit le plus tard (à égalité, le plus petit code). Un cycle n'est pas planifiable : ses nœuds sont absents
 * du résultat.
 */
public final class CalendrierDag {

    /** Nœud planifiable : prédécesseurs et durée en jours. */
    public record Tache(List<String> predecesseurs, int duree) {}

    public record Resultat(Map<String, Integer> debuts, Map<String, Integer> fins, List<String> cheminCritique, int dureeTotale) {}

    private CalendrierDag() {}

    public static Resultat calculer(Map<String, Tache> graphe) {
        Map<String, Integer> fins = new LinkedHashMap<>();
        Map<String, Integer> debuts = new LinkedHashMap<>();
        Set<String> enCours = new HashSet<>();
        Set<String> cycliques = new HashSet<>();
        for (String code : graphe.keySet()) {
            fin(code, graphe, fins, debuts, enCours, cycliques);
        }
        cycliques.forEach(c -> {
            fins.remove(c);
            debuts.remove(c);
        });
        String dernier = fins
            .entrySet()
            .stream()
            .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
            .map(Map.Entry::getKey)
            .orElse(null);
        List<String> chemin = new ArrayList<>();
        String courant = dernier;
        while (courant != null) {
            chemin.add(courant);
            courant = graphe
                .get(courant)
                .predecesseurs()
                .stream()
                .filter(fins::containsKey)
                .max(Comparator.<String>comparingInt(fins::get).thenComparing(Comparator.reverseOrder()))
                .orElse(null);
        }
        Collections.reverse(chemin);
        return new Resultat(Map.copyOf(debuts), Map.copyOf(fins), List.copyOf(chemin), dernier == null ? 0 : fins.get(dernier));
    }

    private static int fin(String code, Map<String, Tache> g, Map<String, Integer> fins, Map<String, Integer> debuts, Set<String> enCours, Set<String> cycliques) {
        Integer connue = fins.get(code);
        if (connue != null) {
            return connue;
        }
        if (!enCours.add(code)) {
            cycliques.add(code);
            return 0;
        }
        int debut = 0;
        for (String p : g.get(code).predecesseurs()) {
            if (g.containsKey(p)) {
                debut = Math.max(debut, fin(p, g, fins, debuts, enCours, cycliques));
                if (cycliques.contains(p)) {
                    cycliques.add(code);
                }
            }
        }
        enCours.remove(code);
        int f = debut + Math.max(0, g.get(code).duree());
        debuts.put(code, debut);
        fins.put(code, f);
        return f;
    }

    /** Successeurs de chaque code (arcs inverses), dans l'ordre des codes. */
    public static Map<String, List<String>> successeurs(Map<String, List<String>> predecesseurs) {
        Map<String, List<String>> s = new HashMap<>();
        predecesseurs.keySet().forEach(c -> s.put(c, new ArrayList<>()));
        predecesseurs.forEach((code, preds) -> preds.forEach(p -> {
            if (s.containsKey(p) && !s.get(p).contains(code)) {
                s.get(p).add(code);
            }
        }));
        s.values().forEach(Collections::sort);
        return s;
    }
}
