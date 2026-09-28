package cm.gimac.des.pilotage.service.criteria;

import cm.gimac.des.pilotage.domain.enumeration.DecisionState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.Decision} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.DecisionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /decisions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DecisionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering DecisionState
     */
    public static class DecisionStateFilter extends Filter<DecisionState> {

        public DecisionStateFilter() {}

        public DecisionStateFilter(DecisionStateFilter filter) {
            super(filter);
        }

        @Override
        public DecisionStateFilter copy() {
            return new DecisionStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter text;

    private UUIDFilter ownerId;

    private LocalDateFilter dueDate;

    private DecisionStateFilter state;

    private StringFilter returnReason;

    private InstantFilter convertedAt;

    private UUIDFilter taskId;

    private UUIDFilter meetingId;

    private UUIDFilter rectifiesId;

    private Boolean distinct;

    public DecisionCriteria() {}

    public DecisionCriteria(DecisionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.text = other.optionalText().map(StringFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(UUIDFilter::copy).orElse(null);
        this.dueDate = other.optionalDueDate().map(LocalDateFilter::copy).orElse(null);
        this.state = other.optionalState().map(DecisionStateFilter::copy).orElse(null);
        this.returnReason = other.optionalReturnReason().map(StringFilter::copy).orElse(null);
        this.convertedAt = other.optionalConvertedAt().map(InstantFilter::copy).orElse(null);
        this.taskId = other.optionalTaskId().map(UUIDFilter::copy).orElse(null);
        this.meetingId = other.optionalMeetingId().map(UUIDFilter::copy).orElse(null);
        this.rectifiesId = other.optionalRectifiesId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DecisionCriteria copy() {
        return new DecisionCriteria(this);
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

    public StringFilter getText() {
        return text;
    }

    public Optional<StringFilter> optionalText() {
        return Optional.ofNullable(text);
    }

    public StringFilter text() {
        if (text == null) {
            setText(new StringFilter());
        }
        return text;
    }

    public void setText(StringFilter text) {
        this.text = text;
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

    public LocalDateFilter getDueDate() {
        return dueDate;
    }

    public Optional<LocalDateFilter> optionalDueDate() {
        return Optional.ofNullable(dueDate);
    }

    public LocalDateFilter dueDate() {
        if (dueDate == null) {
            setDueDate(new LocalDateFilter());
        }
        return dueDate;
    }

    public void setDueDate(LocalDateFilter dueDate) {
        this.dueDate = dueDate;
    }

    public DecisionStateFilter getState() {
        return state;
    }

    public Optional<DecisionStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public DecisionStateFilter state() {
        if (state == null) {
            setState(new DecisionStateFilter());
        }
        return state;
    }

    public void setState(DecisionStateFilter state) {
        this.state = state;
    }

    public StringFilter getReturnReason() {
        return returnReason;
    }

    public Optional<StringFilter> optionalReturnReason() {
        return Optional.ofNullable(returnReason);
    }

    public StringFilter returnReason() {
        if (returnReason == null) {
            setReturnReason(new StringFilter());
        }
        return returnReason;
    }

    public void setReturnReason(StringFilter returnReason) {
        this.returnReason = returnReason;
    }

    public InstantFilter getConvertedAt() {
        return convertedAt;
    }

    public Optional<InstantFilter> optionalConvertedAt() {
        return Optional.ofNullable(convertedAt);
    }

    public InstantFilter convertedAt() {
        if (convertedAt == null) {
            setConvertedAt(new InstantFilter());
        }
        return convertedAt;
    }

    public void setConvertedAt(InstantFilter convertedAt) {
        this.convertedAt = convertedAt;
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

    public UUIDFilter getMeetingId() {
        return meetingId;
    }

    public Optional<UUIDFilter> optionalMeetingId() {
        return Optional.ofNullable(meetingId);
    }

    public UUIDFilter meetingId() {
        if (meetingId == null) {
            setMeetingId(new UUIDFilter());
        }
        return meetingId;
    }

    public void setMeetingId(UUIDFilter meetingId) {
        this.meetingId = meetingId;
    }

    public UUIDFilter getRectifiesId() {
        return rectifiesId;
    }

    public Optional<UUIDFilter> optionalRectifiesId() {
        return Optional.ofNullable(rectifiesId);
    }

    public UUIDFilter rectifiesId() {
        if (rectifiesId == null) {
            setRectifiesId(new UUIDFilter());
        }
        return rectifiesId;
    }

    public void setRectifiesId(UUIDFilter rectifiesId) {
        this.rectifiesId = rectifiesId;
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
        final DecisionCriteria that = (DecisionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(text, that.text) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(dueDate, that.dueDate) &&
            Objects.equals(state, that.state) &&
            Objects.equals(returnReason, that.returnReason) &&
            Objects.equals(convertedAt, that.convertedAt) &&
            Objects.equals(taskId, that.taskId) &&
            Objects.equals(meetingId, that.meetingId) &&
            Objects.equals(rectifiesId, that.rectifiesId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, ownerId, dueDate, state, returnReason, convertedAt, taskId, meetingId, rectifiesId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DecisionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalText().map(f -> "text=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalDueDate().map(f -> "dueDate=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalReturnReason().map(f -> "returnReason=" + f + ", ").orElse("") +
            optionalConvertedAt().map(f -> "convertedAt=" + f + ", ").orElse("") +
            optionalTaskId().map(f -> "taskId=" + f + ", ").orElse("") +
            optionalMeetingId().map(f -> "meetingId=" + f + ", ").orElse("") +
            optionalRectifiesId().map(f -> "rectifiesId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
