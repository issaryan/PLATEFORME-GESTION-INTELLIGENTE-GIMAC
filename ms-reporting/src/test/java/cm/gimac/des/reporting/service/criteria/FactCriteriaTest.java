package cm.gimac.des.reporting.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class FactCriteriaTest {

    @Test
    void newFactCriteriaHasAllFiltersNullTest() {
        var factCriteria = new FactCriteria();
        assertThat(factCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void factCriteriaFluentMethodsCreatesFiltersTest() {
        var factCriteria = new FactCriteria();

        setAllFilters(factCriteria);

        assertThat(factCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void factCriteriaCopyCreatesNullFilterTest() {
        var factCriteria = new FactCriteria();
        var copy = factCriteria.copy();

        assertThat(factCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(factCriteria)
        );
    }

    @Test
    void factCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var factCriteria = new FactCriteria();
        setAllFilters(factCriteria);

        var copy = factCriteria.copy();

        assertThat(factCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(factCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var factCriteria = new FactCriteria();

        assertThat(factCriteria).hasToString("FactCriteria{}");
    }

    private static void setAllFilters(FactCriteria factCriteria) {
        factCriteria.id();
        factCriteria.domain();
        factCriteria.timeKey();
        factCriteria.institutionId();
        factCriteria.serviceCode();
        factCriteria.severity();
        factCriteria.sourceEventId();
        factCriteria.recordedAt();
        factCriteria.superseded();
        factCriteria.compacted();
        factCriteria.statementId();
        factCriteria.distinct();
    }

    private static Condition<FactCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getDomain()) &&
                condition.apply(criteria.getTimeKey()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getSeverity()) &&
                condition.apply(criteria.getSourceEventId()) &&
                condition.apply(criteria.getRecordedAt()) &&
                condition.apply(criteria.getSuperseded()) &&
                condition.apply(criteria.getCompacted()) &&
                condition.apply(criteria.getStatementId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<FactCriteria> copyFiltersAre(FactCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getDomain(), copy.getDomain()) &&
                condition.apply(criteria.getTimeKey(), copy.getTimeKey()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getSeverity(), copy.getSeverity()) &&
                condition.apply(criteria.getSourceEventId(), copy.getSourceEventId()) &&
                condition.apply(criteria.getRecordedAt(), copy.getRecordedAt()) &&
                condition.apply(criteria.getSuperseded(), copy.getSuperseded()) &&
                condition.apply(criteria.getCompacted(), copy.getCompacted()) &&
                condition.apply(criteria.getStatementId(), copy.getStatementId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
