package cm.gimac.des.homologation.application.port.out;

import java.util.UUID;

/**
 * Réauthentification forte FIDO2 (IAM-07) avant une décision engageante : validation d'une opération.
 * L'assertion, transmise par l'interface dans l'en-tête X-Reauth-Assertion, est vérifiée et consommée
 * par MS-IAM ; elle n'est jamais rejouée.
 */
public interface StrongReauthPort {
    /** Vrai si l'assertion est valide, récente et émise par ce sujet pour cette finalité. */
    boolean verifier(String assertion, UUID sujet, String finalite);
}
