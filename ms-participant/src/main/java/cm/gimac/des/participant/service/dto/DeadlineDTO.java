package cm.gimac.des.participant.service.dto;

import cm.gimac.des.participant.domain.enumeration.DeadlineState;
import cm.gimac.des.participant.domain.enumeration.DeadlineThreshold;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.Deadline} entity.
 */
@Schema(
    description = "Échéance armée sur une pièce légale ou une convention (J-90, J-60, J-30,\nJ-0) ; à J-60 et J-30, notification signée avec accusé de réception (RG-69)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeadlineDTO implements Serializable {

    private UUID id;

    @NotNull
    private DeadlineThreshold threshold;

    @NotNull
    private LocalDate dueOn;

    @NotNull
    private DeadlineState state;

    @Size(max = 255)
    private String triggeredAction;

    private Instant triggeredAt;

    @NotNull
    private Boolean acknowledgementRequired;

    @Schema(description = "Identifiant de la notification dans MS-NOTIFICATION.")
    private UUID notificationId;

    private Instant acknowledgedAt;

    @NotNull
    private InstitutionDTO institution;

    private LegalDocumentDTO legalDocument;

    private ConventionDTO convention;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DeadlineThreshold getThreshold() {
        return threshold;
    }

    public void setThreshold(DeadlineThreshold threshold) {
        this.threshold = threshold;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public void setDueOn(LocalDate dueOn) {
        this.dueOn = dueOn;
    }

    public DeadlineState getState() {
        return state;
    }

    public void setState(DeadlineState state) {
        this.state = state;
    }

    public String getTriggeredAction() {
        return triggeredAction;
    }

    public void setTriggeredAction(String triggeredAction) {
        this.triggeredAction = triggeredAction;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public Boolean getAcknowledgementRequired() {
        return acknowledgementRequired;
    }

    public void setAcknowledgementRequired(Boolean acknowledgementRequired) {
        this.acknowledgementRequired = acknowledgementRequired;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public InstitutionDTO getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionDTO institution) {
        this.institution = institution;
    }

    public LegalDocumentDTO getLegalDocument() {
        return legalDocument;
    }

    public void setLegalDocument(LegalDocumentDTO legalDocument) {
        this.legalDocument = legalDocument;
    }

    public ConventionDTO getConvention() {
        return convention;
    }

    public void setConvention(ConventionDTO convention) {
        this.convention = convention;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeadlineDTO)) {
            return false;
        }

        DeadlineDTO deadlineDTO = (DeadlineDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, deadlineDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DeadlineDTO{" +
            "id='" + getId() + "'" +
            ", threshold='" + getThreshold() + "'" +
            ", dueOn='" + getDueOn() + "'" +
            ", state='" + getState() + "'" +
            ", triggeredAction='" + getTriggeredAction() + "'" +
            ", triggeredAt='" + getTriggeredAt() + "'" +
            ", acknowledgementRequired='" + getAcknowledgementRequired() + "'" +
            ", notificationId='" + getNotificationId() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            ", institution=" + getInstitution() +
            ", legalDocument=" + getLegalDocument() +
            ", convention=" + getConvention() +
            "}";
    }
}
