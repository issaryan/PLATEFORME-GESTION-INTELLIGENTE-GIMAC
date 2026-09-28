package cm.gimac.des.monitoring.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class OperatorUnavailabilityCriteriaTest {

    @Test
    void newOperatorUnavailabilityCriteriaHasAllFiltersNullTest() {
        var operatorUnavailabilityCriteria = new OperatorUnavailabilityCriteria();
        assertThat(operatorUnavailabilityCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void operatorUnavailabilityCriteriaFluentMethodsCreatesFiltersTest() {
        var operatorUnavailabilityCriteria = new OperatorUnavailabilityCriteria();

        setAllFilters(operatorUnavailabilityCriteria);

        assertThat(operatorUnavailabilityCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void operatorUnavailabilityCriteriaCopyCreatesNullFilterTest() {
        var operatorUnavailabilityCriteria = new OperatorUnavailabilityCriteria();
        var copy = operatorUnavailabilityCriteria.copy();

        assertThat(operatorUnavailabilityCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(operatorUnavailabilityCriteria)
        );
    }

    @Test
    void operatorUnavailabilityCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var operatorUnavailabilityCriteria = new OperatorUnavailabilityCriteria();
        setAllFilters(operatorUnavailabilityCriteria);

        var copy = operatorUnavailabilityCriteria.copy();

        assertThat(operatorUnavailabilityCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(operatorUnavailabilityCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var operatorUnavailabilityCriteria = new OperatorUnavailabilityCriteria();

        assertThat(operatorUnavailabilityCriteria).hasToString("OperatorUnavailabilityCriteria{}");
    }

    private static void setAllFilters(OperatorUnavailabilityCriteria operatorUnavailabilityCriteria) {
        operatorUnavailabilityCriteria.id();
        operatorUnavailabilityCriteria.availabilityId();
        operatorUnavailabilityCriteria.operatorId();
        operatorUnavailabilityCriteria.startsAt();
        operatorUnavailabilityCriteria.endsAt();
        operatorUnavailabilityCriteria.validatedAt();
        operatorUnavailabilityCriteria.cancelled();
        operatorUnavailabilityCriteria.distinct();
    }

    private static Condition<OperatorUnavailabilityCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getAvailabilityId()) &&
                condition.apply(criteria.getOperatorId()) &&
                condition.apply(criteria.getStartsAt()) &&
                condition.apply(criteria.getEndsAt()) &&
                condition.apply(criteria.getValidatedAt()) &&
                condition.apply(criteria.getCancelled()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<OperatorUnavailabilityCriteria> copyFiltersAre(
        OperatorUnavailabilityCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getAvailabilityId(), copy.getAvailabilityId()) &&
                condition.apply(criteria.getOperatorId(), copy.getOperatorId()) &&
                condition.apply(criteria.getStartsAt(), copy.getStartsAt()) &&
                condition.apply(criteria.getEndsAt(), copy.getEndsAt()) &&
                condition.apply(criteria.getValidatedAt(), copy.getValidatedAt()) &&
                condition.apply(criteria.getCancelled(), copy.getCancelled()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
