package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.MeasureDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.Threshold} entity.
 */
@Schema(description = "Seuil par indicateur, avec hystérésis et confirmation (objet-valeur du CC, persisté).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ThresholdDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 100)
    @Schema(description = "Indicateur, par exemple « latence p95 (ms) ».", requiredMode = Schema.RequiredMode.REQUIRED)
    private String indicator;

    @NotNull
    private BigDecimal warning;

    @NotNull
    private BigDecimal critical;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal hysteresis;

    @NotNull
    private MeasureDirection direction;

    @NotNull
    @Min(value = 1)
    @Schema(description = "Nombre k de fenêtres de confirmation.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer debounce;

    @NotNull
    private SupervisedLinkDTO link;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIndicator() {
        return indicator;
    }

    public void setIndicator(String indicator) {
        this.indicator = indicator;
    }

    public BigDecimal getWarning() {
        return warning;
    }

    public void setWarning(BigDecimal warning) {
        this.warning = warning;
    }

    public BigDecimal getCritical() {
        return critical;
    }

    public void setCritical(BigDecimal critical) {
        this.critical = critical;
    }

    public BigDecimal getHysteresis() {
        return hysteresis;
    }

    public void setHysteresis(BigDecimal hysteresis) {
        this.hysteresis = hysteresis;
    }

    public MeasureDirection getDirection() {
        return direction;
    }

    public void setDirection(MeasureDirection direction) {
        this.direction = direction;
    }

    public Integer getDebounce() {
        return debounce;
    }

    public void setDebounce(Integer debounce) {
        this.debounce = debounce;
    }

    public SupervisedLinkDTO getLink() {
        return link;
    }

    public void setLink(SupervisedLinkDTO link) {
        this.link = link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ThresholdDTO)) {
            return false;
        }

        ThresholdDTO thresholdDTO = (ThresholdDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, thresholdDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ThresholdDTO{" +
            "id='" + getId() + "'" +
            ", indicator='" + getIndicator() + "'" +
            ", warning=" + getWarning() +
            ", critical=" + getCritical() +
            ", hysteresis=" + getHysteresis() +
            ", direction='" + getDirection() + "'" +
            ", debounce=" + getDebounce() +
            ", link=" + getLink() +
            "}";
    }
}
