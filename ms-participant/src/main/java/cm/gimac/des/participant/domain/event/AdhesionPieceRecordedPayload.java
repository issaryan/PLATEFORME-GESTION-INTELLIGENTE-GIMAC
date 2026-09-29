package cm.gimac.des.participant.domain.event;

import java.util.UUID;

/**
 * participant.adhesion-piece-recorded.v1 : dépôt, validation ou rejet d'une pièce du dossier
 * d'adhésion (D-57). Seules la référence et l'empreinte de la pièce circulent, jamais son contenu.
 */
public record AdhesionPieceRecordedPayload(
    UUID institutionId,
    UUID pieceId,
    String code,
    String etat,
    UUID documentId,
    String documentSha256,
    String motif,
    UUID auteurId
) {
    public static final String TYPE = "participant.adhesion-piece-recorded.v1";
}
