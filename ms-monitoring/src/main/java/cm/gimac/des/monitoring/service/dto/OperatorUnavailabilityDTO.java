package cm.gimac.des.monitoring.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.OperatorUnavailability} entity.
 */
@Schema(
    description = "Indisponibilité validée d'un opérateur, projetée depuis\niam.availability-validated.v1 (RG-74) ; ensemble U de l'algorithme 6.45."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OperatorUnavailabilityDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Identifiant de la disponibilité dans MS-IAM (clé d'idempotence).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID availabilityId;

    @NotNull
    private UUID operatorId;

    @NotNull
    private Instant startsAt;

    @NotNull
    private Instant endsAt;

    @NotNull
    private Instant validatedAt;

    @NotNull
    @Schema(description = "Annulation reçue de MS-IAM : l'indisponibilité ne compte plus.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean cancelled;

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

    public UUID getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(UUID operatorId) {
        this.operatorId = operatorId;
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

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperatorUnavailabilityDTO)) {
            return false;
        }

        OperatorUnavailabilityDTO operatorUnavailabilityDTO = (OperatorUnavailabilityDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, operatorUnavailabilityDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OperatorUnavailabilityDTO{" +
            "id='" + getId() + "'" +
            ", availabilityId='" + getAvailabilityId() + "'" +
            ", operatorId='" + getOperatorId() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            ", cancelled='" + getCancelled() + "'" +
            "}";
    }
}
