package cm.gimac.des.reporting.service.dto;

import cm.gimac.des.reporting.domain.enumeration.ThresholdState;
import cm.gimac.des.reporting.domain.enumeration.Trend;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.IndicatorValue} entity.
 */
@Schema(
    description = "Valeur calculée d'un indicateur pour une période et une clé de dimension,\nhistorisée (IndicatorRepositoryPort.series). Les cockpits lisent la\ndernière valeur matérialisée."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IndicatorValueDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[0-9]{4}(-[0-9]{2}|-W[0-9]{2}|-[0-9]{2}-[0-9]{2})?$")
    @Schema(description = "Période, par exemple 2026-08 ou 2026-W35.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String period;

    private LocalDate timeKey;

    @Schema(description = "Institution (MS-PARTICIPANT).")
    private UUID institutionId;

    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    private String serviceCode;

    @Size(max = 20)
    private String severity;

    private BigDecimal value;

    @NotNull
    @Schema(
        description = "Dénominateur nul : valeur précédente conservée avec le marqueur « sans donnée ».",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean noData;

    @NotNull
    private ThresholdState thresholdState;

    private ThresholdState previousThresholdState;

    @NotNull
    private Trend trend;

    @NotNull
    private Instant computedAt;

    @NotNull
    private IndicatorDefinitionDTO definition;

    @Schema(description = "État déclaré dont provient une valeur ETAT_IMPORTE (empreinte et date citées au rapport).")
    private ImportedStatementDTO statement;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public LocalDate getTimeKey() {
        return timeKey;
    }

    public void setTimeKey(LocalDate timeKey) {
        this.timeKey = timeKey;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Boolean getNoData() {
        return noData;
    }

    public void setNoData(Boolean noData) {
        this.noData = noData;
    }

    public ThresholdState getThresholdState() {
        return thresholdState;
    }

    public void setThresholdState(ThresholdState thresholdState) {
        this.thresholdState = thresholdState;
    }

    public ThresholdState getPreviousThresholdState() {
        return previousThresholdState;
    }

    public void setPreviousThresholdState(ThresholdState previousThresholdState) {
        this.previousThresholdState = previousThresholdState;
    }

    public Trend getTrend() {
        return trend;
    }

    public void setTrend(Trend trend) {
        this.trend = trend;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public IndicatorDefinitionDTO getDefinition() {
        return definition;
    }

    public void setDefinition(IndicatorDefinitionDTO definition) {
        this.definition = definition;
    }

    public ImportedStatementDTO getStatement() {
        return statement;
    }

    public void setStatement(ImportedStatementDTO statement) {
        this.statement = statement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndicatorValueDTO)) {
            return false;
        }

        IndicatorValueDTO indicatorValueDTO = (IndicatorValueDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, indicatorValueDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IndicatorValueDTO{" +
            "id='" + getId() + "'" +
            ", period='" + getPeriod() + "'" +
            ", timeKey='" + getTimeKey() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", severity='" + getSeverity() + "'" +
            ", value=" + getValue() +
            ", noData='" + getNoData() + "'" +
            ", thresholdState='" + getThresholdState() + "'" +
            ", previousThresholdState='" + getPreviousThresholdState() + "'" +
            ", trend='" + getTrend() + "'" +
            ", computedAt='" + getComputedAt() + "'" +
            ", definition=" + getDefinition() +
            ", statement=" + getStatement() +
            "}";
    }
}
