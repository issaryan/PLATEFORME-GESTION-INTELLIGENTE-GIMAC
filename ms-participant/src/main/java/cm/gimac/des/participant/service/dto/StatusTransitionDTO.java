package cm.gimac.des.participant.service.dto;

import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.StatusTransition} entity.
 */
@Schema(description = "Transition de statut proposée puis approuvée ou rejetée (maker-checker,\nalgorithme 6.25). Historique immuable.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StatusTransitionDTO implements Serializable {

    private UUID id;

    @NotNull
    private InstitutionStatus fromStatus;

    @NotNull
    private InstitutionStatus toStatus;

    @NotNull
    @Size(max = 1000)
    private String reason;

    @NotNull
    private UUID requesterId;

    @NotNull
    private Instant requestedAt;

    @NotNull
    @Schema(
        description = "Vrai pour la bascule automatique en A_REGULARISER à J-0 (D-13), sans approbateur.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean automaticTransition;

    @NotNull
    @Min(value = 1)
    @Max(value = 2)
    @Schema(
        description = "1 en règle générale ; 2 pour la levée de suspension (DG et Risques, RG-14).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer requiredApprovals;

    private UUID approverId;

    private UUID secondApproverId;

    private Instant secondApprovedAt;

    private Instant decidedAt;

    @NotNull
    private TransitionState state;

    @Size(max = 500)
    private String rejectionReason;

    @Schema(description = "Pièce de décision hors plateforme dans MS-DOCUMENT (exigée pour SUSPENDU).")
    private UUID evidenceDocumentId;

    @NotNull
    private InstitutionDTO institution;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InstitutionStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(InstitutionStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public InstitutionStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(InstitutionStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Boolean getAutomaticTransition() {
        return automaticTransition;
    }

    public void setAutomaticTransition(Boolean automaticTransition) {
        this.automaticTransition = automaticTransition;
    }

    public Integer getRequiredApprovals() {
        return requiredApprovals;
    }

    public void setRequiredApprovals(Integer requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public UUID getSecondApproverId() {
        return secondApproverId;
    }

    public void setSecondApproverId(UUID secondApproverId) {
        this.secondApproverId = secondApproverId;
    }

    public Instant getSecondApprovedAt() {
        return secondApprovedAt;
    }

    public void setSecondApprovedAt(Instant secondApprovedAt) {
        this.secondApprovedAt = secondApprovedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public TransitionState getState() {
        return state;
    }

    public void setState(TransitionState state) {
        this.state = state;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public UUID getEvidenceDocumentId() {
        return evidenceDocumentId;
    }

    public void setEvidenceDocumentId(UUID evidenceDocumentId) {
        this.evidenceDocumentId = evidenceDocumentId;
    }

    public InstitutionDTO getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionDTO institution) {
        this.institution = institution;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatusTransitionDTO)) {
            return false;
        }

        StatusTransitionDTO statusTransitionDTO = (StatusTransitionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, statusTransitionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StatusTransitionDTO{" +
            "id='" + getId() + "'" +
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
            ", institution=" + getInstitution() +
            "}";
    }
}
