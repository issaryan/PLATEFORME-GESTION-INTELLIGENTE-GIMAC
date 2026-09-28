package cm.gimac.des.monitoring.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class SupervisedLinkCriteriaTest {

    @Test
    void newSupervisedLinkCriteriaHasAllFiltersNullTest() {
        var supervisedLinkCriteria = new SupervisedLinkCriteria();
        assertThat(supervisedLinkCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void supervisedLinkCriteriaFluentMethodsCreatesFiltersTest() {
        var supervisedLinkCriteria = new SupervisedLinkCriteria();

        setAllFilters(supervisedLinkCriteria);

        assertThat(supervisedLinkCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void supervisedLinkCriteriaCopyCreatesNullFilterTest() {
        var supervisedLinkCriteria = new SupervisedLinkCriteria();
        var copy = supervisedLinkCriteria.copy();

        assertThat(supervisedLinkCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(supervisedLinkCriteria)
        );
    }

    @Test
    void supervisedLinkCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var supervisedLinkCriteria = new SupervisedLinkCriteria();
        setAllFilters(supervisedLinkCriteria);

        var copy = supervisedLinkCriteria.copy();

        assertThat(supervisedLinkCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(supervisedLinkCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var supervisedLinkCriteria = new SupervisedLinkCriteria();

        assertThat(supervisedLinkCriteria).hasToString("SupervisedLinkCriteria{}");
    }

    private static void setAllFilters(SupervisedLinkCriteria supervisedLinkCriteria) {
        supervisedLinkCriteria.id();
        supervisedLinkCriteria.code();
        supervisedLinkCriteria.label();
        supervisedLinkCriteria.scope();
        supervisedLinkCriteria.institutionId();
        supervisedLinkCriteria.type();
        supervisedLinkCriteria.state();
        supervisedLinkCriteria.stateSince();
        supervisedLinkCriteria.lastMeasureAt();
        supervisedLinkCriteria.measurePeriod();
        supervisedLinkCriteria.maintenanceFrom();
        supervisedLinkCriteria.maintenanceUntil();
        supervisedLinkCriteria.parentPlatformId();
        supervisedLinkCriteria.distinct();
    }

    private static Condition<SupervisedLinkCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getCode()) &&
                condition.apply(criteria.getLabel()) &&
                condition.apply(criteria.getScope()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getStateSince()) &&
                condition.apply(criteria.getLastMeasureAt()) &&
                condition.apply(criteria.getMeasurePeriod()) &&
                condition.apply(criteria.getMaintenanceFrom()) &&
                condition.apply(criteria.getMaintenanceUntil()) &&
                condition.apply(criteria.getParentPlatformId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<SupervisedLinkCriteria> copyFiltersAre(
        SupervisedLinkCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getCode(), copy.getCode()) &&
                condition.apply(criteria.getLabel(), copy.getLabel()) &&
                condition.apply(criteria.getScope(), copy.getScope()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getStateSince(), copy.getStateSince()) &&
                condition.apply(criteria.getLastMeasureAt(), copy.getLastMeasureAt()) &&
                condition.apply(criteria.getMeasurePeriod(), copy.getMeasurePeriod()) &&
                condition.apply(criteria.getMaintenanceFrom(), copy.getMaintenanceFrom()) &&
                condition.apply(criteria.getMaintenanceUntil(), copy.getMaintenanceUntil()) &&
                condition.apply(criteria.getParentPlatformId(), copy.getParentPlatformId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
