package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.LinkScope;
import cm.gimac.des.monitoring.domain.enumeration.LinkState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.SupervisedLink} entity.
 */
@Schema(description = "Liaison d'institution ou plateforme du groupement supervisée (CC §6.1.10.3).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SupervisedLinkDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 50)
    @Schema(description = "Identifiant lisible du CC, par exemple LIA-2026-017.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotNull
    @Size(max = 255)
    private String label;

    @NotNull
    private LinkScope scope;

    @Schema(description = "Institution (MS-PARTICIPANT), pour une liaison de portée INSTITUTION.")
    private UUID institutionId;

    @NotNull
    @Size(max = 100)
    @Schema(
        description = "Type, par exemple « liaison de paiement mobile » ou « commutateur ».",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String type;

    @NotNull
    private LinkState state;

    @NotNull
    private Instant stateSince;

    private Instant lastMeasureAt;

    @NotNull
    @Schema(
        description = "Période de mesure attendue ; au-delà de 3 périodes sans mesure : INJOIGNABLE.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Duration measurePeriod;

    @Schema(description = "Fenêtre de maintenance déclarée par le chef de quart (algorithme 6.47, étape 1).")
    private Instant maintenanceFrom;

    private Instant maintenanceUntil;

    @Schema(description = "Plateforme dont dépend la liaison, utilisée par la corrélation.")
    private SupervisedLinkDTO parentPlatform;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public LinkScope getScope() {
        return scope;
    }

    public void setScope(LinkScope scope) {
        this.scope = scope;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LinkState getState() {
        return state;
    }

    public void setState(LinkState state) {
        this.state = state;
    }

    public Instant getStateSince() {
        return stateSince;
    }

    public void setStateSince(Instant stateSince) {
        this.stateSince = stateSince;
    }

    public Instant getLastMeasureAt() {
        return lastMeasureAt;
    }

    public void setLastMeasureAt(Instant lastMeasureAt) {
        this.lastMeasureAt = lastMeasureAt;
    }

    public Duration getMeasurePeriod() {
        return measurePeriod;
    }

    public void setMeasurePeriod(Duration measurePeriod) {
        this.measurePeriod = measurePeriod;
    }

    public Instant getMaintenanceFrom() {
        return maintenanceFrom;
    }

    public void setMaintenanceFrom(Instant maintenanceFrom) {
        this.maintenanceFrom = maintenanceFrom;
    }

    public Instant getMaintenanceUntil() {
        return maintenanceUntil;
    }

    public void setMaintenanceUntil(Instant maintenanceUntil) {
        this.maintenanceUntil = maintenanceUntil;
    }

    public SupervisedLinkDTO getParentPlatform() {
        return parentPlatform;
    }

    public void setParentPlatform(SupervisedLinkDTO parentPlatform) {
        this.parentPlatform = parentPlatform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SupervisedLinkDTO)) {
            return false;
        }

        SupervisedLinkDTO supervisedLinkDTO = (SupervisedLinkDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, supervisedLinkDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SupervisedLinkDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", scope='" + getScope() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", type='" + getType() + "'" +
            ", state='" + getState() + "'" +
            ", stateSince='" + getStateSince() + "'" +
            ", lastMeasureAt='" + getLastMeasureAt() + "'" +
            ", measurePeriod='" + getMeasurePeriod() + "'" +
            ", maintenanceFrom='" + getMaintenanceFrom() + "'" +
            ", maintenanceUntil='" + getMaintenanceUntil() + "'" +
            ", parentPlatform=" + getParentPlatform() +
            "}";
    }
}
