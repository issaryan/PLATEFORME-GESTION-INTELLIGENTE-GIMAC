package cm.gimac.des.document.domain.enumeration;

/**
 * Périmètre de visibilité d'une pièce. RESTREINT : seuls le déposant, les
 * lecteurs désignés et les porteurs de la permission RH y accèdent (D-29).
 */
public enum DocumentVisibility {
    TOUTES_INSTITUTIONS,
    UNE_INSTITUTION,
    UN_SERVICE,
    RESTREINT,
}
