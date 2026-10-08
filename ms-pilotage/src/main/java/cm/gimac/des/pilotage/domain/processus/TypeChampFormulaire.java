package cm.gimac.des.pilotage.domain.processus;

/** Type d'un champ du formulaire de lancement (D-98 §2). */
public enum TypeChampFormulaire {
    /** Texte court (500 caractères au plus). */
    TEXTE,
    /** Texte long (4000 caractères au plus). */
    TEXTE_LONG,
    /** Nombre décimal. */
    NOMBRE,
    /** Date AAAA-MM-JJ. */
    DATE,
    /** Période { debut, fin } en AAAA-MM-JJ, fin au plus tôt le jour du début. */
    PERIODE,
    /** Une valeur parmi {@code choix}. */
    LISTE,
    /** Case à cocher (vrai ou faux) ; obligatoire : doit être cochée. */
    CASE,
    /** Pièce jointe déposée par les dépôts fragmentés de MS-DOCUMENT (finalité PROCESSUS_UNITE). */
    PIECE_JOINTE,
}
