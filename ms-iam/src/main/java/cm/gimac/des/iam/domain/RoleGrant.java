package cm.gimac.des.iam.domain;

import cm.gimac.des.iam.domain.enumeration.GrantState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Octroi d'un rôle à un collaborateur, avec approbation à quatre yeux
 * (IAM-04, RG-02 : le demandeur et l'approbateur sont distincts).
 */
@Entity
@Table(name = "role_grant")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoleGrant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private GrantState state;

    @NotNull
    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Size(max = 500)
    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    @ManyToOne(optional = false)
    @NotNull
    private Collaborator collaborator;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "permissionses" }, allowSetters = true)
    private Role role;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RoleGrant id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GrantState getState() {
        return this.state;
    }

    public RoleGrant state(GrantState state) {
        this.setState(state);
        return this;
    }

    public void setState(GrantState state) {
        this.state = state;
    }

    public UUID getRequestedBy() {
        return this.requestedBy;
    }

    public RoleGrant requestedBy(UUID requestedBy) {
        this.setRequestedBy(requestedBy);
        return this;
    }

    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return this.requestedAt;
    }

    public RoleGrant requestedAt(Instant requestedAt) {
        this.setRequestedAt(requestedAt);
        return this;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public UUID getApprovedBy() {
        return this.approvedBy;
    }

    public RoleGrant approvedBy(UUID approvedBy) {
        this.setApprovedBy(approvedBy);
        return this;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return this.approvedAt;
    }

    public RoleGrant approvedAt(Instant approvedAt) {
        this.setApprovedAt(approvedAt);
        return this;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getRevokedAt() {
        return this.revokedAt;
    }

    public RoleGrant revokedAt(Instant revokedAt) {
        this.setRevokedAt(revokedAt);
        return this;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevocationReason() {
        return this.revocationReason;
    }

    public RoleGrant revocationReason(String revocationReason) {
        this.setRevocationReason(revocationReason);
        return this;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public Collaborator getCollaborator() {
        return this.collaborator;
    }

    public void setCollaborator(Collaborator collaborator) {
        this.collaborator = collaborator;
    }

    public RoleGrant collaborator(Collaborator collaborator) {
        this.setCollaborator(collaborator);
        return this;
    }

    public Role getRole() {
        return this.role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public RoleGrant role(Role role) {
        this.setRole(role);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleGrant)) {
            return false;
        }
        return getId() != null && getId().equals(((RoleGrant) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoleGrant{" +
            "id=" + getId() +
            ", state='" + getState() + "'" +
            ", requestedBy='" + getRequestedBy() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", approvedBy='" + getApprovedBy() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            ", revocationReason='" + getRevocationReason() + "'" +
            "}";
    }
}
