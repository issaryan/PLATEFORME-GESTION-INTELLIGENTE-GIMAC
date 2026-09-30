package cm.gimac.des.certification.application.port.out;

import java.util.UUID;

/**
 * Pièces probantes conservées par MS-DOCUMENT (WORM) : revérification de l'empreinte déclarée d'une pièce
 * déposée par l'interface ({@code GET /api/v1/interne/documents/{id}/empreinte}) et dépôt pour le compte du
 * participant ({@code POST /api/v1/interne/documents}), jetons de service.
 */
public interface DocumentPort {
    /** Vrai si le document existe et que son empreinte SHA-256 est celle déclarée. */
    boolean empreinteConforme(UUID documentId, String sha256);

    /** Dépôt d'une pièce pour le compte de l'utilisateur ; renvoie sa référence et son empreinte. */
    DocumentDepose deposer(Fichier fichier, String finalite, String reference, String deposantId);

    record Fichier(String nom, String typeMime, byte[] octets) {}

    record DocumentDepose(String id, String sha256) {}
}
