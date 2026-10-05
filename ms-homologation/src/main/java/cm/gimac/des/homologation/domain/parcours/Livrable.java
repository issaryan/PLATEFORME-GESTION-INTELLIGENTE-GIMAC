package cm.gimac.des.homologation.domain.parcours;

import java.util.List;

/**
 * Livrable ou preuve attendu d'une opération (§1), avec la fonction qui le fournit : une fonction externe
 * (le participant) le dépose depuis le portail.
 */
public record Livrable(String code, String libelle, NatureLivrable nature, Fonction deposant, List<String> typesPieces, boolean obligatoire) {
    public Livrable {
        typesPieces = typesPieces == null ? List.of() : List.copyOf(typesPieces);
    }
}
