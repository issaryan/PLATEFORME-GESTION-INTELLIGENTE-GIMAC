package cm.gimac.des.certification.application.port.out;

/**
 * Vérification d'une réauthentification forte FIDO2 (IAM-07, D-35) avant une signature : l'assertion,
 * transmise dans l'en-tête X-Reauth-Assertion, est vérifiée et consommée par MS-IAM
 * ({@code POST /api/v1/interne/reauthentifications/verification}) ; elle n'est jamais rejouable. La finalité des
 * signatures est celle du circuit (gimac.certification.circuits.finalite, SIGNATURE_OPERATION par défaut, D-94).
 */
public interface ReauthentificationPort {
    /** Vrai si l'assertion est valide, récente et émise par ce sujet pour cette finalité. */
    boolean verifier(String assertion, String sujet, String finalite);
}
