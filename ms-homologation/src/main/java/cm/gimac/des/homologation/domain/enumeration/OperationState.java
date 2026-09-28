package cm.gimac.des.homologation.domain.enumeration;

/**
 * Automate de l'opération (REC D-08) : HORS_PERIMETRE (algorithme 6.30) et
 * ARCHIVEE (algorithme 6.34) complètent les six états du CC.
 */
public enum OperationState {
    VERROUILLEE,
    ELIGIBLE,
    EN_COURS,
    EN_VALIDATION,
    CLOSE,
    EN_ARBITRAGE,
    HORS_PERIMETRE,
    ARCHIVEE,
}
