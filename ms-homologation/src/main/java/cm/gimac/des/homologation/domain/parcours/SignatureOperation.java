package cm.gimac.des.homologation.domain.parcours;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Signature exigée à la clôture d'une opération du référentiel (D-91, docs/lot1/types-projet.md §3) : code unique
 * dans l'opération, libellé, fonction du signataire (catalogue), ordre (croissant ; même ordre : indifférent) et
 * livrables portés. Une référence de {@code porte} est soit le code d'un livrable de l'opération ({@code L1}), soit
 * {@code CODE_OPERATION/CODE_LIVRABLE} pour un livrable d'une opération antérieure (prédécesseur transitif).
 */
public record SignatureOperation(String code, String libelle, Fonction signataire, Integer ordre, List<String> porte) {
    /** Code d'une signature : majuscule, puis 1 à 39 majuscules, chiffres ou soulignés. */
    public static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,39}$");

    /** Séparateur d'une référence à un livrable d'une opération antérieure. */
    public static final String SEPARATEUR = "/";

    public static final int ORDRE_MAX = 99;

    public SignatureOperation {
        porte = porte == null ? List.of() : List.copyOf(porte);
    }

    /** Ordre effectif (absent : 1). */
    public int ordreEffectif() {
        return ordre == null ? 1 : ordre;
    }

    /** Code de l'opération d'une référence portée ({@code null} : l'opération elle-même). */
    public static String operationDe(String reference) {
        int i = reference.indexOf(SEPARATEUR);
        return i < 0 ? null : reference.substring(0, i);
    }

    /** Code du livrable d'une référence portée. */
    public static String livrableDe(String reference) {
        int i = reference.indexOf(SEPARATEUR);
        return i < 0 ? reference : reference.substring(i + 1);
    }
}
