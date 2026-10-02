package cm.gimac.des.collaboration.domain.messagerie;

/**
 * Événement de la vie d'un groupe, inscrit dans le fil comme un message sans contenu (D-68) : l'auteur
 * est la personne qui agit, les cibles sont les noms concernés (membres ajoutés ou retiré, nouveau nom).
 */
public enum EvenementGroupe {
    GROUPE_CREE,
    GROUPE_RENOMME,
    MEMBRES_AJOUTES,
    MEMBRE_RETIRE,
    MEMBRE_PARTI,
}
