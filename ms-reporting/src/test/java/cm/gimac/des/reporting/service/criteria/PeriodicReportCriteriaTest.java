package cm.gimac.des.reporting.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class PeriodicReportCriteriaTest {

    @Test
    void newPeriodicReportCriteriaHasAllFiltersNullTest() {
        var periodicReportCriteria = new PeriodicReportCriteria();
        assertThat(periodicReportCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void periodicReportCriteriaFluentMethodsCreatesFiltersTest() {
        var periodicReportCriteria = new PeriodicReportCriteria();

        setAllFilters(periodicReportCriteria);

        assertThat(periodicReportCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void periodicReportCriteriaCopyCreatesNullFilterTest() {
        var periodicReportCriteria = new PeriodicReportCriteria();
        var copy = periodicReportCriteria.copy();

        assertThat(periodicReportCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(periodicReportCriteria)
        );
    }

    @Test
    void periodicReportCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var periodicReportCriteria = new PeriodicReportCriteria();
        setAllFilters(periodicReportCriteria);

        var copy = periodicReportCriteria.copy();

        assertThat(periodicReportCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(periodicReportCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var periodicReportCriteria = new PeriodicReportCriteria();

        assertThat(periodicReportCriteria).hasToString("PeriodicReportCriteria{}");
    }

    private static void setAllFilters(PeriodicReportCriteria periodicReportCriteria) {
        periodicReportCriteria.id();
        periodicReportCriteria.serviceCode();
        periodicReportCriteria.periodicity();
        periodicReportCriteria.period();
        periodicReportCriteria.reportVersion();
        periodicReportCriteria.manual();
        periodicReportCriteria.contentDigest();
        periodicReportCriteria.generatedAt();
        periodicReportCriteria.signerId();
        periodicReportCriteria.signedAt();
        periodicReportCriteria.documentRef();
        periodicReportCriteria.notifiedAt();
        periodicReportCriteria.state();
        periodicReportCriteria.distinct();
    }

    private static Condition<PeriodicReportCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getPeriodicity()) &&
                condition.apply(criteria.getPeriod()) &&
                condition.apply(criteria.getReportVersion()) &&
                condition.apply(criteria.getManual()) &&
                condition.apply(criteria.getContentDigest()) &&
                condition.apply(criteria.getGeneratedAt()) &&
                condition.apply(criteria.getSignerId()) &&
                condition.apply(criteria.getSignedAt()) &&
                condition.apply(criteria.getDocumentRef()) &&
                condition.apply(criteria.getNotifiedAt()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<PeriodicReportCriteria> copyFiltersAre(
        PeriodicReportCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getPeriodicity(), copy.getPeriodicity()) &&
                condition.apply(criteria.getPeriod(), copy.getPeriod()) &&
                condition.apply(criteria.getReportVersion(), copy.getReportVersion()) &&
                condition.apply(criteria.getManual(), copy.getManual()) &&
                condition.apply(criteria.getContentDigest(), copy.getContentDigest()) &&
                condition.apply(criteria.getGeneratedAt(), copy.getGeneratedAt()) &&
                condition.apply(criteria.getSignerId(), copy.getSignerId()) &&
                condition.apply(criteria.getSignedAt(), copy.getSignedAt()) &&
                condition.apply(criteria.getDocumentRef(), copy.getDocumentRef()) &&
                condition.apply(criteria.getNotifiedAt(), copy.getNotifiedAt()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
