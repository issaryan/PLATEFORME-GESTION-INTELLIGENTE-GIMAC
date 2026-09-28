package cm.gimac.des.reporting.domain;

import cm.gimac.des.reporting.domain.enumeration.ThresholdState;
import cm.gimac.des.reporting.domain.enumeration.Trend;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Valeur calculée d'un indicateur pour une période et une clé de dimension,
 * historisée (IndicatorRepositoryPort.series). Les cockpits lisent la
 * dernière valeur matérialisée.
 */
@Entity
@Table(name = "indicator_value")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IndicatorValue implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Période, par exemple 2026-08 ou 2026-W35.
     */
    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[0-9]{4}(-[0-9]{2}|-W[0-9]{2}|-[0-9]{2}-[0-9]{2})?$")
    @Column(name = "period", length = 10, nullable = false)
    private String period;

    @Column(name = "time_key")
    private LocalDate timeKey;

    /**
     * Institution (MS-PARTICIPANT).
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    @Column(name = "service_code", length = 30)
    private String serviceCode;

    @Size(max = 20)
    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "value", precision = 21, scale = 2)
    private BigDecimal value;

    /**
     * Dénominateur nul : valeur précédente conservée avec le marqueur « sans donnée ».
     */
    @NotNull
    @Column(name = "no_data", nullable = false)
    private Boolean noData;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_state", nullable = false)
    private ThresholdState thresholdState;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_threshold_state")
    private ThresholdState previousThresholdState;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "trend", nullable = false)
    private Trend trend;

    @NotNull
    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "cockpits" }, allowSetters = true)
    private IndicatorDefinition definition;

    /**
     * État déclaré dont provient une valeur ETAT_IMPORTE (empreinte et date citées au rapport).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "supersededBy", "importedStatement" }, allowSetters = true)
    private ImportedStatement statement;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public IndicatorValue id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPeriod() {
        return this.period;
    }

    public IndicatorValue period(String period) {
        this.setPeriod(period);
        return this;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public LocalDate getTimeKey() {
        return this.timeKey;
    }

    public IndicatorValue timeKey(LocalDate timeKey) {
        this.setTimeKey(timeKey);
        return this;
    }

    public void setTimeKey(LocalDate timeKey) {
        this.timeKey = timeKey;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public IndicatorValue institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public IndicatorValue serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getSeverity() {
        return this.severity;
    }

    public IndicatorValue severity(String severity) {
        this.setSeverity(severity);
        return this;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public BigDecimal getValue() {
        return this.value;
    }

    public IndicatorValue value(BigDecimal value) {
        this.setValue(value);
        return this;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Boolean getNoData() {
        return this.noData;
    }

    public IndicatorValue noData(Boolean noData) {
        this.setNoData(noData);
        return this;
    }

    public void setNoData(Boolean noData) {
        this.noData = noData;
    }

    public ThresholdState getThresholdState() {
        return this.thresholdState;
    }

    public IndicatorValue thresholdState(ThresholdState thresholdState) {
        this.setThresholdState(thresholdState);
        return this;
    }

    public void setThresholdState(ThresholdState thresholdState) {
        this.thresholdState = thresholdState;
    }

    public ThresholdState getPreviousThresholdState() {
        return this.previousThresholdState;
    }

    public IndicatorValue previousThresholdState(ThresholdState previousThresholdState) {
        this.setPreviousThresholdState(previousThresholdState);
        return this;
    }

    public void setPreviousThresholdState(ThresholdState previousThresholdState) {
        this.previousThresholdState = previousThresholdState;
    }

    public Trend getTrend() {
        return this.trend;
    }

    public IndicatorValue trend(Trend trend) {
        this.setTrend(trend);
        return this;
    }

    public void setTrend(Trend trend) {
        this.trend = trend;
    }

    public Instant getComputedAt() {
        return this.computedAt;
    }

    public IndicatorValue computedAt(Instant computedAt) {
        this.setComputedAt(computedAt);
        return this;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public IndicatorDefinition getDefinition() {
        return this.definition;
    }

    public void setDefinition(IndicatorDefinition indicatorDefinition) {
        this.definition = indicatorDefinition;
    }

    public IndicatorValue definition(IndicatorDefinition indicatorDefinition) {
        this.setDefinition(indicatorDefinition);
        return this;
    }

    public ImportedStatement getStatement() {
        return this.statement;
    }

    public void setStatement(ImportedStatement importedStatement) {
        this.statement = importedStatement;
    }

    public IndicatorValue statement(ImportedStatement importedStatement) {
        this.setStatement(importedStatement);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndicatorValue)) {
            return false;
        }
        return getId() != null && getId().equals(((IndicatorValue) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IndicatorValue{" +
            "id=" + getId() +
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
            "}";
    }
}
