package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class AcceptanceCriterionCriteriaTest {

    @Test
    void newAcceptanceCriterionCriteriaHasAllFiltersNullTest() {
        var acceptanceCriterionCriteria = new AcceptanceCriterionCriteria();
        assertThat(acceptanceCriterionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void acceptanceCriterionCriteriaFluentMethodsCreatesFiltersTest() {
        var acceptanceCriterionCriteria = new AcceptanceCriterionCriteria();

        setAllFilters(acceptanceCriterionCriteria);

        assertThat(acceptanceCriterionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void acceptanceCriterionCriteriaCopyCreatesNullFilterTest() {
        var acceptanceCriterionCriteria = new AcceptanceCriterionCriteria();
        var copy = acceptanceCriterionCriteria.copy();

        assertThat(acceptanceCriterionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(acceptanceCriterionCriteria)
        );
    }

    @Test
    void acceptanceCriterionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var acceptanceCriterionCriteria = new AcceptanceCriterionCriteria();
        setAllFilters(acceptanceCriterionCriteria);

        var copy = acceptanceCriterionCriteria.copy();

        assertThat(acceptanceCriterionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(acceptanceCriterionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var acceptanceCriterionCriteria = new AcceptanceCriterionCriteria();

        assertThat(acceptanceCriterionCriteria).hasToString("AcceptanceCriterionCriteria{}");
    }

    private static void setAllFilters(AcceptanceCriterionCriteria acceptanceCriterionCriteria) {
        acceptanceCriterionCriteria.id();
        acceptanceCriterionCriteria.label();
        acceptanceCriterionCriteria.satisfied();
        acceptanceCriterionCriteria.evidenceRequired();
        acceptanceCriterionCriteria.evidenceRef();
        acceptanceCriterionCriteria.satisfiedAt();
        acceptanceCriterionCriteria.taskId();
        acceptanceCriterionCriteria.distinct();
    }

    private static Condition<AcceptanceCriterionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getLabel()) &&
                condition.apply(criteria.getSatisfied()) &&
                condition.apply(criteria.getEvidenceRequired()) &&
                condition.apply(criteria.getEvidenceRef()) &&
                condition.apply(criteria.getSatisfiedAt()) &&
                condition.apply(criteria.getTaskId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<AcceptanceCriterionCriteria> copyFiltersAre(
        AcceptanceCriterionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getLabel(), copy.getLabel()) &&
                condition.apply(criteria.getSatisfied(), copy.getSatisfied()) &&
                condition.apply(criteria.getEvidenceRequired(), copy.getEvidenceRequired()) &&
                condition.apply(criteria.getEvidenceRef(), copy.getEvidenceRef()) &&
                condition.apply(criteria.getSatisfiedAt(), copy.getSatisfiedAt()) &&
                condition.apply(criteria.getTaskId(), copy.getTaskId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
