package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/**
 * Livrable d'une étape (§1) : pièce à déposer dans MS-DOCUMENT ; {@code typesPieces} : types MIME admis (vide :
 * tout type) ; {@code modele} : document modèle facultatif (identifiant MS-DOCUMENT).
 */
public record Livrable(String code, String libelle, List<String> typesPieces, boolean obligatoire, String modele) {
    public Livrable {
        typesPieces = typesPieces == null ? List.of() : List.copyOf(typesPieces);
    }
}
