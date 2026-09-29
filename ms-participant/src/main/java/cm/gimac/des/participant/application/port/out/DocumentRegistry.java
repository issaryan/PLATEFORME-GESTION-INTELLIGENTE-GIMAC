package cm.gimac.des.participant.application.port.out;

import java.util.UUID;

/**
 * Registre documentaire (MS-DOCUMENT). MS-PARTICIPANT ne reçoit jamais le contenu d'une pièce :
 * il rattache sa référence après avoir vérifié que l'empreinte déclarée est celle du registre.
 */
public interface DocumentRegistry {
    enum Verification {
        CONFORME,
        INCONNUE,
        DIVERGENTE,
    }

    Verification verifier(UUID documentId, String sha256);
}
