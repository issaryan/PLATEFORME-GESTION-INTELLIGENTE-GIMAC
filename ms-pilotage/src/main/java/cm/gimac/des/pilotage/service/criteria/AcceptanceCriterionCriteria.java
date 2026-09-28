package cm.gimac.des.pilotage.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.AcceptanceCriterion} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.AcceptanceCriterionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /acceptance-criteria?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AcceptanceCriterionCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter label;

    private BooleanFilter satisfied;

    private BooleanFilter evidenceRequired;

    private UUIDFilter evidenceRef;

    private InstantFilter satisfiedAt;

    private UUIDFilter taskId;

    private Boolean distinct;

    public AcceptanceCriterionCriteria() {}

    public AcceptanceCriterionCriteria(AcceptanceCriterionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.label = other.optionalLabel().map(StringFilter::copy).orElse(null);
        this.satisfied = other.optionalSatisfied().map(BooleanFilter::copy).orElse(null);
        this.evidenceRequired = other.optionalEvidenceRequired().map(BooleanFilter::copy).orElse(null);
        this.evidenceRef = other.optionalEvidenceRef().map(UUIDFilter::copy).orElse(null);
        this.satisfiedAt = other.optionalSatisfiedAt().map(InstantFilter::copy).orElse(null);
        this.taskId = other.optionalTaskId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public AcceptanceCriterionCriteria copy() {
        return new AcceptanceCriterionCriteria(this);
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

    public BooleanFilter getSatisfied() {
        return satisfied;
    }

    public Optional<BooleanFilter> optionalSatisfied() {
        return Optional.ofNullable(satisfied);
    }

    public BooleanFilter satisfied() {
        if (satisfied == null) {
            setSatisfied(new BooleanFilter());
        }
        return satisfied;
    }

    public void setSatisfied(BooleanFilter satisfied) {
        this.satisfied = satisfied;
    }

    public BooleanFilter getEvidenceRequired() {
        return evidenceRequired;
    }

    public Optional<BooleanFilter> optionalEvidenceRequired() {
        return Optional.ofNullable(evidenceRequired);
    }

    public BooleanFilter evidenceRequired() {
        if (evidenceRequired == null) {
            setEvidenceRequired(new BooleanFilter());
        }
        return evidenceRequired;
    }

    public void setEvidenceRequired(BooleanFilter evidenceRequired) {
        this.evidenceRequired = evidenceRequired;
    }

    public UUIDFilter getEvidenceRef() {
        return evidenceRef;
    }

    public Optional<UUIDFilter> optionalEvidenceRef() {
        return Optional.ofNullable(evidenceRef);
    }

    public UUIDFilter evidenceRef() {
        if (evidenceRef == null) {
            setEvidenceRef(new UUIDFilter());
        }
        return evidenceRef;
    }

    public void setEvidenceRef(UUIDFilter evidenceRef) {
        this.evidenceRef = evidenceRef;
    }

    public InstantFilter getSatisfiedAt() {
        return satisfiedAt;
    }

    public Optional<InstantFilter> optionalSatisfiedAt() {
        return Optional.ofNullable(satisfiedAt);
    }

    public InstantFilter satisfiedAt() {
        if (satisfiedAt == null) {
            setSatisfiedAt(new InstantFilter());
        }
        return satisfiedAt;
    }

    public void setSatisfiedAt(InstantFilter satisfiedAt) {
        this.satisfiedAt = satisfiedAt;
    }

    public UUIDFilter getTaskId() {
        return taskId;
    }

    public Optional<UUIDFilter> optionalTaskId() {
        return Optional.ofNullable(taskId);
    }

    public UUIDFilter taskId() {
        if (taskId == null) {
            setTaskId(new UUIDFilter());
        }
        return taskId;
    }

    public void setTaskId(UUIDFilter taskId) {
        this.taskId = taskId;
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
        final AcceptanceCriterionCriteria that = (AcceptanceCriterionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(label, that.label) &&
            Objects.equals(satisfied, that.satisfied) &&
            Objects.equals(evidenceRequired, that.evidenceRequired) &&
            Objects.equals(evidenceRef, that.evidenceRef) &&
            Objects.equals(satisfiedAt, that.satisfiedAt) &&
            Objects.equals(taskId, that.taskId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, satisfied, evidenceRequired, evidenceRef, satisfiedAt, taskId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AcceptanceCriterionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalLabel().map(f -> "label=" + f + ", ").orElse("") +
            optionalSatisfied().map(f -> "satisfied=" + f + ", ").orElse("") +
            optionalEvidenceRequired().map(f -> "evidenceRequired=" + f + ", ").orElse("") +
            optionalEvidenceRef().map(f -> "evidenceRef=" + f + ", ").orElse("") +
            optionalSatisfiedAt().map(f -> "satisfiedAt=" + f + ", ").orElse("") +
            optionalTaskId().map(f -> "taskId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
