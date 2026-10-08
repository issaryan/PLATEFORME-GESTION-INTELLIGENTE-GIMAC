package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/**
 * Champ du formulaire de lancement (D-98 §2) : rempli par l'initiateur, visible des intervenants à chaque étape.
 * {@code choix} : valeurs d'une LISTE ; {@code typesPieces} : types MIME admis d'une PIECE_JOINTE (vide : tout type) ;
 * {@code aide} : texte d'aide affiché sous le champ.
 */
public record ChampFormulaire(
    String code,
    String libelle,
    TypeChampFormulaire type,
    List<String> choix,
    boolean obligatoire,
    String aide,
    List<String> typesPieces
) {
    public ChampFormulaire {
        choix = choix == null ? List.of() : List.copyOf(choix);
        typesPieces = typesPieces == null ? List.of() : List.copyOf(typesPieces);
    }
}
