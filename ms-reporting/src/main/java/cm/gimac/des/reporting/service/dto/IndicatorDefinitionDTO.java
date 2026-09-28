package cm.gimac.des.reporting.service.dto;

import cm.gimac.des.reporting.domain.enumeration.IndicatorSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.IndicatorDefinition} entity.
 */
@Schema(
    description = "Définition d'un indicateur (code, source, unité, fenêtre, seuil à deux\nniveaux). Écart CC corrigé : l'entité Indicator du CC mêle définition et\nvaleur ; elle est scindée en IndicatorDefinition et IndicatorValue, ce qui\névite aussi « Indicator », mot réservé SQL signalé par JHipster."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IndicatorDefinitionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9-]+$")
    @Schema(description = "Code, par exemple EXP-MTTR-S2, RES-DISPO-DECL.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotNull
    @Size(max = 255)
    private String label;

    @NotNull
    private IndicatorSource source;

    @NotNull
    @Size(max = 20)
    private String unit;

    @NotNull
    @Schema(description = "Fenêtre W, par exemple 30 jours.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration window;

    @NotNull
    @Min(value = 1)
    @Max(value = 1000)
    @Schema(description = "Nombre K de segments du tampon circulaire (K = W / S).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer segmentCount;

    @NotNull
    @Schema(description = "Pas S d'un segment.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration segmentLength;

    @Schema(description = "Seuil d'alerte (objet-valeur Threshold du CC).")
    private BigDecimal warningThreshold;

    @Schema(description = "Seuil critique.")
    private BigDecimal criticalThreshold;

    @NotNull
    @Schema(description = "Sens de lecture : une valeur plus élevée est plus défavorable.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean higherIsWorse;

    @Size(max = 500)
    @Schema(description = "Audience : rôles ou services destinataires, séparés par des virgules.")
    private String audience;

    @NotNull
    private Boolean active;

    private Set<CockpitViewDTO> cockpits = new HashSet<>();

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

    public IndicatorSource getSource() {
        return source;
    }

    public void setSource(IndicatorSource source) {
        this.source = source;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public Integer getSegmentCount() {
        return segmentCount;
    }

    public void setSegmentCount(Integer segmentCount) {
        this.segmentCount = segmentCount;
    }

    public Duration getSegmentLength() {
        return segmentLength;
    }

    public void setSegmentLength(Duration segmentLength) {
        this.segmentLength = segmentLength;
    }

    public BigDecimal getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(BigDecimal warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public BigDecimal getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(BigDecimal criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public Boolean getHigherIsWorse() {
        return higherIsWorse;
    }

    public void setHigherIsWorse(Boolean higherIsWorse) {
        this.higherIsWorse = higherIsWorse;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Set<CockpitViewDTO> getCockpits() {
        return cockpits;
    }

    public void setCockpits(Set<CockpitViewDTO> cockpits) {
        this.cockpits = cockpits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndicatorDefinitionDTO)) {
            return false;
        }

        IndicatorDefinitionDTO indicatorDefinitionDTO = (IndicatorDefinitionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, indicatorDefinitionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IndicatorDefinitionDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", source='" + getSource() + "'" +
            ", unit='" + getUnit() + "'" +
            ", window='" + getWindow() + "'" +
            ", segmentCount=" + getSegmentCount() +
            ", segmentLength='" + getSegmentLength() + "'" +
            ", warningThreshold=" + getWarningThreshold() +
            ", criticalThreshold=" + getCriticalThreshold() +
            ", higherIsWorse='" + getHigherIsWorse() + "'" +
            ", audience='" + getAudience() + "'" +
            ", active='" + getActive() + "'" +
            ", cockpits=" + getCockpits() +
            "}";
    }
}
