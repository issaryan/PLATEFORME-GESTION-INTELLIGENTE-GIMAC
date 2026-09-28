package cm.gimac.des.pilotage.service.criteria;

import cm.gimac.des.pilotage.domain.enumeration.MeetingState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.Meeting} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.MeetingResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /meetings?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeetingCriteria implements Serializable, Criteria {

    /**
     * Class for filtering MeetingState
     */
    public static class MeetingStateFilter extends Filter<MeetingState> {

        public MeetingStateFilter() {}

        public MeetingStateFilter(MeetingStateFilter filter) {
            super(filter);
        }

        @Override
        public MeetingStateFilter copy() {
            return new MeetingStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter instance;

    private InstantFilter scheduledAt;

    private InstantFilter heldAt;

    private UUIDFilter minutesRef;

    private StringFilter minutesDigest;

    private MeetingStateFilter state;

    private Boolean distinct;

    public MeetingCriteria() {}

    public MeetingCriteria(MeetingCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.instance = other.optionalInstance().map(StringFilter::copy).orElse(null);
        this.scheduledAt = other.optionalScheduledAt().map(InstantFilter::copy).orElse(null);
        this.heldAt = other.optionalHeldAt().map(InstantFilter::copy).orElse(null);
        this.minutesRef = other.optionalMinutesRef().map(UUIDFilter::copy).orElse(null);
        this.minutesDigest = other.optionalMinutesDigest().map(StringFilter::copy).orElse(null);
        this.state = other.optionalState().map(MeetingStateFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public MeetingCriteria copy() {
        return new MeetingCriteria(this);
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

    public StringFilter getInstance() {
        return instance;
    }

    public Optional<StringFilter> optionalInstance() {
        return Optional.ofNullable(instance);
    }

    public StringFilter instance() {
        if (instance == null) {
            setInstance(new StringFilter());
        }
        return instance;
    }

    public void setInstance(StringFilter instance) {
        this.instance = instance;
    }

    public InstantFilter getScheduledAt() {
        return scheduledAt;
    }

    public Optional<InstantFilter> optionalScheduledAt() {
        return Optional.ofNullable(scheduledAt);
    }

    public InstantFilter scheduledAt() {
        if (scheduledAt == null) {
            setScheduledAt(new InstantFilter());
        }
        return scheduledAt;
    }

    public void setScheduledAt(InstantFilter scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public InstantFilter getHeldAt() {
        return heldAt;
    }

    public Optional<InstantFilter> optionalHeldAt() {
        return Optional.ofNullable(heldAt);
    }

    public InstantFilter heldAt() {
        if (heldAt == null) {
            setHeldAt(new InstantFilter());
        }
        return heldAt;
    }

    public void setHeldAt(InstantFilter heldAt) {
        this.heldAt = heldAt;
    }

    public UUIDFilter getMinutesRef() {
        return minutesRef;
    }

    public Optional<UUIDFilter> optionalMinutesRef() {
        return Optional.ofNullable(minutesRef);
    }

    public UUIDFilter minutesRef() {
        if (minutesRef == null) {
            setMinutesRef(new UUIDFilter());
        }
        return minutesRef;
    }

    public void setMinutesRef(UUIDFilter minutesRef) {
        this.minutesRef = minutesRef;
    }

    public StringFilter getMinutesDigest() {
        return minutesDigest;
    }

    public Optional<StringFilter> optionalMinutesDigest() {
        return Optional.ofNullable(minutesDigest);
    }

    public StringFilter minutesDigest() {
        if (minutesDigest == null) {
            setMinutesDigest(new StringFilter());
        }
        return minutesDigest;
    }

    public void setMinutesDigest(StringFilter minutesDigest) {
        this.minutesDigest = minutesDigest;
    }

    public MeetingStateFilter getState() {
        return state;
    }

    public Optional<MeetingStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public MeetingStateFilter state() {
        if (state == null) {
            setState(new MeetingStateFilter());
        }
        return state;
    }

    public void setState(MeetingStateFilter state) {
        this.state = state;
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
        final MeetingCriteria that = (MeetingCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(instance, that.instance) &&
            Objects.equals(scheduledAt, that.scheduledAt) &&
            Objects.equals(heldAt, that.heldAt) &&
            Objects.equals(minutesRef, that.minutesRef) &&
            Objects.equals(minutesDigest, that.minutesDigest) &&
            Objects.equals(state, that.state) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, instance, scheduledAt, heldAt, minutesRef, minutesDigest, state, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeetingCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalInstance().map(f -> "instance=" + f + ", ").orElse("") +
            optionalScheduledAt().map(f -> "scheduledAt=" + f + ", ").orElse("") +
            optionalHeldAt().map(f -> "heldAt=" + f + ", ").orElse("") +
            optionalMinutesRef().map(f -> "minutesRef=" + f + ", ").orElse("") +
            optionalMinutesDigest().map(f -> "minutesDigest=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
