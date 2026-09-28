package cm.gimac.des.iam.service.dto;

import cm.gimac.des.iam.domain.enumeration.GrantState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.iam.domain.RoleGrant} entity.
 */
@Schema(
    description = "Octroi d'un rôle à un collaborateur, avec approbation à quatre yeux\n(IAM-04, RG-02 : le demandeur et l'approbateur sont distincts)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoleGrantDTO implements Serializable {

    private UUID id;

    @NotNull
    private GrantState state;

    @NotNull
    private UUID requestedBy;

    @NotNull
    private Instant requestedAt;

    private UUID approvedBy;

    private Instant approvedAt;

    private Instant revokedAt;

    @Size(max = 500)
    private String revocationReason;

    @NotNull
    private CollaboratorDTO collaborator;

    @NotNull
    private RoleDTO role;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GrantState getState() {
        return state;
    }

    public void setState(GrantState state) {
        this.state = state;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public CollaboratorDTO getCollaborator() {
        return collaborator;
    }

    public void setCollaborator(CollaboratorDTO collaborator) {
        this.collaborator = collaborator;
    }

    public RoleDTO getRole() {
        return role;
    }

    public void setRole(RoleDTO role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleGrantDTO)) {
            return false;
        }

        RoleGrantDTO roleGrantDTO = (RoleGrantDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roleGrantDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoleGrantDTO{" +
            "id='" + getId() + "'" +
            ", state='" + getState() + "'" +
            ", requestedBy='" + getRequestedBy() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", approvedBy='" + getApprovedBy() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            ", revocationReason='" + getRevocationReason() + "'" +
            ", collaborator=" + getCollaborator() +
            ", role=" + getRole() +
            "}";
    }
}
