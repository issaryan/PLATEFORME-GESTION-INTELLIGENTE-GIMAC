package cm.gimac.des.document.domain;

import cm.gimac.des.document.domain.enumeration.PurgeState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Demande de purge à l'échéance, à quatre yeux (DOC-06) : demandeur et
 * approbateur distincts ; impossible sous verrou actif ou conservation légale.
 */
@Entity
@Table(name = "purge_request")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PurgeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @NotNull
    @Size(max = 1000)
    @Column(name = "reason", length = 1000, nullable = false)
    private String reason;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private PurgeState state;

    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "executed_at")
    private Instant executedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "lineage", "policy", "documentLineage" }, allowSetters = true)
    private Document document;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public PurgeRequest id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRequesterId() {
        return this.requesterId;
    }

    public PurgeRequest requesterId(UUID requesterId) {
        this.setRequesterId(requesterId);
        return this;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public String getReason() {
        return this.reason;
    }

    public PurgeRequest reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getRequestedAt() {
        return this.requestedAt;
    }

    public PurgeRequest requestedAt(Instant requestedAt) {
        this.setRequestedAt(requestedAt);
        return this;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public UUID getApproverId() {
        return this.approverId;
    }

    public PurgeRequest approverId(UUID approverId) {
        this.setApproverId(approverId);
        return this;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getApprovedAt() {
        return this.approvedAt;
    }

    public PurgeRequest approvedAt(Instant approvedAt) {
        this.setApprovedAt(approvedAt);
        return this;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public PurgeState getState() {
        return this.state;
    }

    public PurgeRequest state(PurgeState state) {
        this.setState(state);
        return this;
    }

    public void setState(PurgeState state) {
        this.state = state;
    }

    public String getRejectionReason() {
        return this.rejectionReason;
    }

    public PurgeRequest rejectionReason(String rejectionReason) {
        this.setRejectionReason(rejectionReason);
        return this;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getExecutedAt() {
        return this.executedAt;
    }

    public PurgeRequest executedAt(Instant executedAt) {
        this.setExecutedAt(executedAt);
        return this;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public Document getDocument() {
        return this.document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public PurgeRequest document(Document document) {
        this.setDocument(document);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PurgeRequest)) {
            return false;
        }
        return getId() != null && getId().equals(((PurgeRequest) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PurgeRequest{" +
            "id=" + getId() +
            ", requesterId='" + getRequesterId() + "'" +
            ", reason='" + getReason() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", state='" + getState() + "'" +
            ", rejectionReason='" + getRejectionReason() + "'" +
            ", executedAt='" + getExecutedAt() + "'" +
            "}";
    }
}
