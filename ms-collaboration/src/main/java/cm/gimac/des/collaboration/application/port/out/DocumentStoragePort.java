package cm.gimac.des.collaboration.application.port.out;

import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt des pièces jointes dans MS-DOCUMENT (dépôt WORM, finalité MESSAGERIE, référence = message),
 * lecture des métadonnées d'un document (tickets des dépôts fragmentés, D-74) et de son contenu, avec
 * le jeton de service de collaboration-service.
 */
public interface DocumentStoragePort {
    String FINALITE = "MESSAGERIE";

    /** Document déposé : identifiant, empreinte SHA-256 (hexadécimal), nom, taille et type retenus. */
    record DocumentDepose(UUID id, String sha256, String nomFichier, long taille, String typeMime) {}

    /** Métadonnées d'un document (docs/lot1/depots-fragmentes.md §4), finalité, référence et déposant compris. */
    record MetadonneesDocument(
        UUID id,
        String nomFichier,
        long taille,
        String typeMime,
        String sha256,
        String finalite,
        String reference,
        String deposant
    ) {}

    /** Flux du document, à fermer par l'appelant. */
    record ContenuDocument(InputStream flux, long taille) {}

    DocumentDepose deposer(FichierJoint fichier, UUID messageId, String deposantId);

    /** Métadonnées d'un document ; vide s'il est inconnu de MS-DOCUMENT. */
    Optional<MetadonneesDocument> metadonnees(UUID documentId);

    ContenuDocument lire(UUID documentId);
}
