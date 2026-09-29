package cm.gimac.des.participant.application.port.out;

import java.util.UUID;

/** Port sortant : vérification d'une réauthentification forte FIDO2 auprès de MS-IAM (IAM-07). */
public interface StrongReauthVerifier {
    boolean verifier(String assertion, UUID sujet, String finalite);
}
