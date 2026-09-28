package cm.gimac.des.iam.service.dto;

import cm.gimac.des.iam.domain.enumeration.DelegationState;
import cm.gimac.des.iam.domain.enumeration.DelegationType;
import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.iam.domain.Delegation} entity.
 */
@Schema(description = "Délégation temporaire d'un niveau supérieur (D-20, RG-01, IAM-06).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DelegationDTO implements Serializable {

    private UUID id;

    @NotNull
    private DelegationType type;

    @NotNull
    @Size(max = 500)
    private String reason;

    @NotNull
    private HabilitationLevel delegatedLevel;

    @NotNull
    private UUID requesterId;

    private UUID approverId;

    @NotNull
    private Instant startsAt;

    @NotNull
    private Instant endsAt;

    @NotNull
    private DelegationState state;

    @NotNull
    private CollaboratorDTO collaborator;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DelegationType getType() {
        return type;
    }

    public void setType(DelegationType type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public HabilitationLevel getDelegatedLevel() {
        return delegatedLevel;
    }

    public void setDelegatedLevel(HabilitationLevel delegatedLevel) {
        this.delegatedLevel = delegatedLevel;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public DelegationState getState() {
        return state;
    }

    public void setState(DelegationState state) {
        this.state = state;
    }

    public CollaboratorDTO getCollaborator() {
        return collaborator;
    }

    public void setCollaborator(CollaboratorDTO collaborator) {
        this.collaborator = collaborator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DelegationDTO)) {
            return false;
        }

        DelegationDTO delegationDTO = (DelegationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, delegationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DelegationDTO{" +
            "id='" + getId() + "'" +
            ", type='" + getType() + "'" +
            ", reason='" + getReason() + "'" +
            ", delegatedLevel='" + getDelegatedLevel() + "'" +
            ", requesterId='" + getRequesterId() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            ", state='" + getState() + "'" +
            ", collaborator=" + getCollaborator() +
            "}";
    }
}
