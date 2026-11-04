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

    /**
     * Dépôt pour le compte de l'utilisateur {@code deposantId} : un identifiant absent n'est jamais remplacé, le registre
     * le refuse (DOC_DEPOSITOR_INVALID, traçabilité du déposant réel).
     */
    Depot deposer(byte[] contenu, String nomFichier, String typeMime, String finalite, String reference, UUID deposantId);

    /**
     * Dépôt par MS-HOMOLOGATION lui-même, sans utilisateur (relève de la sandbox, US-EXI-14) : le déposant est le compte
     * de service du client Keycloak de MS-HOMOLOGATION.
     */
    default Depot deposerParLeService(byte[] contenu, String nomFichier, String typeMime, String finalite, String reference) {
        return deposer(contenu, nomFichier, typeMime, finalite, reference, null);
    }

    /** Contenu d'une pièce (D-109 : téléchargement depuis le dossier de la liaison, empreinte revérifiée par l'appelant). */
    record Contenu(byte[] octets, String nomFichier, String typeMime) {}

    /**
     * Contenu d'une pièce, lu par {@code GET /api/v1/interne/documents/{id}/contenu} (INTERNAL_DOCUMENT_READ) ; vide si
     * le registre ne la connaît pas.
     */
    default java.util.Optional<Contenu> lire(UUID documentId) {
        return java.util.Optional.empty();
    }

    /**
     * Pièce déjà conservée de même empreinte SHA-256 (US-EXI-14 : bilan déposé à la clôture par SBX-CAMPAGNE, que la
     * relève reconnaît sans le redéposer) ; vide si le registre n'en conserve aucune ou ne sait pas répondre.
     */
    default java.util.Optional<UUID> parEmpreinte(String sha256) {
        return java.util.Optional.empty();
    }
}
