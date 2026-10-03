package cm.gimac.des.document.domain.enumeration;

/**
 * Cycle de vie d'un dépôt fragmenté (D-74, docs/lot1/depots-fragmentes.md §3) : ouvert tant que les
 * parties arrivent, en vérification une fois assemblé, puis disponible (document créé), rejeté
 * (motif) ou abandonné (par le déposant ou faute d'activité).
 */
public enum EtatDepotFragmente {
    OUVERT,
    EN_VERIFICATION,
    DISPONIBLE,
    REJETE,
    ABANDONNE,
}
