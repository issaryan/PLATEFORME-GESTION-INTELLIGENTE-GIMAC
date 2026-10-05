package cm.gimac.des.pilotage.application.port.out;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * Documents des rapports en mode DOCUMENT, déposés dans MS-DOCUMENT par la voie des dépôts fragmentés
 * (docs/lot1/depots-fragmentes.md, finalité RAPPORT_ACTIVITE, référence = identifiant du rapport) ;
 * ticket vérifié et contenu lu avec le jeton du client pilotage-service (INTERNAL_DOCUMENT_READ).
 */
public interface DocumentPort {
    String FINALITE = "RAPPORT_ACTIVITE";

    /** Métadonnées d'un document (depots-fragmentes.md §4), champs utiles seulement. */
    record Metadonnees(UUID id, String nomFichier, long taille, String typeMime, String sha256, String finalite, String reference, String deposant) {}

    /** Flux du document, à fermer par l'appelant. */
    record Contenu(InputStream flux, long taille) {}

    /** Vide si MS-DOCUMENT ne connaît pas le document. */
    Optional<Metadonnees> metadonnees(UUID documentId);

    Contenu lire(UUID documentId);
}
