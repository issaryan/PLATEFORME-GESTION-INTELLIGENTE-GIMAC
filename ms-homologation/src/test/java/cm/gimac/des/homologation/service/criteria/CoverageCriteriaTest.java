package cm.gimac.des.homologation.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CoverageCriteriaTest {

    @Test
    void newCoverageCriteriaHasAllFiltersNullTest() {
        var coverageCriteria = new CoverageCriteria();
        assertThat(coverageCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void coverageCriteriaFluentMethodsCreatesFiltersTest() {
        var coverageCriteria = new CoverageCriteria();

        setAllFilters(coverageCriteria);

        assertThat(coverageCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void coverageCriteriaCopyCreatesNullFilterTest() {
        var coverageCriteria = new CoverageCriteria();
        var copy = coverageCriteria.copy();

        assertThat(coverageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(coverageCriteria)
        );
    }

    @Test
    void coverageCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var coverageCriteria = new CoverageCriteria();
        setAllFilters(coverageCriteria);

        var copy = coverageCriteria.copy();

        assertThat(coverageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(coverageCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var coverageCriteria = new CoverageCriteria();

        assertThat(coverageCriteria).hasToString("CoverageCriteria{}");
    }

    private static void setAllFilters(CoverageCriteria coverageCriteria) {
        coverageCriteria.id();
        coverageCriteria.serviceCode();
        coverageCriteria.certificationRate();
        coverageCriteria.commissioningRate();
        coverageCriteria.requiredCount();
        coverageCriteria.closedCount();
        coverageCriteria.computedAt();
        coverageCriteria.liaisonId();
        coverageCriteria.distinct();
    }

    private static Condition<CoverageCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getCertificationRate()) &&
                condition.apply(criteria.getCommissioningRate()) &&
                condition.apply(criteria.getRequiredCount()) &&
                condition.apply(criteria.getClosedCount()) &&
                condition.apply(criteria.getComputedAt()) &&
                condition.apply(criteria.getLiaisonId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CoverageCriteria> copyFiltersAre(CoverageCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getCertificationRate(), copy.getCertificationRate()) &&
                condition.apply(criteria.getCommissioningRate(), copy.getCommissioningRate()) &&
                condition.apply(criteria.getRequiredCount(), copy.getRequiredCount()) &&
                condition.apply(criteria.getClosedCount(), copy.getClosedCount()) &&
                condition.apply(criteria.getComputedAt(), copy.getComputedAt()) &&
                condition.apply(criteria.getLiaisonId(), copy.getLiaisonId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
