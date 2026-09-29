package cm.gimac.des.participant.domain.event;

import java.util.UUID;

/** participant.transition-requested.v1 : proposition de transition en attente d'approbation (6.25, étape 2). */
public record TransitionRequestedPayload(
    UUID institutionId,
    UUID transitionId,
    String de,
    String vers,
    String motif,
    UUID demandeurId,
    int approbationsRequises
) {
    public static final String TYPE = "participant.transition-requested.v1";
}
