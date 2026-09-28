package cm.gimac.des.participant.domain;

import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Transition de statut proposée puis approuvée ou rejetée (maker-checker,
 * algorithme 6.25). Historique immuable.
 */
@Entity
@Table(name = "status_transition")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StatusTransition implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private InstitutionStatus fromStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private InstitutionStatus toStatus;

    @NotNull
    @Size(max = 1000)
    @Column(name = "reason", length = 1000, nullable = false)
    private String reason;

    @NotNull
    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    /**
     * Vrai pour la bascule automatique en A_REGULARISER à J-0 (D-13), sans approbateur.
     */
    @NotNull
    @Column(name = "automatic_transition", nullable = false)
    private Boolean automaticTransition;

    /**
     * 1 en règle générale ; 2 pour la levée de suspension (DG et Risques, RG-14).
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 2)
    @Column(name = "required_approvals", nullable = false)
    private Integer requiredApprovals;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "second_approver_id")
    private UUID secondApproverId;

    @Column(name = "second_approved_at")
    private Instant secondApprovedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private TransitionState state;

    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Pièce de décision hors plateforme dans MS-DOCUMENT (exigée pour SUSPENDU).
     */
    @Column(name = "evidence_document_id")
    private UUID evidenceDocumentId;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution institution;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public StatusTransition id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InstitutionStatus getFromStatus() {
        return this.fromStatus;
    }

    public StatusTransition fromStatus(InstitutionStatus fromStatus) {
        this.setFromStatus(fromStatus);
        return this;
    }

    public void setFromStatus(InstitutionStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public InstitutionStatus getToStatus() {
        return this.toStatus;
    }

    public StatusTransition toStatus(InstitutionStatus toStatus) {
        this.setToStatus(toStatus);
        return this;
    }

    public void setToStatus(InstitutionStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return this.reason;
    }

    public StatusTransition reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getRequesterId() {
        return this.requesterId;
    }

    public StatusTransition requesterId(UUID requesterId) {
        this.setRequesterId(requesterId);
        return this;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public Instant getRequestedAt() {
        return this.requestedAt;
    }

    public StatusTransition requestedAt(Instant requestedAt) {
        this.setRequestedAt(requestedAt);
        return this;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Boolean getAutomaticTransition() {
        return this.automaticTransition;
    }

    public StatusTransition automaticTransition(Boolean automaticTransition) {
        this.setAutomaticTransition(automaticTransition);
        return this;
    }

    public void setAutomaticTransition(Boolean automaticTransition) {
        this.automaticTransition = automaticTransition;
    }

    public Integer getRequiredApprovals() {
        return this.requiredApprovals;
    }

    public StatusTransition requiredApprovals(Integer requiredApprovals) {
        this.setRequiredApprovals(requiredApprovals);
        return this;
    }

    public void setRequiredApprovals(Integer requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    public UUID getApproverId() {
        return this.approverId;
    }

    public StatusTransition approverId(UUID approverId) {
        this.setApproverId(approverId);
        return this;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public UUID getSecondApproverId() {
        return this.secondApproverId;
    }

    public StatusTransition secondApproverId(UUID secondApproverId) {
        this.setSecondApproverId(secondApproverId);
        return this;
    }

    public void setSecondApproverId(UUID secondApproverId) {
        this.secondApproverId = secondApproverId;
    }

    public Instant getSecondApprovedAt() {
        return this.secondApprovedAt;
    }

    public StatusTransition secondApprovedAt(Instant secondApprovedAt) {
        this.setSecondApprovedAt(secondApprovedAt);
        return this;
    }

    public void setSecondApprovedAt(Instant secondApprovedAt) {
        this.secondApprovedAt = secondApprovedAt;
    }

    public Instant getDecidedAt() {
        return this.decidedAt;
    }

    public StatusTransition decidedAt(Instant decidedAt) {
        this.setDecidedAt(decidedAt);
        return this;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public TransitionState getState() {
        return this.state;
    }

    public StatusTransition state(TransitionState state) {
        this.setState(state);
        return this;
    }

    public void setState(TransitionState state) {
        this.state = state;
    }

    public String getRejectionReason() {
        return this.rejectionReason;
    }

    public StatusTransition rejectionReason(String rejectionReason) {
        this.setRejectionReason(rejectionReason);
        return this;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public UUID getEvidenceDocumentId() {
        return this.evidenceDocumentId;
    }

    public StatusTransition evidenceDocumentId(UUID evidenceDocumentId) {
        this.setEvidenceDocumentId(evidenceDocumentId);
        return this;
    }

    public void setEvidenceDocumentId(UUID evidenceDocumentId) {
        this.evidenceDocumentId = evidenceDocumentId;
    }

    public Institution getInstitution() {
        return this.institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public StatusTransition institution(Institution institution) {
        this.setInstitution(institution);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatusTransition)) {
            return false;
        }
        return getId() != null && getId().equals(((StatusTransition) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StatusTransition{" +
            "id=" + getId() +
            ", fromStatus='" + getFromStatus() + "'" +
            ", toStatus='" + getToStatus() + "'" +
            ", reason='" + getReason() + "'" +
            ", requesterId='" + getRequesterId() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", automaticTransition='" + getAutomaticTransition() + "'" +
            ", requiredApprovals=" + getRequiredApprovals() +
            ", approverId='" + getApproverId() + "'" +
            ", secondApproverId='" + getSecondApproverId() + "'" +
            ", secondApprovedAt='" + getSecondApprovedAt() + "'" +
            ", decidedAt='" + getDecidedAt() + "'" +
            ", state='" + getState() + "'" +
            ", rejectionReason='" + getRejectionReason() + "'" +
            ", evidenceDocumentId='" + getEvidenceDocumentId() + "'" +
            "}";
    }
}
