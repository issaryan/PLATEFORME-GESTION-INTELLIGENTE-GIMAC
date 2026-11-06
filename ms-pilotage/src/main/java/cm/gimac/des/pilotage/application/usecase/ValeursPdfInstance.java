package cm.gimac.des.pilotage.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Valeurs lisibles du PDF d'une instance (D-139) : libellé traduit des valeurs d'une liste fermée (choix d'étape, liste du
 * formulaire), case cochée en oui ou non, période « début → fin », pièce jointe par son nom, dates à l'heure de Douala.
 */
final class ValeursPdfInstance {

    static final ZoneId DOUALA = ZoneId.of("Africa/Douala");

    /** Code d'énumération (« ERREUR_CONNUE », « FAVORABLE ») : libellé déduit à défaut de traduction connue. */
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+|[A-Z]{5,}");

    /** Libellés des codes des listes fermées des processus livrés (demande de changement, démonstration), fr, en, es. */
    private static final Map<String, String[]> CHOIX = Map.ofEntries(
        Map.entry("ACCEPTEE", new String[] { "Acceptée", "Accepted", "Aceptada" }),
        Map.entry("REFUSEE", new String[] { "Refusée", "Rejected", "Rechazada" }),
        Map.entry("NORMAL", new String[] { "Normale", "Normal", "Normal" }),
        Map.entry("URGENT", new String[] { "Urgente", "Urgent", "Urgente" }),
        Map.entry("CLIENT", new String[] { "Client", "Customer", "Cliente" }),
        Map.entry("ERREUR_CONNUE", new String[] { "Erreur connue", "Known error", "Error conocido" }),
        Map.entry("PROBLEME", new String[] { "Problème", "Problem", "Problema" }),
        Map.entry("PROJET", new String[] { "Projet", "Project", "Proyecto" }),
        Map.entry("SUPPORT_MONETIQUE", new String[] { "Support monétique", "Card payment support", "Soporte monético" }),
        Map.entry("AUTRE_DES", new String[] { "Autre service du DES", "Other DES service", "Otro servicio del DES" }),
        Map.entry("FAVORABLE", new String[] { "Favorable", "Favourable", "Favorable" }),
        Map.entry("RESERVE", new String[] { "Favorable avec réserves", "Favourable with reservations", "Favorable con reservas" }),
        Map.entry("DEFAVORABLE", new String[] { "Défavorable", "Unfavourable", "Desfavorable" })
    );

    private ValeursPdfInstance() {}

    private static int rang(String langue) {
        return switch (langue) {
            case "en" -> 1;
            case "es" -> 2;
            default -> 0;
        };
    }

    /** Valeur d'un champ ; {@code choix} : champ à liste fermée. Vide : chaîne vide. */
    static String lisible(Map<String, String> t, String langue, Object valeur, boolean choix) {
        if (valeur == null) {
            return "";
        }
        if (valeur instanceof Boolean b) {
            return t.get(b ? "oui" : "non");
        }
        if (valeur instanceof Map<?, ?> m) {
            if (m.get("nomFichier") != null) {
                return m.get("nomFichier").toString();
            }
            if (m.containsKey("debut") || m.containsKey("fin")) {
                return date(t, m.get("debut")) + " → " + date(t, m.get("fin"));
            }
        }
        if (valeur instanceof Collection<?> c) {
            return String.join(", ", c.stream().map(x -> lisible(t, langue, x, choix)).toList());
        }
        return choix ? choix(langue, valeur.toString()) : date(t, valeur);
    }

    /** Libellé d'une valeur de liste fermée : traduction connue, sinon code mis en forme, sinon valeur saisie. */
    static String choix(String langue, String code) {
        String[] l = CHOIX.get(code);
        if (l != null) {
            return l[rang(langue)];
        }
        if (CODE.matcher(code).matches()) {
            String x = code.replace('_', ' ').toLowerCase(Locale.ROOT);
            return x.substring(0, 1).toUpperCase(Locale.ROOT) + x.substring(1);
        }
        return code;
    }

    /** Instant, date-heure ISO ou date ISO lisibles, à l'heure de Douala ; toute autre valeur telle quelle. */
    static String date(Map<String, String> t, Object valeur) {
        if (valeur == null) {
            return "—";
        }
        DateTimeFormatter heure = DateTimeFormatter.ofPattern(t.get("heure"), Locale.ROOT);
        if (valeur instanceof Instant x) {
            return heure.format(x.atZone(DOUALA));
        }
        String s = valeur.toString();
        try {
            return heure.format(OffsetDateTime.parse(s).atZoneSameInstant(DOUALA));
        } catch (DateTimeParseException pasUneDateHeure) {
            try {
                return DateTimeFormatter.ofPattern(t.get("jour"), Locale.ROOT).format(LocalDate.parse(s));
            } catch (DateTimeParseException pasUneDate) {
                return s;
            }
        }
    }
}
