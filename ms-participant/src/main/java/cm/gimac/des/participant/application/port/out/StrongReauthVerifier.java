package cm.gimac.des.participant.application.port.out;

import java.util.UUID;

/**
 * Vérification d'une réauthentification forte FIDO2 (IAM-07) avant une décision engageante :
 * approbation d'une transition de statut (PAR-02). L'assertion, transmise par l'interface dans
 * l'en-tête X-Reauth-Assertion, est vérifiée par MS-IAM ; elle n'est jamais rejouée.
 */
public interface StrongReauthVerifier {
    /** Vrai si l'assertion est valide, récente et émise par ce sujet pour cette finalité. */
    boolean verifier(String assertion, UUID sujet, String finalite);
}
