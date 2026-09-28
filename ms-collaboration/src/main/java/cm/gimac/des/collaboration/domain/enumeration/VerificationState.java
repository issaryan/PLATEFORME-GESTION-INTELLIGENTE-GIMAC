package cm.gimac.des.collaboration.domain.enumeration;

/**
 * État de vérification d'un terminal : seul VERIFIE reçoit des clés.
 */
public enum VerificationState {
    NON_VERIFIE,
    VERIFIE,
    REVOQUE,
}
