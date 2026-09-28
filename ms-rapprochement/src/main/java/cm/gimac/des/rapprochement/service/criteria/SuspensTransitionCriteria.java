package cm.gimac.des.rapprochement.service.criteria;

import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.rapprochement.domain.SuspensTransition} entity. This class is used
 * in {@link cm.gimac.des.rapprochement.web.rest.SuspensTransitionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /suspens-transitions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SuspensTransitionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering SuspensState
     */
    public static class SuspensStateFilter extends Filter<SuspensState> {

        public SuspensStateFilter() {}

        public SuspensStateFilter(SuspensStateFilter filter) {
            super(filter);
        }

        @Override
        public SuspensStateFilter copy() {
            return new SuspensStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private SuspensStateFilter fromState;

    private SuspensStateFilter toState;

    private UUIDFilter actorId;

    private InstantFilter occurredAt;

    private StringFilter justification;

    private UUIDFilter suspensId;

    private Boolean distinct;

    public SuspensTransitionCriteria() {}

    public SuspensTransitionCriteria(SuspensTransitionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.fromState = other.optionalFromState().map(SuspensStateFilter::copy).orElse(null);
        this.toState = other.optionalToState().map(SuspensStateFilter::copy).orElse(null);
        this.actorId = other.optionalActorId().map(UUIDFilter::copy).orElse(null);
        this.occurredAt = other.optionalOccurredAt().map(InstantFilter::copy).orElse(null);
        this.justification = other.optionalJustification().map(StringFilter::copy).orElse(null);
        this.suspensId = other.optionalSuspensId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public SuspensTransitionCriteria copy() {
        return new SuspensTransitionCriteria(this);
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

    public SuspensStateFilter getFromState() {
        return fromState;
    }

    public Optional<SuspensStateFilter> optionalFromState() {
        return Optional.ofNullable(fromState);
    }

    public SuspensStateFilter fromState() {
        if (fromState == null) {
            setFromState(new SuspensStateFilter());
        }
        return fromState;
    }

    public void setFromState(SuspensStateFilter fromState) {
        this.fromState = fromState;
    }

    public SuspensStateFilter getToState() {
        return toState;
    }

    public Optional<SuspensStateFilter> optionalToState() {
        return Optional.ofNullable(toState);
    }

    public SuspensStateFilter toState() {
        if (toState == null) {
            setToState(new SuspensStateFilter());
        }
        return toState;
    }

    public void setToState(SuspensStateFilter toState) {
        this.toState = toState;
    }

    public UUIDFilter getActorId() {
        return actorId;
    }

    public Optional<UUIDFilter> optionalActorId() {
        return Optional.ofNullable(actorId);
    }

    public UUIDFilter actorId() {
        if (actorId == null) {
            setActorId(new UUIDFilter());
        }
        return actorId;
    }

    public void setActorId(UUIDFilter actorId) {
        this.actorId = actorId;
    }

    public InstantFilter getOccurredAt() {
        return occurredAt;
    }

    public Optional<InstantFilter> optionalOccurredAt() {
        return Optional.ofNullable(occurredAt);
    }

    public InstantFilter occurredAt() {
        if (occurredAt == null) {
            setOccurredAt(new InstantFilter());
        }
        return occurredAt;
    }

    public void setOccurredAt(InstantFilter occurredAt) {
        this.occurredAt = occurredAt;
    }

    public StringFilter getJustification() {
        return justification;
    }

    public Optional<StringFilter> optionalJustification() {
        return Optional.ofNullable(justification);
    }

    public StringFilter justification() {
        if (justification == null) {
            setJustification(new StringFilter());
        }
        return justification;
    }

    public void setJustification(StringFilter justification) {
        this.justification = justification;
    }

    public UUIDFilter getSuspensId() {
        return suspensId;
    }

    public Optional<UUIDFilter> optionalSuspensId() {
        return Optional.ofNullable(suspensId);
    }

    public UUIDFilter suspensId() {
        if (suspensId == null) {
            setSuspensId(new UUIDFilter());
        }
        return suspensId;
    }

    public void setSuspensId(UUIDFilter suspensId) {
        this.suspensId = suspensId;
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
        final SuspensTransitionCriteria that = (SuspensTransitionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(fromState, that.fromState) &&
            Objects.equals(toState, that.toState) &&
            Objects.equals(actorId, that.actorId) &&
            Objects.equals(occurredAt, that.occurredAt) &&
            Objects.equals(justification, that.justification) &&
            Objects.equals(suspensId, that.suspensId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fromState, toState, actorId, occurredAt, justification, suspensId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SuspensTransitionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalFromState().map(f -> "fromState=" + f + ", ").orElse("") +
            optionalToState().map(f -> "toState=" + f + ", ").orElse("") +
            optionalActorId().map(f -> "actorId=" + f + ", ").orElse("") +
            optionalOccurredAt().map(f -> "occurredAt=" + f + ", ").orElse("") +
            optionalJustification().map(f -> "justification=" + f + ", ").orElse("") +
            optionalSuspensId().map(f -> "suspensId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
