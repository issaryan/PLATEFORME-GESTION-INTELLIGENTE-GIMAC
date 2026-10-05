package cm.gimac.des.pilotage.domain.service;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Règles communes de Mon activité (docs/lot1/mon-activite.md §2) : fuseau Africa/Douala, bornes des
 * intervalles de lecture, champs de texte, jours ouvrés.
 */
public final class ReglesActivite {

    public static final ZoneId FUSEAU = ZoneId.of("Africa/Douala");
    /** Intervalle d'agenda lisible en une requête (§2.1). */
    public static final int AGENDA_JOURS_MAX = 62;
    /** Intervalle d'activités lisible en une requête (§2.2). */
    public static final int ACTIVITES_JOURS_MAX = 31;
    public static final int PARTICIPANTS_MAX = 50;
    public static final int DUREE_MAX = 1440;
    public static final int TITRE_MAX = 200;
    public static final int DESCRIPTION_MAX = 4000;
    public static final int LIEU_MAX = 200;
    public static final int TAILLE_PAGE_MAX = 100;

    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private ReglesActivite() {}

    /** Jour courant dans le fuseau du GIMAC. */
    public static LocalDate aujourdhui(Clock horloge) {
        return LocalDate.now(horloge.withZone(FUSEAU));
    }

    /** Minuit (début) d'un jour, dans le fuseau du GIMAC. */
    public static Instant debutJour(LocalDate jour) {
        return jour.atStartOfDay(FUSEAU).toInstant();
    }

    /** Horodatage ISO 8601 avec le décalage du fuseau du GIMAC, par exemple 2026-10-06T09:00:00+01:00. */
    public static String horodatage(Instant instant) {
        return instant == null ? null : HORODATAGE.format(instant.atZone(FUSEAU).toOffsetDateTime());
    }

    public static Instant lireHorodatage(String champ, String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw PilotageException.champ(champ, "Horodatage absent");
        }
        try {
            return OffsetDateTime.parse(valeur).toInstant();
        } catch (DateTimeParseException e) {
            throw PilotageException.champ(champ, "Horodatage ISO 8601 attendu");
        }
    }

    public static LocalDate lireJour(String champ, String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw PilotageException.champ(champ, "Jour absent");
        }
        try {
            return LocalDate.parse(valeur);
        } catch (DateTimeParseException e) {
            throw PilotageException.champ(champ, "Jour AAAA-MM-JJ attendu");
        }
    }

    /** Jour facultatif : null si absent. */
    public static LocalDate lireJourFacultatif(String champ, String valeur) {
        return valeur == null || valeur.isBlank() ? null : lireJour(champ, valeur);
    }

    /** Intervalle de jours [du, au], au plus {@code max} jours ; 400 PIL_INTERVALLE_INVALIDE sinon. */
    public static void intervalle(LocalDate du, LocalDate au, int max) {
        if (du == null || au == null || au.isBefore(du) || ChronoUnit.DAYS.between(du, au) + 1 > max) {
            throw PilotageException.invalide(
                "PIL_INTERVALLE_INVALIDE",
                "Intervalle invalide : du ≤ au, " + max + " jours au plus",
                Map.of("maximum", max)
            );
        }
    }

    /** Texte obligatoire, débarrassé des espaces de bord, de 1 à {@code max} caractères. */
    public static String texteObligatoire(String champ, String valeur, int max) {
        String v = valeur == null ? "" : valeur.strip();
        if (v.isEmpty() || v.length() > max) {
            throw PilotageException.champ(champ, "Texte de 1 à " + max + " caractères attendu");
        }
        return v;
    }

    /** Texte facultatif : null si vide, {@code max} caractères au plus. */
    public static String texteFacultatif(String champ, String valeur, int max) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String v = valeur.strip();
        if (v.length() > max) {
            throw PilotageException.champ(champ, "Texte de " + max + " caractères au plus attendu");
        }
        return v;
    }

    /** Valeur d'énumération, ou la valeur par défaut si absente ; 400 si inconnue. */
    public static <E extends Enum<E>> E valeur(String champ, Class<E> type, String valeur, E parDefaut) {
        if (valeur == null || valeur.isBlank()) {
            if (parDefaut == null) {
                throw PilotageException.champ(champ, "Valeur absente");
            }
            return parDefaut;
        }
        try {
            return Enum.valueOf(type, valeur.strip());
        } catch (IllegalArgumentException e) {
            throw PilotageException.champ(champ, "Valeur inconnue : " + valeur);
        }
    }

    /** Durée passée sur une activité : 0 à 1440 minutes, facultative. */
    public static Integer duree(Integer minutes) {
        if (minutes != null && (minutes < 0 || minutes > DUREE_MAX)) {
            throw PilotageException.champ("dureeMinutes", "Durée de 0 à " + DUREE_MAX + " minutes");
        }
        return minutes;
    }

    /**
     * Bornes d'un événement (§2.1) : fin postérieure au début ; journée entière : début et fin à minuit
     * (fuseau du GIMAC), fin exclusive.
     */
    public static void bornesEvenement(Instant debut, Instant fin, boolean journeeEntiere) {
        if (!fin.isAfter(debut)) {
            throw PilotageException.champ("fin", "La fin doit suivre le début");
        }
        if (journeeEntiere && !(aMinuit(debut) && aMinuit(fin))) {
            throw PilotageException.champ("debut", "Journée entière : début et fin à minuit (Africa/Douala), fin exclusive");
        }
    }

    private static boolean aMinuit(Instant instant) {
        ZonedDateTime z = instant.atZone(FUSEAU);
        return z.toLocalTime().equals(LocalTime.MIDNIGHT);
    }

    /** Jour ouvré suivant (lundi à vendredi). */
    public static LocalDate jourOuvreSuivant(LocalDate jour) {
        LocalDate j = jour.plusDays(1);
        while (j.getDayOfWeek() == DayOfWeek.SATURDAY || j.getDayOfWeek() == DayOfWeek.SUNDAY) {
            j = j.plusDays(1);
        }
        return j;
    }

    /** Taille de page bornée (1 à 100, 20 par défaut) et numéro de page positif. */
    public static int taille(Integer taille) {
        if (taille == null) {
            return 20;
        }
        return Math.max(1, Math.min(TAILLE_PAGE_MAX, taille));
    }

    public static int page(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }
}
