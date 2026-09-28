package cm.gimac.des.monitoring.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ShiftCriteriaTest {

    @Test
    void newShiftCriteriaHasAllFiltersNullTest() {
        var shiftCriteria = new ShiftCriteria();
        assertThat(shiftCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void shiftCriteriaFluentMethodsCreatesFiltersTest() {
        var shiftCriteria = new ShiftCriteria();

        setAllFilters(shiftCriteria);

        assertThat(shiftCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void shiftCriteriaCopyCreatesNullFilterTest() {
        var shiftCriteria = new ShiftCriteria();
        var copy = shiftCriteria.copy();

        assertThat(shiftCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(shiftCriteria)
        );
    }

    @Test
    void shiftCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var shiftCriteria = new ShiftCriteria();
        setAllFilters(shiftCriteria);

        var copy = shiftCriteria.copy();

        assertThat(shiftCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(shiftCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var shiftCriteria = new ShiftCriteria();

        assertThat(shiftCriteria).hasToString("ShiftCriteria{}");
    }

    private static void setAllFilters(ShiftCriteria shiftCriteria) {
        shiftCriteria.id();
        shiftCriteria.reference();
        shiftCriteria.shiftDate();
        shiftCriteria.slot();
        shiftCriteria.operatorIds();
        shiftCriteria.shiftLeadId();
        shiftCriteria.startsAt();
        shiftCriteria.endsAt();
        shiftCriteria.rosterId();
        shiftCriteria.shiftReportId();
        shiftCriteria.distinct();
    }

    private static Condition<ShiftCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getShiftDate()) &&
                condition.apply(criteria.getSlot()) &&
                condition.apply(criteria.getOperatorIds()) &&
                condition.apply(criteria.getShiftLeadId()) &&
                condition.apply(criteria.getStartsAt()) &&
                condition.apply(criteria.getEndsAt()) &&
                condition.apply(criteria.getRosterId()) &&
                condition.apply(criteria.getShiftReportId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ShiftCriteria> copyFiltersAre(ShiftCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getShiftDate(), copy.getShiftDate()) &&
                condition.apply(criteria.getSlot(), copy.getSlot()) &&
                condition.apply(criteria.getOperatorIds(), copy.getOperatorIds()) &&
                condition.apply(criteria.getShiftLeadId(), copy.getShiftLeadId()) &&
                condition.apply(criteria.getStartsAt(), copy.getStartsAt()) &&
                condition.apply(criteria.getEndsAt(), copy.getEndsAt()) &&
                condition.apply(criteria.getRosterId(), copy.getRosterId()) &&
                condition.apply(criteria.getShiftReportId(), copy.getShiftReportId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
