package cm.gimac.des.reporting.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.CockpitView} entity.
 */
@Schema(description = "Cockpit ou vue par service (cockpit DG, cockpit DES, vues par service).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CockpitViewDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9-]+$")
    @Schema(description = "Code, par exemple COCKPIT-DG, COCKPIT-DES.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotNull
    @Size(max = 255)
    private String label;

    @NotNull
    @Size(max = 255)
    private String audience;

    @NotNull
    @Schema(description = "Intervalle de rafraîchissement, 60 s par défaut.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration refreshInterval;

    private Set<IndicatorDefinitionDTO> indicators = new HashSet<>();

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

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public Set<IndicatorDefinitionDTO> getIndicators() {
        return indicators;
    }

    public void setIndicators(Set<IndicatorDefinitionDTO> indicators) {
        this.indicators = indicators;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CockpitViewDTO)) {
            return false;
        }

        CockpitViewDTO cockpitViewDTO = (CockpitViewDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, cockpitViewDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CockpitViewDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", audience='" + getAudience() + "'" +
            ", refreshInterval='" + getRefreshInterval() + "'" +
            ", indicators=" + getIndicators() +
            "}";
    }
}
