package cm.gimac.des.incident.service.criteria;

import cm.gimac.des.incident.domain.enumeration.RemediationKind;
import cm.gimac.des.incident.domain.enumeration.RemediationState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.incident.domain.RemediationAction} entity. This class is used
 * in {@link cm.gimac.des.incident.web.rest.RemediationActionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /remediation-actions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RemediationActionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering RemediationKind
     */
    public static class RemediationKindFilter extends Filter<RemediationKind> {

        public RemediationKindFilter() {}

        public RemediationKindFilter(RemediationKindFilter filter) {
            super(filter);
        }

        @Override
        public RemediationKindFilter copy() {
            return new RemediationKindFilter(this);
        }
    }

    /**
     * Class for filtering RemediationState
     */
    public static class RemediationStateFilter extends Filter<RemediationState> {

        public RemediationStateFilter() {}

        public RemediationStateFilter(RemediationStateFilter filter) {
            super(filter);
        }

        @Override
        public RemediationStateFilter copy() {
            return new RemediationStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private RemediationKindFilter kind;

    private StringFilter description;

    private UUIDFilter ownerId;

    private LocalDateFilter dueDate;

    private InstantFilter requestedAt;

    private StringFilter taskRef;

    private RemediationStateFilter state;

    private InstantFilter completedAt;

    private UUIDFilter incidentId;

    private UUIDFilter crisisDecisionId;

    private Boolean distinct;

    public RemediationActionCriteria() {}

    public RemediationActionCriteria(RemediationActionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.kind = other.optionalKind().map(RemediationKindFilter::copy).orElse(null);
        this.description = other.optionalDescription().map(StringFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(UUIDFilter::copy).orElse(null);
        this.dueDate = other.optionalDueDate().map(LocalDateFilter::copy).orElse(null);
        this.requestedAt = other.optionalRequestedAt().map(InstantFilter::copy).orElse(null);
        this.taskRef = other.optionalTaskRef().map(StringFilter::copy).orElse(null);
        this.state = other.optionalState().map(RemediationStateFilter::copy).orElse(null);
        this.completedAt = other.optionalCompletedAt().map(InstantFilter::copy).orElse(null);
        this.incidentId = other.optionalIncidentId().map(UUIDFilter::copy).orElse(null);
        this.crisisDecisionId = other.optionalCrisisDecisionId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public RemediationActionCriteria copy() {
        return new RemediationActionCriteria(this);
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

    public RemediationKindFilter getKind() {
        return kind;
    }

    public Optional<RemediationKindFilter> optionalKind() {
        return Optional.ofNullable(kind);
    }

    public RemediationKindFilter kind() {
        if (kind == null) {
            setKind(new RemediationKindFilter());
        }
        return kind;
    }

    public void setKind(RemediationKindFilter kind) {
        this.kind = kind;
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

    public InstantFilter getRequestedAt() {
        return requestedAt;
    }

    public Optional<InstantFilter> optionalRequestedAt() {
        return Optional.ofNullable(requestedAt);
    }

    public InstantFilter requestedAt() {
        if (requestedAt == null) {
            setRequestedAt(new InstantFilter());
        }
        return requestedAt;
    }

    public void setRequestedAt(InstantFilter requestedAt) {
        this.requestedAt = requestedAt;
    }

    public StringFilter getTaskRef() {
        return taskRef;
    }

    public Optional<StringFilter> optionalTaskRef() {
        return Optional.ofNullable(taskRef);
    }

    public StringFilter taskRef() {
        if (taskRef == null) {
            setTaskRef(new StringFilter());
        }
        return taskRef;
    }

    public void setTaskRef(StringFilter taskRef) {
        this.taskRef = taskRef;
    }

    public RemediationStateFilter getState() {
        return state;
    }

    public Optional<RemediationStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public RemediationStateFilter state() {
        if (state == null) {
            setState(new RemediationStateFilter());
        }
        return state;
    }

    public void setState(RemediationStateFilter state) {
        this.state = state;
    }

    public InstantFilter getCompletedAt() {
        return completedAt;
    }

    public Optional<InstantFilter> optionalCompletedAt() {
        return Optional.ofNullable(completedAt);
    }

    public InstantFilter completedAt() {
        if (completedAt == null) {
            setCompletedAt(new InstantFilter());
        }
        return completedAt;
    }

    public void setCompletedAt(InstantFilter completedAt) {
        this.completedAt = completedAt;
    }

    public UUIDFilter getIncidentId() {
        return incidentId;
    }

    public Optional<UUIDFilter> optionalIncidentId() {
        return Optional.ofNullable(incidentId);
    }

    public UUIDFilter incidentId() {
        if (incidentId == null) {
            setIncidentId(new UUIDFilter());
        }
        return incidentId;
    }

    public void setIncidentId(UUIDFilter incidentId) {
        this.incidentId = incidentId;
    }

    public UUIDFilter getCrisisDecisionId() {
        return crisisDecisionId;
    }

    public Optional<UUIDFilter> optionalCrisisDecisionId() {
        return Optional.ofNullable(crisisDecisionId);
    }

    public UUIDFilter crisisDecisionId() {
        if (crisisDecisionId == null) {
            setCrisisDecisionId(new UUIDFilter());
        }
        return crisisDecisionId;
    }

    public void setCrisisDecisionId(UUIDFilter crisisDecisionId) {
        this.crisisDecisionId = crisisDecisionId;
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
        final RemediationActionCriteria that = (RemediationActionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(kind, that.kind) &&
            Objects.equals(description, that.description) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(dueDate, that.dueDate) &&
            Objects.equals(requestedAt, that.requestedAt) &&
            Objects.equals(taskRef, that.taskRef) &&
            Objects.equals(state, that.state) &&
            Objects.equals(completedAt, that.completedAt) &&
            Objects.equals(incidentId, that.incidentId) &&
            Objects.equals(crisisDecisionId, that.crisisDecisionId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            kind,
            description,
            ownerId,
            dueDate,
            requestedAt,
            taskRef,
            state,
            completedAt,
            incidentId,
            crisisDecisionId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RemediationActionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalKind().map(f -> "kind=" + f + ", ").orElse("") +
            optionalDescription().map(f -> "description=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalDueDate().map(f -> "dueDate=" + f + ", ").orElse("") +
            optionalRequestedAt().map(f -> "requestedAt=" + f + ", ").orElse("") +
            optionalTaskRef().map(f -> "taskRef=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalCompletedAt().map(f -> "completedAt=" + f + ", ").orElse("") +
            optionalIncidentId().map(f -> "incidentId=" + f + ", ").orElse("") +
            optionalCrisisDecisionId().map(f -> "crisisDecisionId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
