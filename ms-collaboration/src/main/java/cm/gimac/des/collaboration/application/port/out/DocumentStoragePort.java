package cm.gimac.des.collaboration.application.port.out;

import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import java.io.InputStream;
import java.util.UUID;

/**
 * Dépôt des pièces jointes dans MS-DOCUMENT (dépôt WORM, finalité MESSAGERIE, référence = message)
 * et lecture de leur contenu, avec le jeton de service de collaboration-service.
 */
public interface DocumentStoragePort {
    String FINALITE = "MESSAGERIE";

    /** Document déposé : identifiant, empreinte SHA-256 (hexadécimal), nom, taille et type retenus. */
    record DocumentDepose(UUID id, String sha256, String nomFichier, long taille, String typeMime) {}

    /** Flux du document, à fermer par l'appelant. */
    record ContenuDocument(InputStream flux, long taille) {}

    DocumentDepose deposer(FichierJoint fichier, UUID messageId, String deposantId);

    ContenuDocument lire(UUID documentId);
}
