package cm.gimac.des.reporting.service.criteria;

import cm.gimac.des.reporting.domain.enumeration.IndicatorSource;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.reporting.domain.IndicatorDefinition} entity. This class is used
 * in {@link cm.gimac.des.reporting.web.rest.IndicatorDefinitionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /indicator-definitions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IndicatorDefinitionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering IndicatorSource
     */
    public static class IndicatorSourceFilter extends Filter<IndicatorSource> {

        public IndicatorSourceFilter() {}

        public IndicatorSourceFilter(IndicatorSourceFilter filter) {
            super(filter);
        }

        @Override
        public IndicatorSourceFilter copy() {
            return new IndicatorSourceFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter code;

    private StringFilter label;

    private IndicatorSourceFilter source;

    private StringFilter unit;

    private DurationFilter window;

    private IntegerFilter segmentCount;

    private DurationFilter segmentLength;

    private BigDecimalFilter warningThreshold;

    private BigDecimalFilter criticalThreshold;

    private BooleanFilter higherIsWorse;

    private StringFilter audience;

    private BooleanFilter active;

    private UUIDFilter cockpitsId;

    private Boolean distinct;

    public IndicatorDefinitionCriteria() {}

    public IndicatorDefinitionCriteria(IndicatorDefinitionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.code = other.optionalCode().map(StringFilter::copy).orElse(null);
        this.label = other.optionalLabel().map(StringFilter::copy).orElse(null);
        this.source = other.optionalSource().map(IndicatorSourceFilter::copy).orElse(null);
        this.unit = other.optionalUnit().map(StringFilter::copy).orElse(null);
        this.window = other.optionalWindow().map(DurationFilter::copy).orElse(null);
        this.segmentCount = other.optionalSegmentCount().map(IntegerFilter::copy).orElse(null);
        this.segmentLength = other.optionalSegmentLength().map(DurationFilter::copy).orElse(null);
        this.warningThreshold = other.optionalWarningThreshold().map(BigDecimalFilter::copy).orElse(null);
        this.criticalThreshold = other.optionalCriticalThreshold().map(BigDecimalFilter::copy).orElse(null);
        this.higherIsWorse = other.optionalHigherIsWorse().map(BooleanFilter::copy).orElse(null);
        this.audience = other.optionalAudience().map(StringFilter::copy).orElse(null);
        this.active = other.optionalActive().map(BooleanFilter::copy).orElse(null);
        this.cockpitsId = other.optionalCockpitsId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public IndicatorDefinitionCriteria copy() {
        return new IndicatorDefinitionCriteria(this);
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

    public StringFilter getCode() {
        return code;
    }

    public Optional<StringFilter> optionalCode() {
        return Optional.ofNullable(code);
    }

    public StringFilter code() {
        if (code == null) {
            setCode(new StringFilter());
        }
        return code;
    }

    public void setCode(StringFilter code) {
        this.code = code;
    }

    public StringFilter getLabel() {
        return label;
    }

    public Optional<StringFilter> optionalLabel() {
        return Optional.ofNullable(label);
    }

    public StringFilter label() {
        if (label == null) {
            setLabel(new StringFilter());
        }
        return label;
    }

    public void setLabel(StringFilter label) {
        this.label = label;
    }

    public IndicatorSourceFilter getSource() {
        return source;
    }

    public Optional<IndicatorSourceFilter> optionalSource() {
        return Optional.ofNullable(source);
    }

    public IndicatorSourceFilter source() {
        if (source == null) {
            setSource(new IndicatorSourceFilter());
        }
        return source;
    }

    public void setSource(IndicatorSourceFilter source) {
        this.source = source;
    }

    public StringFilter getUnit() {
        return unit;
    }

    public Optional<StringFilter> optionalUnit() {
        return Optional.ofNullable(unit);
    }

    public StringFilter unit() {
        if (unit == null) {
            setUnit(new StringFilter());
        }
        return unit;
    }

    public void setUnit(StringFilter unit) {
        this.unit = unit;
    }

    public DurationFilter getWindow() {
        return window;
    }

    public Optional<DurationFilter> optionalWindow() {
        return Optional.ofNullable(window);
    }

    public DurationFilter window() {
        if (window == null) {
            setWindow(new DurationFilter());
        }
        return window;
    }

    public void setWindow(DurationFilter window) {
        this.window = window;
    }

    public IntegerFilter getSegmentCount() {
        return segmentCount;
    }

    public Optional<IntegerFilter> optionalSegmentCount() {
        return Optional.ofNullable(segmentCount);
    }

    public IntegerFilter segmentCount() {
        if (segmentCount == null) {
            setSegmentCount(new IntegerFilter());
        }
        return segmentCount;
    }

    public void setSegmentCount(IntegerFilter segmentCount) {
        this.segmentCount = segmentCount;
    }

    public DurationFilter getSegmentLength() {
        return segmentLength;
    }

    public Optional<DurationFilter> optionalSegmentLength() {
        return Optional.ofNullable(segmentLength);
    }

    public DurationFilter segmentLength() {
        if (segmentLength == null) {
            setSegmentLength(new DurationFilter());
        }
        return segmentLength;
    }

    public void setSegmentLength(DurationFilter segmentLength) {
        this.segmentLength = segmentLength;
    }

    public BigDecimalFilter getWarningThreshold() {
        return warningThreshold;
    }

    public Optional<BigDecimalFilter> optionalWarningThreshold() {
        return Optional.ofNullable(warningThreshold);
    }

    public BigDecimalFilter warningThreshold() {
        if (warningThreshold == null) {
            setWarningThreshold(new BigDecimalFilter());
        }
        return warningThreshold;
    }

    public void setWarningThreshold(BigDecimalFilter warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public BigDecimalFilter getCriticalThreshold() {
        return criticalThreshold;
    }

    public Optional<BigDecimalFilter> optionalCriticalThreshold() {
        return Optional.ofNullable(criticalThreshold);
    }

    public BigDecimalFilter criticalThreshold() {
        if (criticalThreshold == null) {
            setCriticalThreshold(new BigDecimalFilter());
        }
        return criticalThreshold;
    }

    public void setCriticalThreshold(BigDecimalFilter criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public BooleanFilter getHigherIsWorse() {
        return higherIsWorse;
    }

    public Optional<BooleanFilter> optionalHigherIsWorse() {
        return Optional.ofNullable(higherIsWorse);
    }

    public BooleanFilter higherIsWorse() {
        if (higherIsWorse == null) {
            setHigherIsWorse(new BooleanFilter());
        }
        return higherIsWorse;
    }

    public void setHigherIsWorse(BooleanFilter higherIsWorse) {
        this.higherIsWorse = higherIsWorse;
    }

    public StringFilter getAudience() {
        return audience;
    }

    public Optional<StringFilter> optionalAudience() {
        return Optional.ofNullable(audience);
    }

    public StringFilter audience() {
        if (audience == null) {
            setAudience(new StringFilter());
        }
        return audience;
    }

    public void setAudience(StringFilter audience) {
        this.audience = audience;
    }

    public BooleanFilter getActive() {
        return active;
    }

    public Optional<BooleanFilter> optionalActive() {
        return Optional.ofNullable(active);
    }

    public BooleanFilter active() {
        if (active == null) {
            setActive(new BooleanFilter());
        }
        return active;
    }

    public void setActive(BooleanFilter active) {
        this.active = active;
    }

    public UUIDFilter getCockpitsId() {
        return cockpitsId;
    }

    public Optional<UUIDFilter> optionalCockpitsId() {
        return Optional.ofNullable(cockpitsId);
    }

    public UUIDFilter cockpitsId() {
        if (cockpitsId == null) {
            setCockpitsId(new UUIDFilter());
        }
        return cockpitsId;
    }

    public void setCockpitsId(UUIDFilter cockpitsId) {
        this.cockpitsId = cockpitsId;
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
        final IndicatorDefinitionCriteria that = (IndicatorDefinitionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(code, that.code) &&
            Objects.equals(label, that.label) &&
            Objects.equals(source, that.source) &&
            Objects.equals(unit, that.unit) &&
            Objects.equals(window, that.window) &&
            Objects.equals(segmentCount, that.segmentCount) &&
            Objects.equals(segmentLength, that.segmentLength) &&
            Objects.equals(warningThreshold, that.warningThreshold) &&
            Objects.equals(criticalThreshold, that.criticalThreshold) &&
            Objects.equals(higherIsWorse, that.higherIsWorse) &&
            Objects.equals(audience, that.audience) &&
            Objects.equals(active, that.active) &&
            Objects.equals(cockpitsId, that.cockpitsId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            code,
            label,
            source,
            unit,
            window,
            segmentCount,
            segmentLength,
            warningThreshold,
            criticalThreshold,
            higherIsWorse,
            audience,
            active,
            cockpitsId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IndicatorDefinitionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalCode().map(f -> "code=" + f + ", ").orElse("") +
            optionalLabel().map(f -> "label=" + f + ", ").orElse("") +
            optionalSource().map(f -> "source=" + f + ", ").orElse("") +
            optionalUnit().map(f -> "unit=" + f + ", ").orElse("") +
            optionalWindow().map(f -> "window=" + f + ", ").orElse("") +
            optionalSegmentCount().map(f -> "segmentCount=" + f + ", ").orElse("") +
            optionalSegmentLength().map(f -> "segmentLength=" + f + ", ").orElse("") +
            optionalWarningThreshold().map(f -> "warningThreshold=" + f + ", ").orElse("") +
            optionalCriticalThreshold().map(f -> "criticalThreshold=" + f + ", ").orElse("") +
            optionalHigherIsWorse().map(f -> "higherIsWorse=" + f + ", ").orElse("") +
            optionalAudience().map(f -> "audience=" + f + ", ").orElse("") +
            optionalActive().map(f -> "active=" + f + ", ").orElse("") +
            optionalCockpitsId().map(f -> "cockpitsId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
