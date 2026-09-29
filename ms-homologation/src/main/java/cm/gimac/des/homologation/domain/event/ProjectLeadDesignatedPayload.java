package cm.gimac.des.homologation.domain.event;

import java.util.UUID;

/** homologation.liaison-lead-designated.v1 : conducteur de projet désigné ou remplacé (D-58, rang 6). */
public record ProjectLeadDesignatedPayload(UUID liaisonId, UUID institutionId, UUID ancienConducteurId, UUID conducteurId, UUID designePar, String motif) {
    public static final String TYPE = "homologation.liaison-lead-designated.v1";
}
