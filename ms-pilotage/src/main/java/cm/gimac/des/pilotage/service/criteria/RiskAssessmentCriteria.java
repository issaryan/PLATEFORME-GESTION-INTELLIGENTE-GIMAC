package cm.gimac.des.pilotage.service.criteria;

import cm.gimac.des.pilotage.domain.enumeration.RiskZone;
import cm.gimac.des.pilotage.domain.enumeration.RiskZone;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.RiskAssessment} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.RiskAssessmentResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /risk-assessments?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RiskAssessmentCriteria implements Serializable, Criteria {

    /**
     * Class for filtering RiskZone
     */
    public static class RiskZoneFilter extends Filter<RiskZone> {

        public RiskZoneFilter() {}

        public RiskZoneFilter(RiskZoneFilter filter) {
            super(filter);
        }

        @Override
        public RiskZoneFilter copy() {
            return new RiskZoneFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private IntegerFilter probability;

    private IntegerFilter impact;

    private BigDecimalFilter controlCoefficient;

    private IntegerFilter grossScore;

    private BigDecimalFilter residualScore;

    private RiskZoneFilter previousZone;

    private RiskZoneFilter zone;

    private UUIDFilter assessorId;

    private InstantFilter assessedAt;

    private UUIDFilter riskId;

    private Boolean distinct;

    public RiskAssessmentCriteria() {}

    public RiskAssessmentCriteria(RiskAssessmentCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.probability = other.optionalProbability().map(IntegerFilter::copy).orElse(null);
        this.impact = other.optionalImpact().map(IntegerFilter::copy).orElse(null);
        this.controlCoefficient = other.optionalControlCoefficient().map(BigDecimalFilter::copy).orElse(null);
        this.grossScore = other.optionalGrossScore().map(IntegerFilter::copy).orElse(null);
        this.residualScore = other.optionalResidualScore().map(BigDecimalFilter::copy).orElse(null);
        this.previousZone = other.optionalPreviousZone().map(RiskZoneFilter::copy).orElse(null);
        this.zone = other.optionalZone().map(RiskZoneFilter::copy).orElse(null);
        this.assessorId = other.optionalAssessorId().map(UUIDFilter::copy).orElse(null);
        this.assessedAt = other.optionalAssessedAt().map(InstantFilter::copy).orElse(null);
        this.riskId = other.optionalRiskId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public RiskAssessmentCriteria copy() {
        return new RiskAssessmentCriteria(this);
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

    public IntegerFilter getProbability() {
        return probability;
    }

    public Optional<IntegerFilter> optionalProbability() {
        return Optional.ofNullable(probability);
    }

    public IntegerFilter probability() {
        if (probability == null) {
            setProbability(new IntegerFilter());
        }
        return probability;
    }

    public void setProbability(IntegerFilter probability) {
        this.probability = probability;
    }

    public IntegerFilter getImpact() {
        return impact;
    }

    public Optional<IntegerFilter> optionalImpact() {
        return Optional.ofNullable(impact);
    }

    public IntegerFilter impact() {
        if (impact == null) {
            setImpact(new IntegerFilter());
        }
        return impact;
    }

    public void setImpact(IntegerFilter impact) {
        this.impact = impact;
    }

    public BigDecimalFilter getControlCoefficient() {
        return controlCoefficient;
    }

    public Optional<BigDecimalFilter> optionalControlCoefficient() {
        return Optional.ofNullable(controlCoefficient);
    }

    public BigDecimalFilter controlCoefficient() {
        if (controlCoefficient == null) {
            setControlCoefficient(new BigDecimalFilter());
        }
        return controlCoefficient;
    }

    public void setControlCoefficient(BigDecimalFilter controlCoefficient) {
        this.controlCoefficient = controlCoefficient;
    }

    public IntegerFilter getGrossScore() {
        return grossScore;
    }

    public Optional<IntegerFilter> optionalGrossScore() {
        return Optional.ofNullable(grossScore);
    }

    public IntegerFilter grossScore() {
        if (grossScore == null) {
            setGrossScore(new IntegerFilter());
        }
        return grossScore;
    }

    public void setGrossScore(IntegerFilter grossScore) {
        this.grossScore = grossScore;
    }

    public BigDecimalFilter getResidualScore() {
        return residualScore;
    }

    public Optional<BigDecimalFilter> optionalResidualScore() {
        return Optional.ofNullable(residualScore);
    }

    public BigDecimalFilter residualScore() {
        if (residualScore == null) {
            setResidualScore(new BigDecimalFilter());
        }
        return residualScore;
    }

    public void setResidualScore(BigDecimalFilter residualScore) {
        this.residualScore = residualScore;
    }

    public RiskZoneFilter getPreviousZone() {
        return previousZone;
    }

    public Optional<RiskZoneFilter> optionalPreviousZone() {
        return Optional.ofNullable(previousZone);
    }

    public RiskZoneFilter previousZone() {
        if (previousZone == null) {
            setPreviousZone(new RiskZoneFilter());
        }
        return previousZone;
    }

    public void setPreviousZone(RiskZoneFilter previousZone) {
        this.previousZone = previousZone;
    }

    public RiskZoneFilter getZone() {
        return zone;
    }

    public Optional<RiskZoneFilter> optionalZone() {
        return Optional.ofNullable(zone);
    }

    public RiskZoneFilter zone() {
        if (zone == null) {
            setZone(new RiskZoneFilter());
        }
        return zone;
    }

    public void setZone(RiskZoneFilter zone) {
        this.zone = zone;
    }

    public UUIDFilter getAssessorId() {
        return assessorId;
    }

    public Optional<UUIDFilter> optionalAssessorId() {
        return Optional.ofNullable(assessorId);
    }

    public UUIDFilter assessorId() {
        if (assessorId == null) {
            setAssessorId(new UUIDFilter());
        }
        return assessorId;
    }

    public void setAssessorId(UUIDFilter assessorId) {
        this.assessorId = assessorId;
    }

    public InstantFilter getAssessedAt() {
        return assessedAt;
    }

    public Optional<InstantFilter> optionalAssessedAt() {
        return Optional.ofNullable(assessedAt);
    }

    public InstantFilter assessedAt() {
        if (assessedAt == null) {
            setAssessedAt(new InstantFilter());
        }
        return assessedAt;
    }

    public void setAssessedAt(InstantFilter assessedAt) {
        this.assessedAt = assessedAt;
    }

    public UUIDFilter getRiskId() {
        return riskId;
    }

    public Optional<UUIDFilter> optionalRiskId() {
        return Optional.ofNullable(riskId);
    }

    public UUIDFilter riskId() {
        if (riskId == null) {
            setRiskId(new UUIDFilter());
        }
        return riskId;
    }

    public void setRiskId(UUIDFilter riskId) {
        this.riskId = riskId;
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
        final RiskAssessmentCriteria that = (RiskAssessmentCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(probability, that.probability) &&
            Objects.equals(impact, that.impact) &&
            Objects.equals(controlCoefficient, that.controlCoefficient) &&
            Objects.equals(grossScore, that.grossScore) &&
            Objects.equals(residualScore, that.residualScore) &&
            Objects.equals(previousZone, that.previousZone) &&
            Objects.equals(zone, that.zone) &&
            Objects.equals(assessorId, that.assessorId) &&
            Objects.equals(assessedAt, that.assessedAt) &&
            Objects.equals(riskId, that.riskId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            probability,
            impact,
            controlCoefficient,
            grossScore,
            residualScore,
            previousZone,
            zone,
            assessorId,
            assessedAt,
            riskId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RiskAssessmentCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalProbability().map(f -> "probability=" + f + ", ").orElse("") +
            optionalImpact().map(f -> "impact=" + f + ", ").orElse("") +
            optionalControlCoefficient().map(f -> "controlCoefficient=" + f + ", ").orElse("") +
            optionalGrossScore().map(f -> "grossScore=" + f + ", ").orElse("") +
            optionalResidualScore().map(f -> "residualScore=" + f + ", ").orElse("") +
            optionalPreviousZone().map(f -> "previousZone=" + f + ", ").orElse("") +
            optionalZone().map(f -> "zone=" + f + ", ").orElse("") +
            optionalAssessorId().map(f -> "assessorId=" + f + ", ").orElse("") +
            optionalAssessedAt().map(f -> "assessedAt=" + f + ", ").orElse("") +
            optionalRiskId().map(f -> "riskId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
