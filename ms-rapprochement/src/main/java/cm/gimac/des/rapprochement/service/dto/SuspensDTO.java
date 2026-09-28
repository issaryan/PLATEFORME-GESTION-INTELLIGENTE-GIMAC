package cm.gimac.des.rapprochement.service.dto;

import cm.gimac.des.rapprochement.domain.enumeration.DiscrepancyType;
import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.Suspens} entity.
 */
@Schema(
    description = "Dossier de suspens : un par écart (algorithme 6.50). Montants, typologie\net clé pivot sont immuables (RG-44) ; idempotence par (rapprochement, pivot, typologie)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SuspensDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^SUS-[0-9]{4}-[0-9]{4}-[0-9]{3,}$")
    @Schema(
        description = "Référence lisible SUS-<année>-<jjjj>-<séquence>, par exemple SUS-2026-0913-001.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String reference;

    @NotNull
    private UUID institutionId;

    @NotNull
    private DiscrepancyType type;

    @NotNull
    @Size(max = 64)
    private String pivotTransactionRef;

    @Size(max = 64)
    private String pivotAuthorizationRef;

    @NotNull
    private Instant pivotOperationAt;

    private BigDecimal pivotAmount;

    @NotNull
    @Size(max = 20)
    private String pivotParticipantCode;

    @Schema(description = "Montant côté commutateur, vide si absent.")
    private BigDecimal amountSwitch;

    @Schema(description = "Montant côté participant, vide si absent.")
    private BigDecimal amountParticipant;

    @NotNull
    private SuspensState state;

    @Schema(description = "Responsable de régularisation (interlocuteur règlement ou responsable compensation).")
    private UUID assigneeId;

    @Schema(description = "Échéance : J+5 ouvré par défaut.")
    private LocalDate dueDate;

    @NotNull
    private Instant openedAt;

    @Schema(description = "Ticket de litige ouvert par MS-INCIDENT sur contestation.")
    private UUID disputeTicketId;

    private Instant closedAt;

    @NotNull
    private ReconciliationDTO reconciliation;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public DiscrepancyType getType() {
        return type;
    }

    public void setType(DiscrepancyType type) {
        this.type = type;
    }

    public String getPivotTransactionRef() {
        return pivotTransactionRef;
    }

    public void setPivotTransactionRef(String pivotTransactionRef) {
        this.pivotTransactionRef = pivotTransactionRef;
    }

    public String getPivotAuthorizationRef() {
        return pivotAuthorizationRef;
    }

    public void setPivotAuthorizationRef(String pivotAuthorizationRef) {
        this.pivotAuthorizationRef = pivotAuthorizationRef;
    }

    public Instant getPivotOperationAt() {
        return pivotOperationAt;
    }

    public void setPivotOperationAt(Instant pivotOperationAt) {
        this.pivotOperationAt = pivotOperationAt;
    }

    public BigDecimal getPivotAmount() {
        return pivotAmount;
    }

    public void setPivotAmount(BigDecimal pivotAmount) {
        this.pivotAmount = pivotAmount;
    }

    public String getPivotParticipantCode() {
        return pivotParticipantCode;
    }

    public void setPivotParticipantCode(String pivotParticipantCode) {
        this.pivotParticipantCode = pivotParticipantCode;
    }

    public BigDecimal getAmountSwitch() {
        return amountSwitch;
    }

    public void setAmountSwitch(BigDecimal amountSwitch) {
        this.amountSwitch = amountSwitch;
    }

    public BigDecimal getAmountParticipant() {
        return amountParticipant;
    }

    public void setAmountParticipant(BigDecimal amountParticipant) {
        this.amountParticipant = amountParticipant;
    }

    public SuspensState getState() {
        return state;
    }

    public void setState(SuspensState state) {
        this.state = state;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public UUID getDisputeTicketId() {
        return disputeTicketId;
    }

    public void setDisputeTicketId(UUID disputeTicketId) {
        this.disputeTicketId = disputeTicketId;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public ReconciliationDTO getReconciliation() {
        return reconciliation;
    }

    public void setReconciliation(ReconciliationDTO reconciliation) {
        this.reconciliation = reconciliation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SuspensDTO)) {
            return false;
        }

        SuspensDTO suspensDTO = (SuspensDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, suspensDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SuspensDTO{" +
            "id='" + getId() + "'" +
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
            ", reconciliation=" + getReconciliation() +
            "}";
    }
}
