package cm.gimac.des.homologation.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ReferenceOperationCriteriaTest {

    @Test
    void newReferenceOperationCriteriaHasAllFiltersNullTest() {
        var referenceOperationCriteria = new ReferenceOperationCriteria();
        assertThat(referenceOperationCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void referenceOperationCriteriaFluentMethodsCreatesFiltersTest() {
        var referenceOperationCriteria = new ReferenceOperationCriteria();

        setAllFilters(referenceOperationCriteria);

        assertThat(referenceOperationCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void referenceOperationCriteriaCopyCreatesNullFilterTest() {
        var referenceOperationCriteria = new ReferenceOperationCriteria();
        var copy = referenceOperationCriteria.copy();

        assertThat(referenceOperationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(referenceOperationCriteria)
        );
    }

    @Test
    void referenceOperationCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var referenceOperationCriteria = new ReferenceOperationCriteria();
        setAllFilters(referenceOperationCriteria);

        var copy = referenceOperationCriteria.copy();

        assertThat(referenceOperationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(referenceOperationCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var referenceOperationCriteria = new ReferenceOperationCriteria();

        assertThat(referenceOperationCriteria).hasToString("ReferenceOperationCriteria{}");
    }

    private static void setAllFilters(ReferenceOperationCriteria referenceOperationCriteria) {
        referenceOperationCriteria.id();
        referenceOperationCriteria.code();
        referenceOperationCriteria.label();
        referenceOperationCriteria.branch();
        referenceOperationCriteria.phase();
        referenceOperationCriteria.owner();
        referenceOperationCriteria.expectedEvidence();
        referenceOperationCriteria.serviceCode();
        referenceOperationCriteria.scenarioRef();
        referenceOperationCriteria.criticalPath();
        referenceOperationCriteria.catalogId();
        referenceOperationCriteria.predecessorsId();
        referenceOperationCriteria.successorsId();
        referenceOperationCriteria.distinct();
    }

    private static Condition<ReferenceOperationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getCode()) &&
                condition.apply(criteria.getLabel()) &&
                condition.apply(criteria.getBranch()) &&
                condition.apply(criteria.getPhase()) &&
                condition.apply(criteria.getOwner()) &&
                condition.apply(criteria.getExpectedEvidence()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getScenarioRef()) &&
                condition.apply(criteria.getCriticalPath()) &&
                condition.apply(criteria.getCatalogId()) &&
                condition.apply(criteria.getPredecessorsId()) &&
                condition.apply(criteria.getSuccessorsId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ReferenceOperationCriteria> copyFiltersAre(
        ReferenceOperationCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getCode(), copy.getCode()) &&
                condition.apply(criteria.getLabel(), copy.getLabel()) &&
                condition.apply(criteria.getBranch(), copy.getBranch()) &&
                condition.apply(criteria.getPhase(), copy.getPhase()) &&
                condition.apply(criteria.getOwner(), copy.getOwner()) &&
                condition.apply(criteria.getExpectedEvidence(), copy.getExpectedEvidence()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getScenarioRef(), copy.getScenarioRef()) &&
                condition.apply(criteria.getCriticalPath(), copy.getCriticalPath()) &&
                condition.apply(criteria.getCatalogId(), copy.getCatalogId()) &&
                condition.apply(criteria.getPredecessorsId(), copy.getPredecessorsId()) &&
                condition.apply(criteria.getSuccessorsId(), copy.getSuccessorsId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
