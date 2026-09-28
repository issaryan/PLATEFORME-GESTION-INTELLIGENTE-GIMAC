package cm.gimac.des.pilotage.service.dto;

import cm.gimac.des.pilotage.domain.enumeration.ApprovalAuthority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.MilestoneApproval} entity.
 */
@Schema(
    description = "Visa de franchissement d'un jalon (RG-46) : un visa par autorité (Projets,\nSécurité, DES) ; le jalon ne passe ATTEINT qu'avec les trois."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MilestoneApprovalDTO implements Serializable {

    private UUID id;

    @NotNull
    private ApprovalAuthority authority;

    @NotNull
    @Schema(description = "Signataire du visa (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID approverId;

    @NotNull
    private Instant approvedAt;

    @Size(max = 1000)
    private String comment;

    @NotNull
    private MilestoneDTO milestone;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ApprovalAuthority getAuthority() {
        return authority;
    }

    public void setAuthority(ApprovalAuthority authority) {
        this.authority = authority;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public MilestoneDTO getMilestone() {
        return milestone;
    }

    public void setMilestone(MilestoneDTO milestone) {
        this.milestone = milestone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MilestoneApprovalDTO)) {
            return false;
        }

        MilestoneApprovalDTO milestoneApprovalDTO = (MilestoneApprovalDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, milestoneApprovalDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MilestoneApprovalDTO{" +
            "id='" + getId() + "'" +
            ", authority='" + getAuthority() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", comment='" + getComment() + "'" +
            ", milestone=" + getMilestone() +
            "}";
    }
}
