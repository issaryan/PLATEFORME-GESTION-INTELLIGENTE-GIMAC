package cm.gimac.des.reporting.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class IndicatorDefinitionCriteriaTest {

    @Test
    void newIndicatorDefinitionCriteriaHasAllFiltersNullTest() {
        var indicatorDefinitionCriteria = new IndicatorDefinitionCriteria();
        assertThat(indicatorDefinitionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void indicatorDefinitionCriteriaFluentMethodsCreatesFiltersTest() {
        var indicatorDefinitionCriteria = new IndicatorDefinitionCriteria();

        setAllFilters(indicatorDefinitionCriteria);

        assertThat(indicatorDefinitionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void indicatorDefinitionCriteriaCopyCreatesNullFilterTest() {
        var indicatorDefinitionCriteria = new IndicatorDefinitionCriteria();
        var copy = indicatorDefinitionCriteria.copy();

        assertThat(indicatorDefinitionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(indicatorDefinitionCriteria)
        );
    }

    @Test
    void indicatorDefinitionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var indicatorDefinitionCriteria = new IndicatorDefinitionCriteria();
        setAllFilters(indicatorDefinitionCriteria);

        var copy = indicatorDefinitionCriteria.copy();

        assertThat(indicatorDefinitionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(indicatorDefinitionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var indicatorDefinitionCriteria = new IndicatorDefinitionCriteria();

        assertThat(indicatorDefinitionCriteria).hasToString("IndicatorDefinitionCriteria{}");
    }

    private static void setAllFilters(IndicatorDefinitionCriteria indicatorDefinitionCriteria) {
        indicatorDefinitionCriteria.id();
        indicatorDefinitionCriteria.code();
        indicatorDefinitionCriteria.label();
        indicatorDefinitionCriteria.source();
        indicatorDefinitionCriteria.unit();
        indicatorDefinitionCriteria.window();
        indicatorDefinitionCriteria.segmentCount();
        indicatorDefinitionCriteria.segmentLength();
        indicatorDefinitionCriteria.warningThreshold();
        indicatorDefinitionCriteria.criticalThreshold();
        indicatorDefinitionCriteria.higherIsWorse();
        indicatorDefinitionCriteria.audience();
        indicatorDefinitionCriteria.active();
        indicatorDefinitionCriteria.cockpitsId();
        indicatorDefinitionCriteria.distinct();
    }

    private static Condition<IndicatorDefinitionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getCode()) &&
                condition.apply(criteria.getLabel()) &&
                condition.apply(criteria.getSource()) &&
                condition.apply(criteria.getUnit()) &&
                condition.apply(criteria.getWindow()) &&
                condition.apply(criteria.getSegmentCount()) &&
                condition.apply(criteria.getSegmentLength()) &&
                condition.apply(criteria.getWarningThreshold()) &&
                condition.apply(criteria.getCriticalThreshold()) &&
                condition.apply(criteria.getHigherIsWorse()) &&
                condition.apply(criteria.getAudience()) &&
                condition.apply(criteria.getActive()) &&
                condition.apply(criteria.getCockpitsId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<IndicatorDefinitionCriteria> copyFiltersAre(
        IndicatorDefinitionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getCode(), copy.getCode()) &&
                condition.apply(criteria.getLabel(), copy.getLabel()) &&
                condition.apply(criteria.getSource(), copy.getSource()) &&
                condition.apply(criteria.getUnit(), copy.getUnit()) &&
                condition.apply(criteria.getWindow(), copy.getWindow()) &&
                condition.apply(criteria.getSegmentCount(), copy.getSegmentCount()) &&
                condition.apply(criteria.getSegmentLength(), copy.getSegmentLength()) &&
                condition.apply(criteria.getWarningThreshold(), copy.getWarningThreshold()) &&
                condition.apply(criteria.getCriticalThreshold(), copy.getCriticalThreshold()) &&
                condition.apply(criteria.getHigherIsWorse(), copy.getHigherIsWorse()) &&
                condition.apply(criteria.getAudience(), copy.getAudience()) &&
                condition.apply(criteria.getActive(), copy.getActive()) &&
                condition.apply(criteria.getCockpitsId(), copy.getCockpitsId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
