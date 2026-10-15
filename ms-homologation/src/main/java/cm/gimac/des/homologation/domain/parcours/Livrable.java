package cm.gimac.des.homologation.domain.parcours;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Livrable ou preuve attendu d'une opération (§1), avec la fonction qui le fournit : une fonction externe
 * (le participant) le dépose depuis le portail. {@code grain} (D-109) : une pièce pour l'opération, ou une pièce par
 * service inclus au périmètre (absent : l'opération) ; {@code consigne} (D-109) : nature précise et format de la
 * pièce attendue, affichés à qui la dépose.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Livrable(
    String code,
    String libelle,
    NatureLivrable nature,
    Fonction deposant,
    List<String> typesPieces,
    boolean obligatoire,
    GrainLivrable grain,
    String consigne
) {
    /** Longueur maximale d'une consigne. */
    public static final int CONSIGNE_MAX = 600;

    public Livrable {
        typesPieces = typesPieces == null ? List.of() : List.copyOf(typesPieces);
        consigne = consigne == null || consigne.isBlank() ? null : consigne.trim();
    }

    /** Livrable antérieur à D-109 : au grain de l'opération, sans consigne. */
    public Livrable(String code, String libelle, NatureLivrable nature, Fonction deposant, List<String> typesPieces, boolean obligatoire) {
        this(code, libelle, nature, deposant, typesPieces, obligatoire, null, null);
    }

    /** Grain effectif (absent : l'opération). */
    public GrainLivrable grainEffectif() {
        return grain == null ? GrainLivrable.OPERATION : grain;
    }

    /** Une pièce par service inclus au périmètre. */
    public boolean parService() {
        return grainEffectif() == GrainLivrable.SERVICE;
    }
}
