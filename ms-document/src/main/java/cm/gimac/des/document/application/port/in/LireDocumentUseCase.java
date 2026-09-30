package cm.gimac.des.document.application.port.in;

import java.util.UUID;

/**
 * Port entrant de la lecture d'un document déposé par un service interne (pièces jointes de la
 * messagerie, D-64) : contenu conservé dans le dépôt WORM, vérifié contre l'empreinte inscrite
 * au dépôt, et lecture journalisée avec le client du service lecteur.
 */
public interface LireDocumentUseCase {
    /** Document relu : métadonnées inscrites au dépôt et contenu. */
    record Contenu(UUID id, String nomFichier, String typeMime, String sha256, byte[] octets) {
        public long taille() {
            return octets.length;
        }
    }

    /**
     * Contenu du document {@code id} lu pour le service {@code lecteur} ; DOC_NOT_FOUND (404) s'il
     * est inconnu, DOC_CONTENT_MISSING ou DOC_INTEGRITY_MISMATCH (500) si l'objet conservé manque
     * ou ne correspond plus à l'empreinte inscrite.
     */
    Contenu lire(UUID id, String lecteur);
}
