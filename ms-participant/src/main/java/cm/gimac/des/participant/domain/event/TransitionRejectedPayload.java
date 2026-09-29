package cm.gimac.des.participant.domain.event;

import java.util.UUID;

/** participant.transition-rejected.v1 : proposition rejetée par l'approbateur, avec motif (PAR-02, critère 2). */
public record TransitionRejectedPayload(UUID institutionId, UUID transitionId, String de, String vers, String motif, UUID approbateurId) {
    public static final String TYPE = "participant.transition-rejected.v1";
}
