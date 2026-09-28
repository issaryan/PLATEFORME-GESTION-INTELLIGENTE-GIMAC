package cm.gimac.des.iam.domain;

import cm.gimac.des.iam.domain.enumeration.DelegationState;
import cm.gimac.des.iam.domain.enumeration.DelegationType;
import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Délégation temporaire d'un niveau supérieur (D-20, RG-01, IAM-06).
 */
@Entity
@Table(name = "delegation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Delegation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DelegationType type;

    @NotNull
    @Size(max = 500)
    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "delegated_level", nullable = false)
    private HabilitationLevel delegatedLevel;

    @NotNull
    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "approver_id")
    private UUID approverId;

    @NotNull
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @NotNull
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private DelegationState state;

    @ManyToOne(optional = false)
    @NotNull
    private Collaborator collaborator;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Delegation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DelegationType getType() {
        return this.type;
    }

    public Delegation type(DelegationType type) {
        this.setType(type);
        return this;
    }

    public void setType(DelegationType type) {
        this.type = type;
    }

    public String getReason() {
        return this.reason;
    }

    public Delegation reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public HabilitationLevel getDelegatedLevel() {
        return this.delegatedLevel;
    }

    public Delegation delegatedLevel(HabilitationLevel delegatedLevel) {
        this.setDelegatedLevel(delegatedLevel);
        return this;
    }

    public void setDelegatedLevel(HabilitationLevel delegatedLevel) {
        this.delegatedLevel = delegatedLevel;
    }

    public UUID getRequesterId() {
        return this.requesterId;
    }

    public Delegation requesterId(UUID requesterId) {
        this.setRequesterId(requesterId);
        return this;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public UUID getApproverId() {
        return this.approverId;
    }

    public Delegation approverId(UUID approverId) {
        this.setApproverId(approverId);
        return this;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getStartsAt() {
        return this.startsAt;
    }

    public Delegation startsAt(Instant startsAt) {
        this.setStartsAt(startsAt);
        return this;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return this.endsAt;
    }

    public Delegation endsAt(Instant endsAt) {
        this.setEndsAt(endsAt);
        return this;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public DelegationState getState() {
        return this.state;
    }

    public Delegation state(DelegationState state) {
        this.setState(state);
        return this;
    }

    public void setState(DelegationState state) {
        this.state = state;
    }

    public Collaborator getCollaborator() {
        return this.collaborator;
    }

    public void setCollaborator(Collaborator collaborator) {
        this.collaborator = collaborator;
    }

    public Delegation collaborator(Collaborator collaborator) {
        this.setCollaborator(collaborator);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Delegation)) {
            return false;
        }
        return getId() != null && getId().equals(((Delegation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Delegation{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", reason='" + getReason() + "'" +
            ", delegatedLevel='" + getDelegatedLevel() + "'" +
            ", requesterId='" + getRequesterId() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            ", state='" + getState() + "'" +
            "}";
    }
}
