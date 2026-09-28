package cm.gimac.des.monitoring.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class MeasureWindowCriteriaTest {

    @Test
    void newMeasureWindowCriteriaHasAllFiltersNullTest() {
        var measureWindowCriteria = new MeasureWindowCriteria();
        assertThat(measureWindowCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void measureWindowCriteriaFluentMethodsCreatesFiltersTest() {
        var measureWindowCriteria = new MeasureWindowCriteria();

        setAllFilters(measureWindowCriteria);

        assertThat(measureWindowCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void measureWindowCriteriaCopyCreatesNullFilterTest() {
        var measureWindowCriteria = new MeasureWindowCriteria();
        var copy = measureWindowCriteria.copy();

        assertThat(measureWindowCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(measureWindowCriteria)
        );
    }

    @Test
    void measureWindowCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var measureWindowCriteria = new MeasureWindowCriteria();
        setAllFilters(measureWindowCriteria);

        var copy = measureWindowCriteria.copy();

        assertThat(measureWindowCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(measureWindowCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var measureWindowCriteria = new MeasureWindowCriteria();

        assertThat(measureWindowCriteria).hasToString("MeasureWindowCriteria{}");
    }

    private static void setAllFilters(MeasureWindowCriteria measureWindowCriteria) {
        measureWindowCriteria.id();
        measureWindowCriteria.windowStart();
        measureWindowCriteria.windowEnd();
        measureWindowCriteria.receivedAt();
        measureWindowCriteria.linkId();
        measureWindowCriteria.distinct();
    }

    private static Condition<MeasureWindowCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getWindowStart()) &&
                condition.apply(criteria.getWindowEnd()) &&
                condition.apply(criteria.getReceivedAt()) &&
                condition.apply(criteria.getLinkId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<MeasureWindowCriteria> copyFiltersAre(
        MeasureWindowCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getWindowStart(), copy.getWindowStart()) &&
                condition.apply(criteria.getWindowEnd(), copy.getWindowEnd()) &&
                condition.apply(criteria.getReceivedAt(), copy.getReceivedAt()) &&
                condition.apply(criteria.getLinkId(), copy.getLinkId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
