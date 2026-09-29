package cm.gimac.des.participant.application.port.out;

import java.util.UUID;

/** Port sortant : vérification d'une pièce déposée dans MS-DOCUMENT (référence et empreinte). */
public interface DocumentRegistry {
    enum Verification {
        CONFORME,
        DIVERGENTE,
        INCONNUE,
    }

    Verification verifier(UUID documentId, String sha256);
}
