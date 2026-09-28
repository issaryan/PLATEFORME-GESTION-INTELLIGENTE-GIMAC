package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DecisionCriteriaTest {

    @Test
    void newDecisionCriteriaHasAllFiltersNullTest() {
        var decisionCriteria = new DecisionCriteria();
        assertThat(decisionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void decisionCriteriaFluentMethodsCreatesFiltersTest() {
        var decisionCriteria = new DecisionCriteria();

        setAllFilters(decisionCriteria);

        assertThat(decisionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void decisionCriteriaCopyCreatesNullFilterTest() {
        var decisionCriteria = new DecisionCriteria();
        var copy = decisionCriteria.copy();

        assertThat(decisionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(decisionCriteria)
        );
    }

    @Test
    void decisionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var decisionCriteria = new DecisionCriteria();
        setAllFilters(decisionCriteria);

        var copy = decisionCriteria.copy();

        assertThat(decisionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(decisionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var decisionCriteria = new DecisionCriteria();

        assertThat(decisionCriteria).hasToString("DecisionCriteria{}");
    }

    private static void setAllFilters(DecisionCriteria decisionCriteria) {
        decisionCriteria.id();
        decisionCriteria.text();
        decisionCriteria.ownerId();
        decisionCriteria.dueDate();
        decisionCriteria.state();
        decisionCriteria.returnReason();
        decisionCriteria.convertedAt();
        decisionCriteria.taskId();
        decisionCriteria.meetingId();
        decisionCriteria.rectifiesId();
        decisionCriteria.distinct();
    }

    private static Condition<DecisionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getText()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getDueDate()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getReturnReason()) &&
                condition.apply(criteria.getConvertedAt()) &&
                condition.apply(criteria.getTaskId()) &&
                condition.apply(criteria.getMeetingId()) &&
                condition.apply(criteria.getRectifiesId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DecisionCriteria> copyFiltersAre(DecisionCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getText(), copy.getText()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getDueDate(), copy.getDueDate()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getReturnReason(), copy.getReturnReason()) &&
                condition.apply(criteria.getConvertedAt(), copy.getConvertedAt()) &&
                condition.apply(criteria.getTaskId(), copy.getTaskId()) &&
                condition.apply(criteria.getMeetingId(), copy.getMeetingId()) &&
                condition.apply(criteria.getRectifiesId(), copy.getRectifiesId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
