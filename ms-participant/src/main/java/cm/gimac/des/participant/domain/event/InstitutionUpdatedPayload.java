package cm.gimac.des.participant.domain.event;

import java.util.List;
import java.util.UUID;

/** participant.institution-updated.v1 : données descriptives ou conditions d'adhésion modifiées (PAR-01). */
public record InstitutionUpdatedPayload(UUID institutionId, List<String> champs, UUID auteurId) {
    public static final String TYPE = "participant.institution-updated.v1";
}
