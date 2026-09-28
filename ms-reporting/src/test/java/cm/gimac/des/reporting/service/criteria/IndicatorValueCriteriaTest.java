package cm.gimac.des.reporting.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class IndicatorValueCriteriaTest {

    @Test
    void newIndicatorValueCriteriaHasAllFiltersNullTest() {
        var indicatorValueCriteria = new IndicatorValueCriteria();
        assertThat(indicatorValueCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void indicatorValueCriteriaFluentMethodsCreatesFiltersTest() {
        var indicatorValueCriteria = new IndicatorValueCriteria();

        setAllFilters(indicatorValueCriteria);

        assertThat(indicatorValueCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void indicatorValueCriteriaCopyCreatesNullFilterTest() {
        var indicatorValueCriteria = new IndicatorValueCriteria();
        var copy = indicatorValueCriteria.copy();

        assertThat(indicatorValueCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(indicatorValueCriteria)
        );
    }

    @Test
    void indicatorValueCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var indicatorValueCriteria = new IndicatorValueCriteria();
        setAllFilters(indicatorValueCriteria);

        var copy = indicatorValueCriteria.copy();

        assertThat(indicatorValueCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(indicatorValueCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var indicatorValueCriteria = new IndicatorValueCriteria();

        assertThat(indicatorValueCriteria).hasToString("IndicatorValueCriteria{}");
    }

    private static void setAllFilters(IndicatorValueCriteria indicatorValueCriteria) {
        indicatorValueCriteria.id();
        indicatorValueCriteria.period();
        indicatorValueCriteria.timeKey();
        indicatorValueCriteria.institutionId();
        indicatorValueCriteria.serviceCode();
        indicatorValueCriteria.severity();
        indicatorValueCriteria.value();
        indicatorValueCriteria.noData();
        indicatorValueCriteria.thresholdState();
        indicatorValueCriteria.previousThresholdState();
        indicatorValueCriteria.trend();
        indicatorValueCriteria.computedAt();
        indicatorValueCriteria.definitionId();
        indicatorValueCriteria.statementId();
        indicatorValueCriteria.distinct();
    }

    private static Condition<IndicatorValueCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getPeriod()) &&
                condition.apply(criteria.getTimeKey()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getSeverity()) &&
                condition.apply(criteria.getValue()) &&
                condition.apply(criteria.getNoData()) &&
                condition.apply(criteria.getThresholdState()) &&
                condition.apply(criteria.getPreviousThresholdState()) &&
                condition.apply(criteria.getTrend()) &&
                condition.apply(criteria.getComputedAt()) &&
                condition.apply(criteria.getDefinitionId()) &&
                condition.apply(criteria.getStatementId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<IndicatorValueCriteria> copyFiltersAre(
        IndicatorValueCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getPeriod(), copy.getPeriod()) &&
                condition.apply(criteria.getTimeKey(), copy.getTimeKey()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getSeverity(), copy.getSeverity()) &&
                condition.apply(criteria.getValue(), copy.getValue()) &&
                condition.apply(criteria.getNoData(), copy.getNoData()) &&
                condition.apply(criteria.getThresholdState(), copy.getThresholdState()) &&
                condition.apply(criteria.getPreviousThresholdState(), copy.getPreviousThresholdState()) &&
                condition.apply(criteria.getTrend(), copy.getTrend()) &&
                condition.apply(criteria.getComputedAt(), copy.getComputedAt()) &&
                condition.apply(criteria.getDefinitionId(), copy.getDefinitionId()) &&
                condition.apply(criteria.getStatementId(), copy.getStatementId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
