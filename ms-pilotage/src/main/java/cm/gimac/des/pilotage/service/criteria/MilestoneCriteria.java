package cm.gimac.des.pilotage.service.criteria;

import cm.gimac.des.pilotage.domain.enumeration.MilestoneState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.Milestone} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.MilestoneResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /milestones?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MilestoneCriteria implements Serializable, Criteria {

    /**
     * Class for filtering MilestoneState
     */
    public static class MilestoneStateFilter extends Filter<MilestoneState> {

        public MilestoneStateFilter() {}

        public MilestoneStateFilter(MilestoneStateFilter filter) {
            super(filter);
        }

        @Override
        public MilestoneStateFilter copy() {
            return new MilestoneStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter label;

    private DurationFilter optimistic;

    private DurationFilter mostLikely;

    private DurationFilter pessimistic;

    private DurationFilter expectedDuration;

    private BigDecimalFilter variance;

    private LocalDateFilter earliestStart;

    private LocalDateFilter latestStart;

    private DurationFilter totalFloat;

    private DurationFilter freeFloat;

    private BooleanFilter critical;

    private MilestoneStateFilter state;

    private DurationFilter actualDuration;

    private InstantFilter reachedAt;

    private UUIDFilter projectId;

    private UUIDFilter predecessorsId;

    private UUIDFilter successorsId;

    private Boolean distinct;

    public MilestoneCriteria() {}

    public MilestoneCriteria(MilestoneCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.label = other.optionalLabel().map(StringFilter::copy).orElse(null);
        this.optimistic = other.optionalOptimistic().map(DurationFilter::copy).orElse(null);
        this.mostLikely = other.optionalMostLikely().map(DurationFilter::copy).orElse(null);
        this.pessimistic = other.optionalPessimistic().map(DurationFilter::copy).orElse(null);
        this.expectedDuration = other.optionalExpectedDuration().map(DurationFilter::copy).orElse(null);
        this.variance = other.optionalVariance().map(BigDecimalFilter::copy).orElse(null);
        this.earliestStart = other.optionalEarliestStart().map(LocalDateFilter::copy).orElse(null);
        this.latestStart = other.optionalLatestStart().map(LocalDateFilter::copy).orElse(null);
        this.totalFloat = other.optionalTotalFloat().map(DurationFilter::copy).orElse(null);
        this.freeFloat = other.optionalFreeFloat().map(DurationFilter::copy).orElse(null);
        this.critical = other.optionalCritical().map(BooleanFilter::copy).orElse(null);
        this.state = other.optionalState().map(MilestoneStateFilter::copy).orElse(null);
        this.actualDuration = other.optionalActualDuration().map(DurationFilter::copy).orElse(null);
        this.reachedAt = other.optionalReachedAt().map(InstantFilter::copy).orElse(null);
        this.projectId = other.optionalProjectId().map(UUIDFilter::copy).orElse(null);
        this.predecessorsId = other.optionalPredecessorsId().map(UUIDFilter::copy).orElse(null);
        this.successorsId = other.optionalSuccessorsId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public MilestoneCriteria copy() {
        return new MilestoneCriteria(this);
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

    public DurationFilter getOptimistic() {
        return optimistic;
    }

    public Optional<DurationFilter> optionalOptimistic() {
        return Optional.ofNullable(optimistic);
    }

    public DurationFilter optimistic() {
        if (optimistic == null) {
            setOptimistic(new DurationFilter());
        }
        return optimistic;
    }

    public void setOptimistic(DurationFilter optimistic) {
        this.optimistic = optimistic;
    }

    public DurationFilter getMostLikely() {
        return mostLikely;
    }

    public Optional<DurationFilter> optionalMostLikely() {
        return Optional.ofNullable(mostLikely);
    }

    public DurationFilter mostLikely() {
        if (mostLikely == null) {
            setMostLikely(new DurationFilter());
        }
        return mostLikely;
    }

    public void setMostLikely(DurationFilter mostLikely) {
        this.mostLikely = mostLikely;
    }

    public DurationFilter getPessimistic() {
        return pessimistic;
    }

    public Optional<DurationFilter> optionalPessimistic() {
        return Optional.ofNullable(pessimistic);
    }

    public DurationFilter pessimistic() {
        if (pessimistic == null) {
            setPessimistic(new DurationFilter());
        }
        return pessimistic;
    }

    public void setPessimistic(DurationFilter pessimistic) {
        this.pessimistic = pessimistic;
    }

    public DurationFilter getExpectedDuration() {
        return expectedDuration;
    }

    public Optional<DurationFilter> optionalExpectedDuration() {
        return Optional.ofNullable(expectedDuration);
    }

    public DurationFilter expectedDuration() {
        if (expectedDuration == null) {
            setExpectedDuration(new DurationFilter());
        }
        return expectedDuration;
    }

    public void setExpectedDuration(DurationFilter expectedDuration) {
        this.expectedDuration = expectedDuration;
    }

    public BigDecimalFilter getVariance() {
        return variance;
    }

    public Optional<BigDecimalFilter> optionalVariance() {
        return Optional.ofNullable(variance);
    }

    public BigDecimalFilter variance() {
        if (variance == null) {
            setVariance(new BigDecimalFilter());
        }
        return variance;
    }

    public void setVariance(BigDecimalFilter variance) {
        this.variance = variance;
    }

    public LocalDateFilter getEarliestStart() {
        return earliestStart;
    }

    public Optional<LocalDateFilter> optionalEarliestStart() {
        return Optional.ofNullable(earliestStart);
    }

    public LocalDateFilter earliestStart() {
        if (earliestStart == null) {
            setEarliestStart(new LocalDateFilter());
        }
        return earliestStart;
    }

    public void setEarliestStart(LocalDateFilter earliestStart) {
        this.earliestStart = earliestStart;
    }

    public LocalDateFilter getLatestStart() {
        return latestStart;
    }

    public Optional<LocalDateFilter> optionalLatestStart() {
        return Optional.ofNullable(latestStart);
    }

    public LocalDateFilter latestStart() {
        if (latestStart == null) {
            setLatestStart(new LocalDateFilter());
        }
        return latestStart;
    }

    public void setLatestStart(LocalDateFilter latestStart) {
        this.latestStart = latestStart;
    }

    public DurationFilter getTotalFloat() {
        return totalFloat;
    }

    public Optional<DurationFilter> optionalTotalFloat() {
        return Optional.ofNullable(totalFloat);
    }

    public DurationFilter totalFloat() {
        if (totalFloat == null) {
            setTotalFloat(new DurationFilter());
        }
        return totalFloat;
    }

    public void setTotalFloat(DurationFilter totalFloat) {
        this.totalFloat = totalFloat;
    }

    public DurationFilter getFreeFloat() {
        return freeFloat;
    }

    public Optional<DurationFilter> optionalFreeFloat() {
        return Optional.ofNullable(freeFloat);
    }

    public DurationFilter freeFloat() {
        if (freeFloat == null) {
            setFreeFloat(new DurationFilter());
        }
        return freeFloat;
    }

    public void setFreeFloat(DurationFilter freeFloat) {
        this.freeFloat = freeFloat;
    }

    public BooleanFilter getCritical() {
        return critical;
    }

    public Optional<BooleanFilter> optionalCritical() {
        return Optional.ofNullable(critical);
    }

    public BooleanFilter critical() {
        if (critical == null) {
            setCritical(new BooleanFilter());
        }
        return critical;
    }

    public void setCritical(BooleanFilter critical) {
        this.critical = critical;
    }

    public MilestoneStateFilter getState() {
        return state;
    }

    public Optional<MilestoneStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public MilestoneStateFilter state() {
        if (state == null) {
            setState(new MilestoneStateFilter());
        }
        return state;
    }

    public void setState(MilestoneStateFilter state) {
        this.state = state;
    }

    public DurationFilter getActualDuration() {
        return actualDuration;
    }

    public Optional<DurationFilter> optionalActualDuration() {
        return Optional.ofNullable(actualDuration);
    }

    public DurationFilter actualDuration() {
        if (actualDuration == null) {
            setActualDuration(new DurationFilter());
        }
        return actualDuration;
    }

    public void setActualDuration(DurationFilter actualDuration) {
        this.actualDuration = actualDuration;
    }

    public InstantFilter getReachedAt() {
        return reachedAt;
    }

    public Optional<InstantFilter> optionalReachedAt() {
        return Optional.ofNullable(reachedAt);
    }

    public InstantFilter reachedAt() {
        if (reachedAt == null) {
            setReachedAt(new InstantFilter());
        }
        return reachedAt;
    }

    public void setReachedAt(InstantFilter reachedAt) {
        this.reachedAt = reachedAt;
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

    public UUIDFilter getPredecessorsId() {
        return predecessorsId;
    }

    public Optional<UUIDFilter> optionalPredecessorsId() {
        return Optional.ofNullable(predecessorsId);
    }

    public UUIDFilter predecessorsId() {
        if (predecessorsId == null) {
            setPredecessorsId(new UUIDFilter());
        }
        return predecessorsId;
    }

    public void setPredecessorsId(UUIDFilter predecessorsId) {
        this.predecessorsId = predecessorsId;
    }

    public UUIDFilter getSuccessorsId() {
        return successorsId;
    }

    public Optional<UUIDFilter> optionalSuccessorsId() {
        return Optional.ofNullable(successorsId);
    }

    public UUIDFilter successorsId() {
        if (successorsId == null) {
            setSuccessorsId(new UUIDFilter());
        }
        return successorsId;
    }

    public void setSuccessorsId(UUIDFilter successorsId) {
        this.successorsId = successorsId;
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
        final MilestoneCriteria that = (MilestoneCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(label, that.label) &&
            Objects.equals(optimistic, that.optimistic) &&
            Objects.equals(mostLikely, that.mostLikely) &&
            Objects.equals(pessimistic, that.pessimistic) &&
            Objects.equals(expectedDuration, that.expectedDuration) &&
            Objects.equals(variance, that.variance) &&
            Objects.equals(earliestStart, that.earliestStart) &&
            Objects.equals(latestStart, that.latestStart) &&
            Objects.equals(totalFloat, that.totalFloat) &&
            Objects.equals(freeFloat, that.freeFloat) &&
            Objects.equals(critical, that.critical) &&
            Objects.equals(state, that.state) &&
            Objects.equals(actualDuration, that.actualDuration) &&
            Objects.equals(reachedAt, that.reachedAt) &&
            Objects.equals(projectId, that.projectId) &&
            Objects.equals(predecessorsId, that.predecessorsId) &&
            Objects.equals(successorsId, that.successorsId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            label,
            optimistic,
            mostLikely,
            pessimistic,
            expectedDuration,
            variance,
            earliestStart,
            latestStart,
            totalFloat,
            freeFloat,
            critical,
            state,
            actualDuration,
            reachedAt,
            projectId,
            predecessorsId,
            successorsId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MilestoneCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalLabel().map(f -> "label=" + f + ", ").orElse("") +
            optionalOptimistic().map(f -> "optimistic=" + f + ", ").orElse("") +
            optionalMostLikely().map(f -> "mostLikely=" + f + ", ").orElse("") +
            optionalPessimistic().map(f -> "pessimistic=" + f + ", ").orElse("") +
            optionalExpectedDuration().map(f -> "expectedDuration=" + f + ", ").orElse("") +
            optionalVariance().map(f -> "variance=" + f + ", ").orElse("") +
            optionalEarliestStart().map(f -> "earliestStart=" + f + ", ").orElse("") +
            optionalLatestStart().map(f -> "latestStart=" + f + ", ").orElse("") +
            optionalTotalFloat().map(f -> "totalFloat=" + f + ", ").orElse("") +
            optionalFreeFloat().map(f -> "freeFloat=" + f + ", ").orElse("") +
            optionalCritical().map(f -> "critical=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalActualDuration().map(f -> "actualDuration=" + f + ", ").orElse("") +
            optionalReachedAt().map(f -> "reachedAt=" + f + ", ").orElse("") +
            optionalProjectId().map(f -> "projectId=" + f + ", ").orElse("") +
            optionalPredecessorsId().map(f -> "predecessorsId=" + f + ", ").orElse("") +
            optionalSuccessorsId().map(f -> "successorsId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
