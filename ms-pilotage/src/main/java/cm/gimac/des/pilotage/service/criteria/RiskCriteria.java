package cm.gimac.des.pilotage.service.criteria;

import cm.gimac.des.pilotage.domain.enumeration.RiskZone;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.Risk} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.RiskResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /risks?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RiskCriteria implements Serializable, Criteria {

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

    private StringFilter scope;

    private StringFilter description;

    private IntegerFilter probability;

    private IntegerFilter impact;

    private BigDecimalFilter controlCoefficient;

    private IntegerFilter grossScore;

    private BigDecimalFilter residualScore;

    private RiskZoneFilter zone;

    private UUIDFilter ownerId;

    private StringFilter mitigationPlan;

    private InstantFilter reviewedAt;

    private UUIDFilter projectId;

    private Boolean distinct;

    public RiskCriteria() {}

    public RiskCriteria(RiskCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.scope = other.optionalScope().map(StringFilter::copy).orElse(null);
        this.description = other.optionalDescription().map(StringFilter::copy).orElse(null);
        this.probability = other.optionalProbability().map(IntegerFilter::copy).orElse(null);
        this.impact = other.optionalImpact().map(IntegerFilter::copy).orElse(null);
        this.controlCoefficient = other.optionalControlCoefficient().map(BigDecimalFilter::copy).orElse(null);
        this.grossScore = other.optionalGrossScore().map(IntegerFilter::copy).orElse(null);
        this.residualScore = other.optionalResidualScore().map(BigDecimalFilter::copy).orElse(null);
        this.zone = other.optionalZone().map(RiskZoneFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(UUIDFilter::copy).orElse(null);
        this.mitigationPlan = other.optionalMitigationPlan().map(StringFilter::copy).orElse(null);
        this.reviewedAt = other.optionalReviewedAt().map(InstantFilter::copy).orElse(null);
        this.projectId = other.optionalProjectId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public RiskCriteria copy() {
        return new RiskCriteria(this);
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

    public StringFilter getScope() {
        return scope;
    }

    public Optional<StringFilter> optionalScope() {
        return Optional.ofNullable(scope);
    }

    public StringFilter scope() {
        if (scope == null) {
            setScope(new StringFilter());
        }
        return scope;
    }

    public void setScope(StringFilter scope) {
        this.scope = scope;
    }

    public StringFilter getDescription() {
        return description;
    }

    public Optional<StringFilter> optionalDescription() {
        return Optional.ofNullable(description);
    }

    public StringFilter description() {
        if (description == null) {
            setDescription(new StringFilter());
        }
        return description;
    }

    public void setDescription(StringFilter description) {
        this.description = description;
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

    public UUIDFilter getOwnerId() {
        return ownerId;
    }

    public Optional<UUIDFilter> optionalOwnerId() {
        return Optional.ofNullable(ownerId);
    }

    public UUIDFilter ownerId() {
        if (ownerId == null) {
            setOwnerId(new UUIDFilter());
        }
        return ownerId;
    }

    public void setOwnerId(UUIDFilter ownerId) {
        this.ownerId = ownerId;
    }

    public StringFilter getMitigationPlan() {
        return mitigationPlan;
    }

    public Optional<StringFilter> optionalMitigationPlan() {
        return Optional.ofNullable(mitigationPlan);
    }

    public StringFilter mitigationPlan() {
        if (mitigationPlan == null) {
            setMitigationPlan(new StringFilter());
        }
        return mitigationPlan;
    }

    public void setMitigationPlan(StringFilter mitigationPlan) {
        this.mitigationPlan = mitigationPlan;
    }

    public InstantFilter getReviewedAt() {
        return reviewedAt;
    }

    public Optional<InstantFilter> optionalReviewedAt() {
        return Optional.ofNullable(reviewedAt);
    }

    public InstantFilter reviewedAt() {
        if (reviewedAt == null) {
            setReviewedAt(new InstantFilter());
        }
        return reviewedAt;
    }

    public void setReviewedAt(InstantFilter reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public UUIDFilter getProjectId() {
        return projectId;
    }

    public Optional<UUIDFilter> optionalProjectId() {
        return Optional.ofNullable(projectId);
    }

    public UUIDFilter projectId() {
        if (projectId == null) {
            setProjectId(new UUIDFilter());
        }
        return projectId;
    }

    public void setProjectId(UUIDFilter projectId) {
        this.projectId = projectId;
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
        final RiskCriteria that = (RiskCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(scope, that.scope) &&
            Objects.equals(description, that.description) &&
            Objects.equals(probability, that.probability) &&
            Objects.equals(impact, that.impact) &&
            Objects.equals(controlCoefficient, that.controlCoefficient) &&
            Objects.equals(grossScore, that.grossScore) &&
            Objects.equals(residualScore, that.residualScore) &&
            Objects.equals(zone, that.zone) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(mitigationPlan, that.mitigationPlan) &&
            Objects.equals(reviewedAt, that.reviewedAt) &&
            Objects.equals(projectId, that.projectId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            scope,
            description,
            probability,
            impact,
            controlCoefficient,
            grossScore,
            residualScore,
            zone,
            ownerId,
            mitigationPlan,
            reviewedAt,
            projectId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RiskCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalScope().map(f -> "scope=" + f + ", ").orElse("") +
            optionalDescription().map(f -> "description=" + f + ", ").orElse("") +
            optionalProbability().map(f -> "probability=" + f + ", ").orElse("") +
            optionalImpact().map(f -> "impact=" + f + ", ").orElse("") +
            optionalControlCoefficient().map(f -> "controlCoefficient=" + f + ", ").orElse("") +
            optionalGrossScore().map(f -> "grossScore=" + f + ", ").orElse("") +
            optionalResidualScore().map(f -> "residualScore=" + f + ", ").orElse("") +
            optionalZone().map(f -> "zone=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalMitigationPlan().map(f -> "mitigationPlan=" + f + ", ").orElse("") +
            optionalReviewedAt().map(f -> "reviewedAt=" + f + ", ").orElse("") +
            optionalProjectId().map(f -> "projectId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
