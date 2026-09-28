package cm.gimac.des.pilotage.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.CollaboratorProjection} entity.
 */
@Schema(
    description = "Projection locale de l'annuaire (iam.user-changed.v1), lue sans appel\nsynchrone à MS-IAM pour vérifier qu'un responsable est actif."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CollaboratorProjectionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Identifiant du collaborateur dans MS-IAM.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID collaboratorId;

    @NotNull
    @Size(max = 255)
    private String displayName;

    @NotNull
    private Boolean active;

    @Size(max = 30)
    @Schema(description = "Service ou pôle d'affectation, pour choisir le tableau d'une décision.")
    private String serviceCode;

    @NotNull
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCollaboratorId() {
        return collaboratorId;
    }

    public void setCollaboratorId(UUID collaboratorId) {
        this.collaboratorId = collaboratorId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
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
        if (!(o instanceof CollaboratorProjectionDTO)) {
            return false;
        }

        CollaboratorProjectionDTO collaboratorProjectionDTO = (CollaboratorProjectionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, collaboratorProjectionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CollaboratorProjectionDTO{" +
            "id='" + getId() + "'" +
            ", collaboratorId='" + getCollaboratorId() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", active='" + getActive() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
