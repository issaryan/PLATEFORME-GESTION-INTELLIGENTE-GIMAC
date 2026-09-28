package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class RiskCriteriaTest {

    @Test
    void newRiskCriteriaHasAllFiltersNullTest() {
        var riskCriteria = new RiskCriteria();
        assertThat(riskCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void riskCriteriaFluentMethodsCreatesFiltersTest() {
        var riskCriteria = new RiskCriteria();

        setAllFilters(riskCriteria);

        assertThat(riskCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void riskCriteriaCopyCreatesNullFilterTest() {
        var riskCriteria = new RiskCriteria();
        var copy = riskCriteria.copy();

        assertThat(riskCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(riskCriteria)
        );
    }

    @Test
    void riskCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var riskCriteria = new RiskCriteria();
        setAllFilters(riskCriteria);

        var copy = riskCriteria.copy();

        assertThat(riskCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(riskCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var riskCriteria = new RiskCriteria();

        assertThat(riskCriteria).hasToString("RiskCriteria{}");
    }

    private static void setAllFilters(RiskCriteria riskCriteria) {
        riskCriteria.id();
        riskCriteria.scope();
        riskCriteria.description();
        riskCriteria.probability();
        riskCriteria.impact();
        riskCriteria.controlCoefficient();
        riskCriteria.grossScore();
        riskCriteria.residualScore();
        riskCriteria.zone();
        riskCriteria.ownerId();
        riskCriteria.mitigationPlan();
        riskCriteria.reviewedAt();
        riskCriteria.projectId();
        riskCriteria.distinct();
    }

    private static Condition<RiskCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getScope()) &&
                condition.apply(criteria.getDescription()) &&
                condition.apply(criteria.getProbability()) &&
                condition.apply(criteria.getImpact()) &&
                condition.apply(criteria.getControlCoefficient()) &&
                condition.apply(criteria.getGrossScore()) &&
                condition.apply(criteria.getResidualScore()) &&
                condition.apply(criteria.getZone()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getMitigationPlan()) &&
                condition.apply(criteria.getReviewedAt()) &&
                condition.apply(criteria.getProjectId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<RiskCriteria> copyFiltersAre(RiskCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getScope(), copy.getScope()) &&
                condition.apply(criteria.getDescription(), copy.getDescription()) &&
                condition.apply(criteria.getProbability(), copy.getProbability()) &&
                condition.apply(criteria.getImpact(), copy.getImpact()) &&
                condition.apply(criteria.getControlCoefficient(), copy.getControlCoefficient()) &&
                condition.apply(criteria.getGrossScore(), copy.getGrossScore()) &&
                condition.apply(criteria.getResidualScore(), copy.getResidualScore()) &&
                condition.apply(criteria.getZone(), copy.getZone()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getMitigationPlan(), copy.getMitigationPlan()) &&
                condition.apply(criteria.getReviewedAt(), copy.getReviewedAt()) &&
                condition.apply(criteria.getProjectId(), copy.getProjectId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
