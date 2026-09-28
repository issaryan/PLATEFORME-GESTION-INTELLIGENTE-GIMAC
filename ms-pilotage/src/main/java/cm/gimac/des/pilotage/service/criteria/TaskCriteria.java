package cm.gimac.des.pilotage.service.criteria;

import cm.gimac.des.pilotage.domain.enumeration.OriginKind;
import cm.gimac.des.pilotage.domain.enumeration.TaskState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.Task} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.TaskResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /tasks?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TaskCriteria implements Serializable, Criteria {

    /**
     * Class for filtering OriginKind
     */
    public static class OriginKindFilter extends Filter<OriginKind> {

        public OriginKindFilter() {}

        public OriginKindFilter(OriginKindFilter filter) {
            super(filter);
        }

        @Override
        public OriginKindFilter copy() {
            return new OriginKindFilter(this);
        }
    }

    /**
     * Class for filtering TaskState
     */
    public static class TaskStateFilter extends Filter<TaskState> {

        public TaskStateFilter() {}

        public TaskStateFilter(TaskStateFilter filter) {
            super(filter);
        }

        @Override
        public TaskStateFilter copy() {
            return new TaskStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter title;

    private UUIDFilter assigneeId;

    private LocalDateFilter dueDate;

    private OriginKindFilter originKind;

    private StringFilter originReference;

    private StringFilter originService;

    private InstantFilter startedAt;

    private InstantFilter completedAt;

    private TaskStateFilter state;

    private IntegerFilter reminderLevel;

    private InstantFilter closureProposedAt;

    private UUIDFilter boardId;

    private UUIDFilter boardColumnId;

    private UUIDFilter actionSheetId;

    private UUIDFilter decisionId;

    private Boolean distinct;

    public TaskCriteria() {}

    public TaskCriteria(TaskCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.assigneeId = other.optionalAssigneeId().map(UUIDFilter::copy).orElse(null);
        this.dueDate = other.optionalDueDate().map(LocalDateFilter::copy).orElse(null);
        this.originKind = other.optionalOriginKind().map(OriginKindFilter::copy).orElse(null);
        this.originReference = other.optionalOriginReference().map(StringFilter::copy).orElse(null);
        this.originService = other.optionalOriginService().map(StringFilter::copy).orElse(null);
        this.startedAt = other.optionalStartedAt().map(InstantFilter::copy).orElse(null);
        this.completedAt = other.optionalCompletedAt().map(InstantFilter::copy).orElse(null);
        this.state = other.optionalState().map(TaskStateFilter::copy).orElse(null);
        this.reminderLevel = other.optionalReminderLevel().map(IntegerFilter::copy).orElse(null);
        this.closureProposedAt = other.optionalClosureProposedAt().map(InstantFilter::copy).orElse(null);
        this.boardId = other.optionalBoardId().map(UUIDFilter::copy).orElse(null);
        this.boardColumnId = other.optionalBoardColumnId().map(UUIDFilter::copy).orElse(null);
        this.actionSheetId = other.optionalActionSheetId().map(UUIDFilter::copy).orElse(null);
        this.decisionId = other.optionalDecisionId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public TaskCriteria copy() {
        return new TaskCriteria(this);
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

    public StringFilter getTitle() {
        return title;
    }

    public Optional<StringFilter> optionalTitle() {
        return Optional.ofNullable(title);
    }

    public StringFilter title() {
        if (title == null) {
            setTitle(new StringFilter());
        }
        return title;
    }

    public void setTitle(StringFilter title) {
        this.title = title;
    }

    public UUIDFilter getAssigneeId() {
        return assigneeId;
    }

    public Optional<UUIDFilter> optionalAssigneeId() {
        return Optional.ofNullable(assigneeId);
    }

    public UUIDFilter assigneeId() {
        if (assigneeId == null) {
            setAssigneeId(new UUIDFilter());
        }
        return assigneeId;
    }

    public void setAssigneeId(UUIDFilter assigneeId) {
        this.assigneeId = assigneeId;
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

    public OriginKindFilter getOriginKind() {
        return originKind;
    }

    public Optional<OriginKindFilter> optionalOriginKind() {
        return Optional.ofNullable(originKind);
    }

    public OriginKindFilter originKind() {
        if (originKind == null) {
            setOriginKind(new OriginKindFilter());
        }
        return originKind;
    }

    public void setOriginKind(OriginKindFilter originKind) {
        this.originKind = originKind;
    }

    public StringFilter getOriginReference() {
        return originReference;
    }

    public Optional<StringFilter> optionalOriginReference() {
        return Optional.ofNullable(originReference);
    }

    public StringFilter originReference() {
        if (originReference == null) {
            setOriginReference(new StringFilter());
        }
        return originReference;
    }

    public void setOriginReference(StringFilter originReference) {
        this.originReference = originReference;
    }

    public StringFilter getOriginService() {
        return originService;
    }

    public Optional<StringFilter> optionalOriginService() {
        return Optional.ofNullable(originService);
    }

    public StringFilter originService() {
        if (originService == null) {
            setOriginService(new StringFilter());
        }
        return originService;
    }

    public void setOriginService(StringFilter originService) {
        this.originService = originService;
    }

    public InstantFilter getStartedAt() {
        return startedAt;
    }

    public Optional<InstantFilter> optionalStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public InstantFilter startedAt() {
        if (startedAt == null) {
            setStartedAt(new InstantFilter());
        }
        return startedAt;
    }

    public void setStartedAt(InstantFilter startedAt) {
        this.startedAt = startedAt;
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

    public TaskStateFilter getState() {
        return state;
    }

    public Optional<TaskStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public TaskStateFilter state() {
        if (state == null) {
            setState(new TaskStateFilter());
        }
        return state;
    }

    public void setState(TaskStateFilter state) {
        this.state = state;
    }

    public IntegerFilter getReminderLevel() {
        return reminderLevel;
    }

    public Optional<IntegerFilter> optionalReminderLevel() {
        return Optional.ofNullable(reminderLevel);
    }

    public IntegerFilter reminderLevel() {
        if (reminderLevel == null) {
            setReminderLevel(new IntegerFilter());
        }
        return reminderLevel;
    }

    public void setReminderLevel(IntegerFilter reminderLevel) {
        this.reminderLevel = reminderLevel;
    }

    public InstantFilter getClosureProposedAt() {
        return closureProposedAt;
    }

    public Optional<InstantFilter> optionalClosureProposedAt() {
        return Optional.ofNullable(closureProposedAt);
    }

    public InstantFilter closureProposedAt() {
        if (closureProposedAt == null) {
            setClosureProposedAt(new InstantFilter());
        }
        return closureProposedAt;
    }

    public void setClosureProposedAt(InstantFilter closureProposedAt) {
        this.closureProposedAt = closureProposedAt;
    }

    public UUIDFilter getBoardId() {
        return boardId;
    }

    public Optional<UUIDFilter> optionalBoardId() {
        return Optional.ofNullable(boardId);
    }

    public UUIDFilter boardId() {
        if (boardId == null) {
            setBoardId(new UUIDFilter());
        }
        return boardId;
    }

    public void setBoardId(UUIDFilter boardId) {
        this.boardId = boardId;
    }

    public UUIDFilter getBoardColumnId() {
        return boardColumnId;
    }

    public Optional<UUIDFilter> optionalBoardColumnId() {
        return Optional.ofNullable(boardColumnId);
    }

    public UUIDFilter boardColumnId() {
        if (boardColumnId == null) {
            setBoardColumnId(new UUIDFilter());
        }
        return boardColumnId;
    }

    public void setBoardColumnId(UUIDFilter boardColumnId) {
        this.boardColumnId = boardColumnId;
    }

    public UUIDFilter getActionSheetId() {
        return actionSheetId;
    }

    public Optional<UUIDFilter> optionalActionSheetId() {
        return Optional.ofNullable(actionSheetId);
    }

    public UUIDFilter actionSheetId() {
        if (actionSheetId == null) {
            setActionSheetId(new UUIDFilter());
        }
        return actionSheetId;
    }

    public void setActionSheetId(UUIDFilter actionSheetId) {
        this.actionSheetId = actionSheetId;
    }

    public UUIDFilter getDecisionId() {
        return decisionId;
    }

    public Optional<UUIDFilter> optionalDecisionId() {
        return Optional.ofNullable(decisionId);
    }

    public UUIDFilter decisionId() {
        if (decisionId == null) {
            setDecisionId(new UUIDFilter());
        }
        return decisionId;
    }

    public void setDecisionId(UUIDFilter decisionId) {
        this.decisionId = decisionId;
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
        final TaskCriteria that = (TaskCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(assigneeId, that.assigneeId) &&
            Objects.equals(dueDate, that.dueDate) &&
            Objects.equals(originKind, that.originKind) &&
            Objects.equals(originReference, that.originReference) &&
            Objects.equals(originService, that.originService) &&
            Objects.equals(startedAt, that.startedAt) &&
            Objects.equals(completedAt, that.completedAt) &&
            Objects.equals(state, that.state) &&
            Objects.equals(reminderLevel, that.reminderLevel) &&
            Objects.equals(closureProposedAt, that.closureProposedAt) &&
            Objects.equals(boardId, that.boardId) &&
            Objects.equals(boardColumnId, that.boardColumnId) &&
            Objects.equals(actionSheetId, that.actionSheetId) &&
            Objects.equals(decisionId, that.decisionId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            title,
            assigneeId,
            dueDate,
            originKind,
            originReference,
            originService,
            startedAt,
            completedAt,
            state,
            reminderLevel,
            closureProposedAt,
            boardId,
            boardColumnId,
            actionSheetId,
            decisionId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TaskCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalAssigneeId().map(f -> "assigneeId=" + f + ", ").orElse("") +
            optionalDueDate().map(f -> "dueDate=" + f + ", ").orElse("") +
            optionalOriginKind().map(f -> "originKind=" + f + ", ").orElse("") +
            optionalOriginReference().map(f -> "originReference=" + f + ", ").orElse("") +
            optionalOriginService().map(f -> "originService=" + f + ", ").orElse("") +
            optionalStartedAt().map(f -> "startedAt=" + f + ", ").orElse("") +
            optionalCompletedAt().map(f -> "completedAt=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalReminderLevel().map(f -> "reminderLevel=" + f + ", ").orElse("") +
            optionalClosureProposedAt().map(f -> "closureProposedAt=" + f + ", ").orElse("") +
            optionalBoardId().map(f -> "boardId=" + f + ", ").orElse("") +
            optionalBoardColumnId().map(f -> "boardColumnId=" + f + ", ").orElse("") +
            optionalActionSheetId().map(f -> "actionSheetId=" + f + ", ").orElse("") +
            optionalDecisionId().map(f -> "decisionId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
