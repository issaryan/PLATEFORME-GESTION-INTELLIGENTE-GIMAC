package cm.gimac.des.monitoring.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class OnCallAssignmentCriteriaTest {

    @Test
    void newOnCallAssignmentCriteriaHasAllFiltersNullTest() {
        var onCallAssignmentCriteria = new OnCallAssignmentCriteria();
        assertThat(onCallAssignmentCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void onCallAssignmentCriteriaFluentMethodsCreatesFiltersTest() {
        var onCallAssignmentCriteria = new OnCallAssignmentCriteria();

        setAllFilters(onCallAssignmentCriteria);

        assertThat(onCallAssignmentCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void onCallAssignmentCriteriaCopyCreatesNullFilterTest() {
        var onCallAssignmentCriteria = new OnCallAssignmentCriteria();
        var copy = onCallAssignmentCriteria.copy();

        assertThat(onCallAssignmentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(onCallAssignmentCriteria)
        );
    }

    @Test
    void onCallAssignmentCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var onCallAssignmentCriteria = new OnCallAssignmentCriteria();
        setAllFilters(onCallAssignmentCriteria);

        var copy = onCallAssignmentCriteria.copy();

        assertThat(onCallAssignmentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(onCallAssignmentCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var onCallAssignmentCriteria = new OnCallAssignmentCriteria();

        assertThat(onCallAssignmentCriteria).hasToString("OnCallAssignmentCriteria{}");
    }

    private static void setAllFilters(OnCallAssignmentCriteria onCallAssignmentCriteria) {
        onCallAssignmentCriteria.id();
        onCallAssignmentCriteria.dutyDate();
        onCallAssignmentCriteria.slot();
        onCallAssignmentCriteria.level();
        onCallAssignmentCriteria.operatorId();
        onCallAssignmentCriteria.shiftLead();
        onCallAssignmentCriteria.rosterId();
        onCallAssignmentCriteria.distinct();
    }

    private static Condition<OnCallAssignmentCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getDutyDate()) &&
                condition.apply(criteria.getSlot()) &&
                condition.apply(criteria.getLevel()) &&
                condition.apply(criteria.getOperatorId()) &&
                condition.apply(criteria.getShiftLead()) &&
                condition.apply(criteria.getRosterId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<OnCallAssignmentCriteria> copyFiltersAre(
        OnCallAssignmentCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getDutyDate(), copy.getDutyDate()) &&
                condition.apply(criteria.getSlot(), copy.getSlot()) &&
                condition.apply(criteria.getLevel(), copy.getLevel()) &&
                condition.apply(criteria.getOperatorId(), copy.getOperatorId()) &&
                condition.apply(criteria.getShiftLead(), copy.getShiftLead()) &&
                condition.apply(criteria.getRosterId(), copy.getRosterId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
