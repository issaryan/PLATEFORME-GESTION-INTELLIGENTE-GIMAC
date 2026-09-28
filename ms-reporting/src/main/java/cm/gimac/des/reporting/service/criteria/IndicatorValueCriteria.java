package cm.gimac.des.reporting.service.criteria;

import cm.gimac.des.reporting.domain.enumeration.ThresholdState;
import cm.gimac.des.reporting.domain.enumeration.ThresholdState;
import cm.gimac.des.reporting.domain.enumeration.Trend;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.reporting.domain.IndicatorValue} entity. This class is used
 * in {@link cm.gimac.des.reporting.web.rest.IndicatorValueResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /indicator-values?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IndicatorValueCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ThresholdState
     */
    public static class ThresholdStateFilter extends Filter<ThresholdState> {

        public ThresholdStateFilter() {}

        public ThresholdStateFilter(ThresholdStateFilter filter) {
            super(filter);
        }

        @Override
        public ThresholdStateFilter copy() {
            return new ThresholdStateFilter(this);
        }
    }

    /**
     * Class for filtering Trend
     */
    public static class TrendFilter extends Filter<Trend> {

        public TrendFilter() {}

        public TrendFilter(TrendFilter filter) {
            super(filter);
        }

        @Override
        public TrendFilter copy() {
            return new TrendFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter period;

    private LocalDateFilter timeKey;

    private UUIDFilter institutionId;

    private StringFilter serviceCode;

    private StringFilter severity;

    private BigDecimalFilter value;

    private BooleanFilter noData;

    private ThresholdStateFilter thresholdState;

    private ThresholdStateFilter previousThresholdState;

    private TrendFilter trend;

    private InstantFilter computedAt;

    private UUIDFilter definitionId;

    private UUIDFilter statementId;

    private Boolean distinct;

    public IndicatorValueCriteria() {}

    public IndicatorValueCriteria(IndicatorValueCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.period = other.optionalPeriod().map(StringFilter::copy).orElse(null);
        this.timeKey = other.optionalTimeKey().map(LocalDateFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.severity = other.optionalSeverity().map(StringFilter::copy).orElse(null);
        this.value = other.optionalValue().map(BigDecimalFilter::copy).orElse(null);
        this.noData = other.optionalNoData().map(BooleanFilter::copy).orElse(null);
        this.thresholdState = other.optionalThresholdState().map(ThresholdStateFilter::copy).orElse(null);
        this.previousThresholdState = other.optionalPreviousThresholdState().map(ThresholdStateFilter::copy).orElse(null);
        this.trend = other.optionalTrend().map(TrendFilter::copy).orElse(null);
        this.computedAt = other.optionalComputedAt().map(InstantFilter::copy).orElse(null);
        this.definitionId = other.optionalDefinitionId().map(UUIDFilter::copy).orElse(null);
        this.statementId = other.optionalStatementId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public IndicatorValueCriteria copy() {
        return new IndicatorValueCriteria(this);
    }

    public UUIDFilter getId() {
        return id;
    }

    public Optional<UUIDFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public UUIDFilter id() {
        if (id == null) {
            setId(new UUIDFilter());
        }
        return id;
    }

    public void setId(UUIDFilter id) {
        this.id = id;
    }

    public StringFilter getPeriod() {
        return period;
    }

    public Optional<StringFilter> optionalPeriod() {
        return Optional.ofNullable(period);
    }

    public StringFilter period() {
        if (period == null) {
            setPeriod(new StringFilter());
        }
        return period;
    }

    public void setPeriod(StringFilter period) {
        this.period = period;
    }

    public LocalDateFilter getTimeKey() {
        return timeKey;
    }

    public Optional<LocalDateFilter> optionalTimeKey() {
        return Optional.ofNullable(timeKey);
    }

    public LocalDateFilter timeKey() {
        if (timeKey == null) {
            setTimeKey(new LocalDateFilter());
        }
        return timeKey;
    }

    public void setTimeKey(LocalDateFilter timeKey) {
        this.timeKey = timeKey;
    }

    public UUIDFilter getInstitutionId() {
        return institutionId;
    }

    public Optional<UUIDFilter> optionalInstitutionId() {
        return Optional.ofNullable(institutionId);
    }

    public UUIDFilter institutionId() {
        if (institutionId == null) {
            setInstitutionId(new UUIDFilter());
        }
        return institutionId;
    }

    public void setInstitutionId(UUIDFilter institutionId) {
        this.institutionId = institutionId;
    }

    public StringFilter getServiceCode() {
        return serviceCode;
    }

    public Optional<StringFilter> optionalServiceCode() {
        return Optional.ofNullable(serviceCode);
    }

    public StringFilter serviceCode() {
        if (serviceCode == null) {
            setServiceCode(new StringFilter());
        }
        return serviceCode;
    }

    public void setServiceCode(StringFilter serviceCode) {
        this.serviceCode = serviceCode;
    }

    public StringFilter getSeverity() {
        return severity;
    }

    public Optional<StringFilter> optionalSeverity() {
        return Optional.ofNullable(severity);
    }

    public StringFilter severity() {
        if (severity == null) {
            setSeverity(new StringFilter());
        }
        return severity;
    }

    public void setSeverity(StringFilter severity) {
        this.severity = severity;
    }

    public BigDecimalFilter getValue() {
        return value;
    }

    public Optional<BigDecimalFilter> optionalValue() {
        return Optional.ofNullable(value);
    }

    public BigDecimalFilter value() {
        if (value == null) {
            setValue(new BigDecimalFilter());
        }
        return value;
    }

    public void setValue(BigDecimalFilter value) {
        this.value = value;
    }

    public BooleanFilter getNoData() {
        return noData;
    }

    public Optional<BooleanFilter> optionalNoData() {
        return Optional.ofNullable(noData);
    }

    public BooleanFilter noData() {
        if (noData == null) {
            setNoData(new BooleanFilter());
        }
        return noData;
    }

    public void setNoData(BooleanFilter noData) {
        this.noData = noData;
    }

    public ThresholdStateFilter getThresholdState() {
        return thresholdState;
    }

    public Optional<ThresholdStateFilter> optionalThresholdState() {
        return Optional.ofNullable(thresholdState);
    }

    public ThresholdStateFilter thresholdState() {
        if (thresholdState == null) {
            setThresholdState(new ThresholdStateFilter());
        }
        return thresholdState;
    }

    public void setThresholdState(ThresholdStateFilter thresholdState) {
        this.thresholdState = thresholdState;
    }

    public ThresholdStateFilter getPreviousThresholdState() {
        return previousThresholdState;
    }

    public Optional<ThresholdStateFilter> optionalPreviousThresholdState() {
        return Optional.ofNullable(previousThresholdState);
    }

    public ThresholdStateFilter previousThresholdState() {
        if (previousThresholdState == null) {
            setPreviousThresholdState(new ThresholdStateFilter());
        }
        return previousThresholdState;
    }

    public void setPreviousThresholdState(ThresholdStateFilter previousThresholdState) {
        this.previousThresholdState = previousThresholdState;
    }

    public TrendFilter getTrend() {
        return trend;
    }

    public Optional<TrendFilter> optionalTrend() {
        return Optional.ofNullable(trend);
    }

    public TrendFilter trend() {
        if (trend == null) {
            setTrend(new TrendFilter());
        }
        return trend;
    }

    public void setTrend(TrendFilter trend) {
        this.trend = trend;
    }

    public InstantFilter getComputedAt() {
        return computedAt;
    }

    public Optional<InstantFilter> optionalComputedAt() {
        return Optional.ofNullable(computedAt);
    }

    public InstantFilter computedAt() {
        if (computedAt == null) {
            setComputedAt(new InstantFilter());
        }
        return computedAt;
    }

    public void setComputedAt(InstantFilter computedAt) {
        this.computedAt = computedAt;
    }

    public UUIDFilter getDefinitionId() {
        return definitionId;
    }

    public Optional<UUIDFilter> optionalDefinitionId() {
        return Optional.ofNullable(definitionId);
    }

    public UUIDFilter definitionId() {
        if (definitionId == null) {
            setDefinitionId(new UUIDFilter());
        }
        return definitionId;
    }

    public void setDefinitionId(UUIDFilter definitionId) {
        this.definitionId = definitionId;
    }

    public UUIDFilter getStatementId() {
        return statementId;
    }

    public Optional<UUIDFilter> optionalStatementId() {
        return Optional.ofNullable(statementId);
    }

    public UUIDFilter statementId() {
        if (statementId == null) {
            setStatementId(new UUIDFilter());
        }
        return statementId;
    }

    public void setStatementId(UUIDFilter statementId) {
        this.statementId = statementId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final IndicatorValueCriteria that = (IndicatorValueCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(period, that.period) &&
            Objects.equals(timeKey, that.timeKey) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(severity, that.severity) &&
            Objects.equals(value, that.value) &&
            Objects.equals(noData, that.noData) &&
            Objects.equals(thresholdState, that.thresholdState) &&
            Objects.equals(previousThresholdState, that.previousThresholdState) &&
            Objects.equals(trend, that.trend) &&
            Objects.equals(computedAt, that.computedAt) &&
            Objects.equals(definitionId, that.definitionId) &&
            Objects.equals(statementId, that.statementId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            period,
            timeKey,
            institutionId,
            serviceCode,
            severity,
            value,
            noData,
            thresholdState,
            previousThresholdState,
            trend,
            computedAt,
            definitionId,
            statementId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IndicatorValueCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalPeriod().map(f -> "period=" + f + ", ").orElse("") +
            optionalTimeKey().map(f -> "timeKey=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalSeverity().map(f -> "severity=" + f + ", ").orElse("") +
            optionalValue().map(f -> "value=" + f + ", ").orElse("") +
            optionalNoData().map(f -> "noData=" + f + ", ").orElse("") +
            optionalThresholdState().map(f -> "thresholdState=" + f + ", ").orElse("") +
            optionalPreviousThresholdState().map(f -> "previousThresholdState=" + f + ", ").orElse("") +
            optionalTrend().map(f -> "trend=" + f + ", ").orElse("") +
            optionalComputedAt().map(f -> "computedAt=" + f + ", ").orElse("") +
            optionalDefinitionId().map(f -> "definitionId=" + f + ", ").orElse("") +
            optionalStatementId().map(f -> "statementId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
