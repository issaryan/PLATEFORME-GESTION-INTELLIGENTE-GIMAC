package cm.gimac.des.participant.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * participant.status-changed.v1 (D-37) : statut d'une institution modifié après approbation.
 * Les champs institutionId, statut et motif sont ceux lus par MS-HOMOLOGATION (HOM-06).
 */
public record StatusChangedPayload(
    UUID institutionId,
    String participantCode,
    String ancienStatut,
    String statut,
    String motif,
    UUID transitionId,
    Instant dateEffet
) {
    public static final String TYPE = "participant.status-changed.v1";
}
