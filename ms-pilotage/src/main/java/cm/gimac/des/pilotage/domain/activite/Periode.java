package cm.gimac.des.pilotage.domain.activite;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Période d'un rapport (§2.4) : un jour {@code AAAA-MM-JJ} (QUOTIDIEN) ou une semaine ISO
 * {@code AAAA-Www} (HEBDOMADAIRE), du lundi au dimanche.
 */
public record Periode(TypeRapport type, String texte, LocalDate debut, LocalDate fin) {
    private static final Pattern JOUR = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern SEMAINE = Pattern.compile("^(\\d{4})-W(\\d{2})$");

    public static Periode de(TypeRapport type, String texte) {
        if (type == null) {
            throw PilotageException.champ("type", "Type de rapport absent ou inconnu");
        }
        if (texte == null) {
            throw invalide(texte);
        }
        if (type == TypeRapport.QUOTIDIEN) {
            if (!JOUR.matcher(texte).matches()) {
                throw invalide(texte);
            }
            try {
                LocalDate jour = LocalDate.parse(texte);
                return new Periode(type, texte, jour, jour);
            } catch (DateTimeParseException e) {
                throw invalide(texte);
            }
        }
        Matcher m = SEMAINE.matcher(texte);
        if (!m.matches()) {
            throw invalide(texte);
        }
        int annee = Integer.parseInt(m.group(1));
        int semaine = Integer.parseInt(m.group(2));
        LocalDate reference = LocalDate.of(annee, 1, 4);
        if (semaine < 1 || semaine > reference.range(IsoFields.WEEK_OF_WEEK_BASED_YEAR).getMaximum()) {
            throw invalide(texte);
        }
        LocalDate lundi = reference.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, semaine).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Periode(type, texte, lundi, lundi.plusDays(6));
    }

    /** Période du jour donné, pour un type de rapport. */
    public static Periode contenant(TypeRapport type, LocalDate jour) {
        if (type == TypeRapport.QUOTIDIEN) {
            return new Periode(type, jour.toString(), jour, jour);
        }
        LocalDate lundi = jour.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String texte = String.format("%04d-W%02d", lundi.get(IsoFields.WEEK_BASED_YEAR), lundi.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        return new Periode(type, texte, lundi, lundi.plusDays(6));
    }

    /** Une période commençant après aujourd'hui est future (422 PIL_PERIODE_FUTURE) ; la semaine en cours ne l'est pas. */
    public boolean estFuture(LocalDate aujourdhui) {
        return debut.isAfter(aujourdhui);
    }

    public int semaine() {
        return debut.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    private static PilotageException invalide(String texte) {
        return PilotageException.invalide(
            "PIL_PERIODE_INVALIDE",
            "Période invalide : AAAA-MM-JJ pour un rapport quotidien, AAAA-Www pour un rapport hebdomadaire",
            Map.of("periode", String.valueOf(texte))
        );
    }
}
