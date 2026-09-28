package cm.gimac.des.incident.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.AlertObservation} entity.
 */
@Schema(
    description = "Alerte reçue sur un objet (monitoring.alert-raised.v1), projetée pour\nvérifier la période d'observation du test de résolution (algorithme 6.43)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AlertObservationDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID alertId;

    @Size(max = 50)
    private String linkId;

    private UUID institutionId;

    @NotNull
    @Size(max = 20)
    @Schema(description = "Gravité proposée par MS-MONITORING (MINEURE, MAJEURE, CRITIQUE).", requiredMode = Schema.RequiredMode.REQUIRED)
    private String alertSeverity;

    @NotNull
    private Instant raisedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public void setAlertId(UUID alertId) {
        this.alertId = alertId;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getAlertSeverity() {
        return alertSeverity;
    }

    public void setAlertSeverity(String alertSeverity) {
        this.alertSeverity = alertSeverity;
    }

    public Instant getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(Instant raisedAt) {
        this.raisedAt = raisedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AlertObservationDTO)) {
            return false;
        }

        AlertObservationDTO alertObservationDTO = (AlertObservationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, alertObservationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AlertObservationDTO{" +
            "id='" + getId() + "'" +
            ", alertId='" + getAlertId() + "'" +
            ", linkId='" + getLinkId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", alertSeverity='" + getAlertSeverity() + "'" +
            ", raisedAt='" + getRaisedAt() + "'" +
            "}";
    }
}
