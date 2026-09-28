package cm.gimac.des.monitoring.domain.enumeration;

/**
 * État d'une liaison supervisée (algorithme 6.47).
 */
public enum LinkState {
    NOMINALE,
    DEGRADEE,
    CRITIQUE,
    INJOIGNABLE,
    MAINTENANCE,
}
