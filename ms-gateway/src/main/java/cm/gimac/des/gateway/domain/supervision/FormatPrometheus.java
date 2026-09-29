package cm.gimac.des.gateway.domain.supervision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Analyse du format d'exposition texte de Prometheus (version 0.0.4, celui de
 * /management/prometheus) et extraction des métriques d'exploitation d'un service.
 *
 * <p>Une ligne d'échantillon s'écrit {@code nom{etiquette="valeur",...} valeur [horodatage]} ;
 * les lignes de commentaire (# HELP, # TYPE) et les lignes mal formées sont ignorées.
 */
public final class FormatPrometheus {

    /** Métriques retenues pour l'instantané d'un service. */
    public static final Set<String> METRIQUES_EXPLOITATION = Set.of(
        "process_uptime_seconds",
        "process_cpu_usage",
        "system_cpu_usage",
        "system_cpu_count",
        "jvm_memory_used_bytes",
        "jvm_memory_max_bytes",
        "disk_free_bytes",
        "disk_total_bytes",
        "system_load_average_1m",
        "jvm_threads_live_threads"
    );

    /** Échantillon : nom de la métrique, étiquettes, valeur. */
    public record Echantillon(String nom, Map<String, String> etiquettes, double valeur) {
        public Echantillon {
            etiquettes = Map.copyOf(etiquettes);
        }
    }

    private FormatPrometheus() {}

    /** Nom de la métrique d'une ligne d'échantillon, ou vide pour un commentaire ou une ligne vide. */
    public static Optional<String> nomMetrique(String ligne) {
        if (ligne == null) {
            return Optional.empty();
        }
        String l = ligne.strip();
        if (l.isEmpty() || l.startsWith("#")) {
            return Optional.empty();
        }
        int fin = 0;
        while (fin < l.length() && l.charAt(fin) != '{' && !Character.isWhitespace(l.charAt(fin))) {
            fin++;
        }
        return Optional.of(l.substring(0, fin));
    }

    /** Analyse les lignes données et garde les échantillons dont le nom est accepté. */
    public static List<Echantillon> analyser(Collection<String> lignes, Predicate<String> nomAccepte) {
        List<Echantillon> resultat = new ArrayList<>();
        for (String ligne : lignes) {
            Optional<String> nom = nomMetrique(ligne);
            if (nom.isPresent() && nomAccepte.test(nom.orElseThrow())) {
                analyserLigne(ligne.strip()).ifPresent(resultat::add);
            }
        }
        return resultat;
    }

    /** Analyse une ligne d'échantillon ; vide si elle est mal formée. */
    static Optional<Echantillon> analyserLigne(String ligne) {
        String nom = nomMetrique(ligne).orElse("");
        if (nom.isEmpty()) {
            return Optional.empty();
        }
        int position = nom.length();
        Map<String, String> etiquettes = new LinkedHashMap<>();
        if (position < ligne.length() && ligne.charAt(position) == '{') {
            position = lireEtiquettes(ligne, position + 1, etiquettes);
            if (position < 0) {
                return Optional.empty();
            }
        }
        String[] reste = ligne.substring(position).strip().split("\\s+");
        if (reste.length == 0 || reste[0].isEmpty()) {
            return Optional.empty();
        }
        Double valeur = valeur(reste[0]);
        return valeur == null ? Optional.empty() : Optional.of(new Echantillon(nom, etiquettes, valeur));
    }

    /** Lit les étiquettes jusqu'à l'accolade fermante ; renvoie la position qui la suit, ou -1. */
    private static int lireEtiquettes(String ligne, int debut, Map<String, String> etiquettes) {
        int i = debut;
        while (i < ligne.length()) {
            char c = ligne.charAt(i);
            if (c == '}') {
                return i + 1;
            }
            if (c == ',' || c == ' ') {
                i++;
                continue;
            }
            int egal = ligne.indexOf('=', i);
            if (egal < 0 || egal + 1 >= ligne.length() || ligne.charAt(egal + 1) != '"') {
                return -1;
            }
            String cle = ligne.substring(i, egal).strip();
            StringBuilder valeur = new StringBuilder();
            int j = egal + 2;
            boolean ferme = false;
            while (j < ligne.length()) {
                char v = ligne.charAt(j);
                if (v == '\\' && j + 1 < ligne.length()) {
                    char suivant = ligne.charAt(j + 1);
                    valeur.append(suivant == 'n' ? '\n' : suivant);
                    j += 2;
                } else if (v == '"') {
                    ferme = true;
                    j++;
                    break;
                } else {
                    valeur.append(v);
                    j++;
                }
            }
            if (!ferme) {
                return -1;
            }
            etiquettes.put(cle, valeur.toString());
            i = j;
        }
        return -1;
    }

    /** Valeur numérique, y compris NaN, +Inf et -Inf ; null si illisible. */
    static Double valeur(String texte) {
        return switch (texte) {
            case "NaN" -> Double.NaN;
            case "+Inf", "Inf" -> Double.POSITIVE_INFINITY;
            case "-Inf" -> Double.NEGATIVE_INFINITY;
            default -> {
                try {
                    yield Double.parseDouble(texte);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    /**
     * Métriques d'exploitation d'un service. La mémoire de tas additionne les zones
     * « heap » (une série par espace mémoire) ; un maximum non défini (-1) est ignoré.
     */
    public static Metriques extraire(List<Echantillon> echantillons) {
        Double tasUtilise = somme(echantillons, "jvm_memory_used_bytes", "area", "heap");
        Double tasMax = somme(echantillons, "jvm_memory_max_bytes", "area", "heap");
        Double tasPourcent = null;
        if (tasUtilise != null && tasMax != null && tasMax > 0) {
            tasPourcent = arrondi(100 * tasUtilise / tasMax);
        }
        Double nombreCpu = premiere(echantillons, "system_cpu_count");
        Double threads = premiere(echantillons, "jvm_threads_live_threads");
        return new Metriques(
            premiere(echantillons, "process_uptime_seconds"),
            premiere(echantillons, "process_cpu_usage"),
            premiere(echantillons, "system_cpu_usage"),
            nombreCpu == null ? null : nombreCpu.intValue(),
            tasUtilise,
            tasMax,
            tasPourcent,
            premiere(echantillons, "disk_free_bytes"),
            premiere(echantillons, "disk_total_bytes"),
            premiere(echantillons, "system_load_average_1m"),
            threads == null ? null : threads.intValue()
        );
    }

    private static Double premiere(List<Echantillon> echantillons, String nom) {
        return echantillons
            .stream()
            .filter(e -> e.nom().equals(nom) && Double.isFinite(e.valeur()))
            .map(Echantillon::valeur)
            .findFirst()
            .orElse(null);
    }

    private static Double somme(List<Echantillon> echantillons, String nom, String etiquette, String valeur) {
        List<Double> valeurs = echantillons
            .stream()
            .filter(e -> e.nom().equals(nom) && valeur.equals(e.etiquettes().get(etiquette)))
            .map(Echantillon::valeur)
            .filter(v -> Double.isFinite(v) && v >= 0)
            .toList();
        return valeurs.isEmpty() ? null : valeurs.stream().mapToDouble(Double::doubleValue).sum();
    }

    /** Arrondi à deux décimales. */
    public static double arrondi(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
