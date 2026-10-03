package cm.gimac.des.document.application.port.in;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Port entrant de la lecture d'un document par un service interne (pièces jointes de la
 * messagerie, D-64 ; tickets des dépôts fragmentés, D-74) : métadonnées inscrites au dépôt, et
 * contenu conservé dans le dépôt WORM, servi en flux et vérifié contre l'empreinte inscrite au
 * fil de la lecture ; chaque lecture du contenu est journalisée avec le client du service lecteur.
 */
public interface LireDocumentUseCase {
    /**
     * Document relu : métadonnées inscrites au dépôt et contenu en flux, à fermer par le lecteur.
     * Le flux échoue (IOException) avant de rendre ses derniers octets si le contenu lu ne
     * correspond pas à l'empreinte inscrite : un contenu altéré n'est jamais restitué en entier.
     */
    record Contenu(UUID id, String nomFichier, String typeMime, String sha256, long taille, InputStream flux) {}

    /** Métadonnées d'un document (docs/lot1/depots-fragmentes.md §4). */
    record Metadonnees(
        UUID id,
        String nomFichier,
        long taille,
        String typeMime,
        String sha256,
        String finalite,
        String reference,
        String deposant,
        String institutionId,
        Instant deposeLe
    ) {}

    /**
     * Contenu du document {@code id} lu pour le service {@code lecteur} ; DOC_NOT_FOUND (404) s'il
     * est inconnu, DOC_CONTENT_MISSING ou DOC_INTEGRITY_MISMATCH (500) si l'objet conservé manque
     * ou n'a pas la taille inscrite.
     */
    Contenu lire(UUID id, String lecteur);

    /** Métadonnées du document {@code id} ; DOC_NOT_FOUND (404) s'il est inconnu. */
    Metadonnees metadonnees(UUID id);
}
