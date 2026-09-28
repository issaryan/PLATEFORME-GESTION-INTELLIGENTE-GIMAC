package cm.gimac.des.certification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CoverageSnapshotCriteriaTest {

    @Test
    void newCoverageSnapshotCriteriaHasAllFiltersNullTest() {
        var coverageSnapshotCriteria = new CoverageSnapshotCriteria();
        assertThat(coverageSnapshotCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void coverageSnapshotCriteriaFluentMethodsCreatesFiltersTest() {
        var coverageSnapshotCriteria = new CoverageSnapshotCriteria();

        setAllFilters(coverageSnapshotCriteria);

        assertThat(coverageSnapshotCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void coverageSnapshotCriteriaCopyCreatesNullFilterTest() {
        var coverageSnapshotCriteria = new CoverageSnapshotCriteria();
        var copy = coverageSnapshotCriteria.copy();

        assertThat(coverageSnapshotCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(coverageSnapshotCriteria)
        );
    }

    @Test
    void coverageSnapshotCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var coverageSnapshotCriteria = new CoverageSnapshotCriteria();
        setAllFilters(coverageSnapshotCriteria);

        var copy = coverageSnapshotCriteria.copy();

        assertThat(coverageSnapshotCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(coverageSnapshotCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var coverageSnapshotCriteria = new CoverageSnapshotCriteria();

        assertThat(coverageSnapshotCriteria).hasToString("CoverageSnapshotCriteria{}");
    }

    private static void setAllFilters(CoverageSnapshotCriteria coverageSnapshotCriteria) {
        coverageSnapshotCriteria.id();
        coverageSnapshotCriteria.liaisonId();
        coverageSnapshotCriteria.serviceCode();
        coverageSnapshotCriteria.included();
        coverageSnapshotCriteria.certificationRate();
        coverageSnapshotCriteria.commissioningRate();
        coverageSnapshotCriteria.sourceEventId();
        coverageSnapshotCriteria.computedAt();
        coverageSnapshotCriteria.receivedAt();
        coverageSnapshotCriteria.distinct();
    }

    private static Condition<CoverageSnapshotCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getLiaisonId()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getIncluded()) &&
                condition.apply(criteria.getCertificationRate()) &&
                condition.apply(criteria.getCommissioningRate()) &&
                condition.apply(criteria.getSourceEventId()) &&
                condition.apply(criteria.getComputedAt()) &&
                condition.apply(criteria.getReceivedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CoverageSnapshotCriteria> copyFiltersAre(
        CoverageSnapshotCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getLiaisonId(), copy.getLiaisonId()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getIncluded(), copy.getIncluded()) &&
                condition.apply(criteria.getCertificationRate(), copy.getCertificationRate()) &&
                condition.apply(criteria.getCommissioningRate(), copy.getCommissioningRate()) &&
                condition.apply(criteria.getSourceEventId(), copy.getSourceEventId()) &&
                condition.apply(criteria.getComputedAt(), copy.getComputedAt()) &&
                condition.apply(criteria.getReceivedAt(), copy.getReceivedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
