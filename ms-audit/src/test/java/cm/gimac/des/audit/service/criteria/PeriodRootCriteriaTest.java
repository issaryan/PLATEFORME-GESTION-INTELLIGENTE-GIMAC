package cm.gimac.des.audit.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class PeriodRootCriteriaTest {

    @Test
    void newPeriodRootCriteriaHasAllFiltersNullTest() {
        var periodRootCriteria = new PeriodRootCriteria();
        assertThat(periodRootCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void periodRootCriteriaFluentMethodsCreatesFiltersTest() {
        var periodRootCriteria = new PeriodRootCriteria();

        setAllFilters(periodRootCriteria);

        assertThat(periodRootCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void periodRootCriteriaCopyCreatesNullFilterTest() {
        var periodRootCriteria = new PeriodRootCriteria();
        var copy = periodRootCriteria.copy();

        assertThat(periodRootCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(periodRootCriteria)
        );
    }

    @Test
    void periodRootCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var periodRootCriteria = new PeriodRootCriteria();
        setAllFilters(periodRootCriteria);

        var copy = periodRootCriteria.copy();

        assertThat(periodRootCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(periodRootCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var periodRootCriteria = new PeriodRootCriteria();

        assertThat(periodRootCriteria).hasToString("PeriodRootCriteria{}");
    }

    private static void setAllFilters(PeriodRootCriteria periodRootCriteria) {
        periodRootCriteria.id();
        periodRootCriteria.period();
        periodRootCriteria.treeSize();
        periodRootCriteria.rootHash();
        periodRootCriteria.sealedAt();
        periodRootCriteria.state();
        periodRootCriteria.timestampSerialNumber();
        periodRootCriteria.timestampGenTime();
        periodRootCriteria.timestampImprint();
        periodRootCriteria.publicationReference();
        periodRootCriteria.publishedAt();
        periodRootCriteria.previousRootId();
        periodRootCriteria.consistencyWithPreviousId();
        periodRootCriteria.periodRootId();
        periodRootCriteria.distinct();
    }

    private static Condition<PeriodRootCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getPeriod()) &&
                condition.apply(criteria.getTreeSize()) &&
                condition.apply(criteria.getRootHash()) &&
                condition.apply(criteria.getSealedAt()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getTimestampSerialNumber()) &&
                condition.apply(criteria.getTimestampGenTime()) &&
                condition.apply(criteria.getTimestampImprint()) &&
                condition.apply(criteria.getPublicationReference()) &&
                condition.apply(criteria.getPublishedAt()) &&
                condition.apply(criteria.getPreviousRootId()) &&
                condition.apply(criteria.getConsistencyWithPreviousId()) &&
                condition.apply(criteria.getPeriodRootId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<PeriodRootCriteria> copyFiltersAre(PeriodRootCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getPeriod(), copy.getPeriod()) &&
                condition.apply(criteria.getTreeSize(), copy.getTreeSize()) &&
                condition.apply(criteria.getRootHash(), copy.getRootHash()) &&
                condition.apply(criteria.getSealedAt(), copy.getSealedAt()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getTimestampSerialNumber(), copy.getTimestampSerialNumber()) &&
                condition.apply(criteria.getTimestampGenTime(), copy.getTimestampGenTime()) &&
                condition.apply(criteria.getTimestampImprint(), copy.getTimestampImprint()) &&
                condition.apply(criteria.getPublicationReference(), copy.getPublicationReference()) &&
                condition.apply(criteria.getPublishedAt(), copy.getPublishedAt()) &&
                condition.apply(criteria.getPreviousRootId(), copy.getPreviousRootId()) &&
                condition.apply(criteria.getConsistencyWithPreviousId(), copy.getConsistencyWithPreviousId()) &&
                condition.apply(criteria.getPeriodRootId(), copy.getPeriodRootId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
