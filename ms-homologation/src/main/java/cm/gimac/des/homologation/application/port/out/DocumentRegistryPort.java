package cm.gimac.des.homologation.application.port.out;

import java.util.UUID;

/**
 * Registre documentaire (MS-DOCUMENT, WORM) : revérification de l'empreinte d'une pièce déposée par
 * l'interface avant de la rattacher à une opération, et dépôt d'une pièce reçue du portail pour le compte
 * du correspondant. Aucune pièce n'est conservée par MS-HOMOLOGATION : seulement sa référence et son empreinte.
 */
public interface DocumentRegistryPort {
    enum Verification {
        CONFORME,
        DIVERGENTE,
        INCONNUE,
    }

    /** Pièce déposée : identifiant et empreinte SHA-256 calculée par MS-DOCUMENT. */
    record Depot(UUID id, String sha256, String nomFichier) {}

    Verification verifier(UUID documentId, String sha256);

    /**
     * Type de la pièce déposée (MIME), lu par {@code GET /api/v1/interne/documents/{id}} (D-93 §3 : types de pièces du
     * livrable) ; null si le registre ne le connaît pas.
     */
    default String typeMime(UUID documentId) {
        return null;
    }

    Depot deposer(byte[] contenu, String nomFichier, String typeMime, String finalite, String reference, UUID deposantId);
}
