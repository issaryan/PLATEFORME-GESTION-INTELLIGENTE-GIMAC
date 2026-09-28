package cm.gimac.des.document.domain.enumeration;

/**
 * Canal de la consultation : origine de la requête (NetworkZone, REC D-24),
 * ou appel d'un service interne sous mTLS.
 */
public enum ConsultationChannel {
    PORTAIL_DMZ,
    DEPARTEMENT,
    ADMINISTRATION,
    SERVICE_INTERNE,
}
