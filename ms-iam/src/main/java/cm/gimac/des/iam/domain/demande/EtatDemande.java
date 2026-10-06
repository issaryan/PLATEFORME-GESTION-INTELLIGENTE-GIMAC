package cm.gimac.des.iam.domain.demande;

/** État d'une demande à l'administration (§4) ; seule une demande EN_ATTENTE peut être décidée ou annulée. */
public enum EtatDemande {
    EN_ATTENTE,
    ACCEPTEE,
    REFUSEE,
    ANNULEE,
}
