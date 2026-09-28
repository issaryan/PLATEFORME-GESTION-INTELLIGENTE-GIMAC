package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class TaskCriteriaTest {

    @Test
    void newTaskCriteriaHasAllFiltersNullTest() {
        var taskCriteria = new TaskCriteria();
        assertThat(taskCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void taskCriteriaFluentMethodsCreatesFiltersTest() {
        var taskCriteria = new TaskCriteria();

        setAllFilters(taskCriteria);

        assertThat(taskCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void taskCriteriaCopyCreatesNullFilterTest() {
        var taskCriteria = new TaskCriteria();
        var copy = taskCriteria.copy();

        assertThat(taskCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(taskCriteria)
        );
    }

    @Test
    void taskCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var taskCriteria = new TaskCriteria();
        setAllFilters(taskCriteria);

        var copy = taskCriteria.copy();

        assertThat(taskCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(taskCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var taskCriteria = new TaskCriteria();

        assertThat(taskCriteria).hasToString("TaskCriteria{}");
    }

    private static void setAllFilters(TaskCriteria taskCriteria) {
        taskCriteria.id();
        taskCriteria.title();
        taskCriteria.assigneeId();
        taskCriteria.dueDate();
        taskCriteria.originKind();
        taskCriteria.originReference();
        taskCriteria.originService();
        taskCriteria.startedAt();
        taskCriteria.completedAt();
        taskCriteria.state();
        taskCriteria.reminderLevel();
        taskCriteria.closureProposedAt();
        taskCriteria.boardId();
        taskCriteria.boardColumnId();
        taskCriteria.actionSheetId();
        taskCriteria.decisionId();
        taskCriteria.distinct();
    }

    private static Condition<TaskCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getAssigneeId()) &&
                condition.apply(criteria.getDueDate()) &&
                condition.apply(criteria.getOriginKind()) &&
                condition.apply(criteria.getOriginReference()) &&
                condition.apply(criteria.getOriginService()) &&
                condition.apply(criteria.getStartedAt()) &&
                condition.apply(criteria.getCompletedAt()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getReminderLevel()) &&
                condition.apply(criteria.getClosureProposedAt()) &&
                condition.apply(criteria.getBoardId()) &&
                condition.apply(criteria.getBoardColumnId()) &&
                condition.apply(criteria.getActionSheetId()) &&
                condition.apply(criteria.getDecisionId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<TaskCriteria> copyFiltersAre(TaskCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getAssigneeId(), copy.getAssigneeId()) &&
                condition.apply(criteria.getDueDate(), copy.getDueDate()) &&
                condition.apply(criteria.getOriginKind(), copy.getOriginKind()) &&
                condition.apply(criteria.getOriginReference(), copy.getOriginReference()) &&
                condition.apply(criteria.getOriginService(), copy.getOriginService()) &&
                condition.apply(criteria.getStartedAt(), copy.getStartedAt()) &&
                condition.apply(criteria.getCompletedAt(), copy.getCompletedAt()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getReminderLevel(), copy.getReminderLevel()) &&
                condition.apply(criteria.getClosureProposedAt(), copy.getClosureProposedAt()) &&
                condition.apply(criteria.getBoardId(), copy.getBoardId()) &&
                condition.apply(criteria.getBoardColumnId(), copy.getBoardColumnId()) &&
                condition.apply(criteria.getActionSheetId(), copy.getActionSheetId()) &&
                condition.apply(criteria.getDecisionId(), copy.getDecisionId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
