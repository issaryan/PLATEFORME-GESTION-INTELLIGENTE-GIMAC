package cm.gimac.des.monitoring.application.port.out;

import java.util.Optional;

/**
 * Pièces déposées dans MS-DOCUMENT (D-116 §10.1) : métadonnées pour vérifier qu'un rapport a bien été déposé par son
 * auteur avec la finalité RAPPORT_MONITORING, et contenu pour le restituer aux personnes qui peuvent le lire.
 */
public interface PiecesPort {
    String FINALITE = "RAPPORT_MONITORING";

    record Piece(String id, String nom, long taille, String type, String finalite, String deposant) {}

    /** Métadonnées de la pièce ; vide si elle n'existe pas. */
    Optional<Piece> lire(String id);

    /** Contenu de la pièce. */
    byte[] contenu(String id);
}
