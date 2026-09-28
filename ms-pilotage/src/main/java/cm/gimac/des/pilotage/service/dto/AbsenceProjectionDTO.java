package cm.gimac.des.pilotage.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.AbsenceProjection} entity.
 */
@Schema(
    description = "Absence validée (iam.availability-validated.v1), pour vérifier qu'un\nresponsable est disponible à l'échéance (algorithme 6.55, étape 2)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AbsenceProjectionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Identifiant de la disponibilité dans MS-IAM.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID availabilityId;

    @NotNull
    @Schema(description = "Identifiant du collaborateur dans MS-IAM.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID collaboratorId;

    @NotNull
    private Instant startsAt;

    @NotNull
    private Instant endsAt;

    @NotNull
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAvailabilityId() {
        return availabilityId;
    }

    public void setAvailabilityId(UUID availabilityId) {
        this.availabilityId = availabilityId;
    }

    public UUID getCollaboratorId() {
        return collaboratorId;
    }

    public void setCollaboratorId(UUID collaboratorId) {
        this.collaboratorId = collaboratorId;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbsenceProjectionDTO)) {
            return false;
        }

        AbsenceProjectionDTO absenceProjectionDTO = (AbsenceProjectionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, absenceProjectionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AbsenceProjectionDTO{" +
            "id='" + getId() + "'" +
            ", availabilityId='" + getAvailabilityId() + "'" +
            ", collaboratorId='" + getCollaboratorId() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
