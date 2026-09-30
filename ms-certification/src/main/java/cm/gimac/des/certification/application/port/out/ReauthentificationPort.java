package cm.gimac.des.certification.application.port.out;

/**
 * Vérification d'une réauthentification forte FIDO2 (IAM-07, D-35) avant une signature : l'assertion,
 * transmise dans l'en-tête X-Reauth-Assertion, est vérifiée et consommée par MS-IAM
 * ({@code POST /api/v1/interne/reauthentifications/verification}) ; elle n'est jamais rejouable.
 */
public interface ReauthentificationPort {
    /** Finalité du défi WebAuthn demandé par l'interface pour les signatures des verrous. */
    String FINALITE_SIGNATURE = "QUITUS_SIGN";

    /** Vrai si l'assertion est valide, récente et émise par ce sujet pour cette finalité. */
    boolean verifier(String assertion, String sujet, String finalite);
}
