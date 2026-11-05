package cm.gimac.des.pilotage.domain.processus;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Références lisibles générées par le moteur, selon le gabarit figé dans la version du processus : {CODE}, {AAAA},
 * {MM} et {JJ} facultatifs, séquence {N} (sans zéros, format de la fiche de changement CHG-AAAA-N) ou {NNNNNN}.
 */
public final class ReferenceProcessus {

    private static final Pattern AUTORISE = Pattern.compile("^[A-Z0-9_{}\\-.]+$");
    private static final DateTimeFormatter ANNEE = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MOIS_JOUR = DateTimeFormatter.ofPattern("MM-dd");

    private ReferenceProcessus() {}

    public static String valider(String modele) {
        if (modele == null || modele.isBlank()) {
            return null;
        }
        String m = modele.strip().toUpperCase(java.util.Locale.ROOT);
        boolean sequence = m.contains("{N}") || m.contains("{NNNNNN}");
        if (m.length() > 120 || !AUTORISE.matcher(m).matches() || !m.contains("{AAAA}") || !sequence) {
            throw PilotageException.champ("referenceModele", "Gabarit attendu : AAAA et N (ou NNNNNN), CODE, MM et JJ facultatifs, par exemple CHG-{AAAA}-{N}");
        }
        return m;
    }

    public static String suivante(String modele, String codeProcessus, LocalDate date, long sequence) {
        String m = valider(modele);
        if (m == null) {
            return null;
        }
        String[] moisJour = MOIS_JOUR.format(date).split("-");
        return m.replace("{CODE}", codeProcessus)
            .replace("{AAAA}", ANNEE.format(date))
            .replace("{MM}", moisJour[0])
            .replace("{JJ}", moisJour[1])
            .replace("{NNNNNN}", "%06d".formatted(sequence))
            .replace("{N}", Long.toString(sequence));
    }
}
