package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class RiskAssessmentCriteriaTest {

    @Test
    void newRiskAssessmentCriteriaHasAllFiltersNullTest() {
        var riskAssessmentCriteria = new RiskAssessmentCriteria();
        assertThat(riskAssessmentCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void riskAssessmentCriteriaFluentMethodsCreatesFiltersTest() {
        var riskAssessmentCriteria = new RiskAssessmentCriteria();

        setAllFilters(riskAssessmentCriteria);

        assertThat(riskAssessmentCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void riskAssessmentCriteriaCopyCreatesNullFilterTest() {
        var riskAssessmentCriteria = new RiskAssessmentCriteria();
        var copy = riskAssessmentCriteria.copy();

        assertThat(riskAssessmentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(riskAssessmentCriteria)
        );
    }

    @Test
    void riskAssessmentCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var riskAssessmentCriteria = new RiskAssessmentCriteria();
        setAllFilters(riskAssessmentCriteria);

        var copy = riskAssessmentCriteria.copy();

        assertThat(riskAssessmentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(riskAssessmentCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var riskAssessmentCriteria = new RiskAssessmentCriteria();

        assertThat(riskAssessmentCriteria).hasToString("RiskAssessmentCriteria{}");
    }

    private static void setAllFilters(RiskAssessmentCriteria riskAssessmentCriteria) {
        riskAssessmentCriteria.id();
        riskAssessmentCriteria.probability();
        riskAssessmentCriteria.impact();
        riskAssessmentCriteria.controlCoefficient();
        riskAssessmentCriteria.grossScore();
        riskAssessmentCriteria.residualScore();
        riskAssessmentCriteria.previousZone();
        riskAssessmentCriteria.zone();
        riskAssessmentCriteria.assessorId();
        riskAssessmentCriteria.assessedAt();
        riskAssessmentCriteria.riskId();
        riskAssessmentCriteria.distinct();
    }

    private static Condition<RiskAssessmentCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getProbability()) &&
                condition.apply(criteria.getImpact()) &&
                condition.apply(criteria.getControlCoefficient()) &&
                condition.apply(criteria.getGrossScore()) &&
                condition.apply(criteria.getResidualScore()) &&
                condition.apply(criteria.getPreviousZone()) &&
                condition.apply(criteria.getZone()) &&
                condition.apply(criteria.getAssessorId()) &&
                condition.apply(criteria.getAssessedAt()) &&
                condition.apply(criteria.getRiskId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<RiskAssessmentCriteria> copyFiltersAre(
        RiskAssessmentCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getProbability(), copy.getProbability()) &&
                condition.apply(criteria.getImpact(), copy.getImpact()) &&
                condition.apply(criteria.getControlCoefficient(), copy.getControlCoefficient()) &&
                condition.apply(criteria.getGrossScore(), copy.getGrossScore()) &&
                condition.apply(criteria.getResidualScore(), copy.getResidualScore()) &&
                condition.apply(criteria.getPreviousZone(), copy.getPreviousZone()) &&
                condition.apply(criteria.getZone(), copy.getZone()) &&
                condition.apply(criteria.getAssessorId(), copy.getAssessorId()) &&
                condition.apply(criteria.getAssessedAt(), copy.getAssessedAt()) &&
                condition.apply(criteria.getRiskId(), copy.getRiskId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
