package cm.gimac.des.iam.service.dto;

import cm.gimac.des.iam.domain.enumeration.AvailabilityType;
import cm.gimac.des.iam.domain.enumeration.ValidationState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.iam.domain.Availability} entity.
 */
@Schema(
    description = "Congé ou absence déclaré puis validé (BF-ADMIN, RG-74, IAM-09).\nLe justificatif est stocké dans MS-DOCUMENT, à accès restreint (D-29)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AvailabilityDTO implements Serializable {

    private UUID id;

    @NotNull
    private AvailabilityType type;

    @NotNull
    private Instant startsAt;

    @NotNull
    private Instant endsAt;

    @Size(max = 500)
    private String reason;

    private UUID justificationDocumentId;

    @NotNull
    private ValidationState validationState;

    private UUID validatorId;

    private Instant validatedAt;

    @NotNull
    private CollaboratorDTO collaborator;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AvailabilityType getType() {
        return type;
    }

    public void setType(AvailabilityType type) {
        this.type = type;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getJustificationDocumentId() {
        return justificationDocumentId;
    }

    public void setJustificationDocumentId(UUID justificationDocumentId) {
        this.justificationDocumentId = justificationDocumentId;
    }

    public ValidationState getValidationState() {
        return validationState;
    }

    public void setValidationState(ValidationState validationState) {
        this.validationState = validationState;
    }

    public UUID getValidatorId() {
        return validatorId;
    }

    public void setValidatorId(UUID validatorId) {
        this.validatorId = validatorId;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
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
        if (!(o instanceof AvailabilityDTO)) {
            return false;
        }

        AvailabilityDTO availabilityDTO = (AvailabilityDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, availabilityDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AvailabilityDTO{" +
            "id='" + getId() + "'" +
            ", type='" + getType() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            ", reason='" + getReason() + "'" +
            ", justificationDocumentId='" + getJustificationDocumentId() + "'" +
            ", validationState='" + getValidationState() + "'" +
            ", validatorId='" + getValidatorId() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            ", collaborator=" + getCollaborator() +
            "}";
    }
}
