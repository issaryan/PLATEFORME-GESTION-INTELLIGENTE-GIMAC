package cm.gimac.des.incident.domain.enumeration;

/**
 * Origine d'un ticket (écart CC complété : l'origine n'était qu'une chaîne libre).
 */
public enum TicketOrigin {
    APPLICATION_DES,
    PORTAIL,
    ALERTE_MONITORING,
    ESCALADE_EPUISEE,
    SUSPENS_CONTESTE,
    DIVERGENCE_INTEGRITE,
}
