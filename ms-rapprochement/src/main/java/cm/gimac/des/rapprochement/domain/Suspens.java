package cm.gimac.des.rapprochement.domain;

import cm.gimac.des.rapprochement.domain.enumeration.DiscrepancyType;
import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Dossier de suspens : un par écart (algorithme 6.50). Montants, typologie
 * et clé pivot sont immuables (RG-44) ; idempotence par (rapprochement, pivot, typologie).
 */
@Entity
@Table(name = "suspens")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Suspens implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Référence lisible SUS-<année>-<jjjj>-<séquence>, par exemple SUS-2026-0913-001.
     */
    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^SUS-[0-9]{4}-[0-9]{4}-[0-9]{3,}$")
    @Column(name = "reference", length = 30, nullable = false, unique = true)
    private String reference;

    @NotNull
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DiscrepancyType type;

    @NotNull
    @Size(max = 64)
    @Column(name = "pivot_transaction_ref", length = 64, nullable = false)
    private String pivotTransactionRef;

    @Size(max = 64)
    @Column(name = "pivot_authorization_ref", length = 64)
    private String pivotAuthorizationRef;

    @NotNull
    @Column(name = "pivot_operation_at", nullable = false)
    private Instant pivotOperationAt;

    @Column(name = "pivot_amount", precision = 21, scale = 2)
    private BigDecimal pivotAmount;

    @NotNull
    @Size(max = 20)
    @Column(name = "pivot_participant_code", length = 20, nullable = false)
    private String pivotParticipantCode;

    /**
     * Montant côté commutateur, vide si absent.
     */
    @Column(name = "amount_switch", precision = 21, scale = 2)
    private BigDecimal amountSwitch;

    /**
     * Montant côté participant, vide si absent.
     */
    @Column(name = "amount_participant", precision = 21, scale = 2)
    private BigDecimal amountParticipant;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private SuspensState state;

    /**
     * Responsable de régularisation (interlocuteur règlement ou responsable compensation).
     */
    @Column(name = "assignee_id")
    private UUID assigneeId;

    /**
     * Échéance : J+5 ouvré par défaut.
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotNull
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    /**
     * Ticket de litige ouvert par MS-INCIDENT sur contestation.
     */
    @Column(name = "dispute_ticket_id")
    private UUID disputeTicketId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "switchStatement", "participantStatement", "settlementStatement" }, allowSetters = true)
    private Reconciliation reconciliation;

    @JsonIgnoreProperties(value = { "suspens" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "suspens")
    private RegularizationAttestation regularizationAttestation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Suspens id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return this.reference;
    }

    public Suspens reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Suspens institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public DiscrepancyType getType() {
        return this.type;
    }

    public Suspens type(DiscrepancyType type) {
        this.setType(type);
        return this;
    }

    public void setType(DiscrepancyType type) {
        this.type = type;
    }

    public String getPivotTransactionRef() {
        return this.pivotTransactionRef;
    }

    public Suspens pivotTransactionRef(String pivotTransactionRef) {
        this.setPivotTransactionRef(pivotTransactionRef);
        return this;
    }

    public void setPivotTransactionRef(String pivotTransactionRef) {
        this.pivotTransactionRef = pivotTransactionRef;
    }

    public String getPivotAuthorizationRef() {
        return this.pivotAuthorizationRef;
    }

    public Suspens pivotAuthorizationRef(String pivotAuthorizationRef) {
        this.setPivotAuthorizationRef(pivotAuthorizationRef);
        return this;
    }

    public void setPivotAuthorizationRef(String pivotAuthorizationRef) {
        this.pivotAuthorizationRef = pivotAuthorizationRef;
    }

    public Instant getPivotOperationAt() {
        return this.pivotOperationAt;
    }

    public Suspens pivotOperationAt(Instant pivotOperationAt) {
        this.setPivotOperationAt(pivotOperationAt);
        return this;
    }

    public void setPivotOperationAt(Instant pivotOperationAt) {
        this.pivotOperationAt = pivotOperationAt;
    }

    public BigDecimal getPivotAmount() {
        return this.pivotAmount;
    }

    public Suspens pivotAmount(BigDecimal pivotAmount) {
        this.setPivotAmount(pivotAmount);
        return this;
    }

    public void setPivotAmount(BigDecimal pivotAmount) {
        this.pivotAmount = pivotAmount;
    }

    public String getPivotParticipantCode() {
        return this.pivotParticipantCode;
    }

    public Suspens pivotParticipantCode(String pivotParticipantCode) {
        this.setPivotParticipantCode(pivotParticipantCode);
        return this;
    }

    public void setPivotParticipantCode(String pivotParticipantCode) {
        this.pivotParticipantCode = pivotParticipantCode;
    }

    public BigDecimal getAmountSwitch() {
        return this.amountSwitch;
    }

    public Suspens amountSwitch(BigDecimal amountSwitch) {
        this.setAmountSwitch(amountSwitch);
        return this;
    }

    public void setAmountSwitch(BigDecimal amountSwitch) {
        this.amountSwitch = amountSwitch;
    }

    public BigDecimal getAmountParticipant() {
        return this.amountParticipant;
    }

    public Suspens amountParticipant(BigDecimal amountParticipant) {
        this.setAmountParticipant(amountParticipant);
        return this;
    }

    public void setAmountParticipant(BigDecimal amountParticipant) {
        this.amountParticipant = amountParticipant;
    }

    public SuspensState getState() {
        return this.state;
    }

    public Suspens state(SuspensState state) {
        this.setState(state);
        return this;
    }

    public void setState(SuspensState state) {
        this.state = state;
    }

    public UUID getAssigneeId() {
        return this.assigneeId;
    }

    public Suspens assigneeId(UUID assigneeId) {
        this.setAssigneeId(assigneeId);
        return this;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public Suspens dueDate(LocalDate dueDate) {
        this.setDueDate(dueDate);
        return this;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getOpenedAt() {
        return this.openedAt;
    }

    public Suspens openedAt(Instant openedAt) {
        this.setOpenedAt(openedAt);
        return this;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public UUID getDisputeTicketId() {
        return this.disputeTicketId;
    }

    public Suspens disputeTicketId(UUID disputeTicketId) {
        this.setDisputeTicketId(disputeTicketId);
        return this;
    }

    public void setDisputeTicketId(UUID disputeTicketId) {
        this.disputeTicketId = disputeTicketId;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public Suspens closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Reconciliation getReconciliation() {
        return this.reconciliation;
    }

    public void setReconciliation(Reconciliation reconciliation) {
        this.reconciliation = reconciliation;
    }

    public Suspens reconciliation(Reconciliation reconciliation) {
        this.setReconciliation(reconciliation);
        return this;
    }

    public RegularizationAttestation getRegularizationAttestation() {
        return this.regularizationAttestation;
    }

    public void setRegularizationAttestation(RegularizationAttestation regularizationAttestation) {
        if (this.regularizationAttestation != null) {
            this.regularizationAttestation.setSuspens(null);
        }
        if (regularizationAttestation != null) {
            regularizationAttestation.setSuspens(this);
        }
        this.regularizationAttestation = regularizationAttestation;
    }

    public Suspens regularizationAttestation(RegularizationAttestation regularizationAttestation) {
        this.setRegularizationAttestation(regularizationAttestation);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Suspens)) {
            return false;
        }
        return getId() != null && getId().equals(((Suspens) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Suspens{" +
            "id=" + getId() +
            ", reference='" + getReference() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", type='" + getType() + "'" +
            ", pivotTransactionRef='" + getPivotTransactionRef() + "'" +
            ", pivotAuthorizationRef='" + getPivotAuthorizationRef() + "'" +
            ", pivotOperationAt='" + getPivotOperationAt() + "'" +
            ", pivotAmount=" + getPivotAmount() +
            ", pivotParticipantCode='" + getPivotParticipantCode() + "'" +
            ", amountSwitch=" + getAmountSwitch() +
            ", amountParticipant=" + getAmountParticipant() +
            ", state='" + getState() + "'" +
            ", assigneeId='" + getAssigneeId() + "'" +
            ", dueDate='" + getDueDate() + "'" +
            ", openedAt='" + getOpenedAt() + "'" +
            ", disputeTicketId='" + getDisputeTicketId() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            "}";
    }
}
