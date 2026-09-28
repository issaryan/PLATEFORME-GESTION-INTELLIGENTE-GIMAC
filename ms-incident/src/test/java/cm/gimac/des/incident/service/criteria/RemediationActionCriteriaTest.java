package cm.gimac.des.incident.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class RemediationActionCriteriaTest {

    @Test
    void newRemediationActionCriteriaHasAllFiltersNullTest() {
        var remediationActionCriteria = new RemediationActionCriteria();
        assertThat(remediationActionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void remediationActionCriteriaFluentMethodsCreatesFiltersTest() {
        var remediationActionCriteria = new RemediationActionCriteria();

        setAllFilters(remediationActionCriteria);

        assertThat(remediationActionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void remediationActionCriteriaCopyCreatesNullFilterTest() {
        var remediationActionCriteria = new RemediationActionCriteria();
        var copy = remediationActionCriteria.copy();

        assertThat(remediationActionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(remediationActionCriteria)
        );
    }

    @Test
    void remediationActionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var remediationActionCriteria = new RemediationActionCriteria();
        setAllFilters(remediationActionCriteria);

        var copy = remediationActionCriteria.copy();

        assertThat(remediationActionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(remediationActionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var remediationActionCriteria = new RemediationActionCriteria();

        assertThat(remediationActionCriteria).hasToString("RemediationActionCriteria{}");
    }

    private static void setAllFilters(RemediationActionCriteria remediationActionCriteria) {
        remediationActionCriteria.id();
        remediationActionCriteria.kind();
        remediationActionCriteria.description();
        remediationActionCriteria.ownerId();
        remediationActionCriteria.dueDate();
        remediationActionCriteria.requestedAt();
        remediationActionCriteria.taskRef();
        remediationActionCriteria.state();
        remediationActionCriteria.completedAt();
        remediationActionCriteria.incidentId();
        remediationActionCriteria.crisisDecisionId();
        remediationActionCriteria.distinct();
    }

    private static Condition<RemediationActionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getKind()) &&
                condition.apply(criteria.getDescription()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getDueDate()) &&
                condition.apply(criteria.getRequestedAt()) &&
                condition.apply(criteria.getTaskRef()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getCompletedAt()) &&
                condition.apply(criteria.getIncidentId()) &&
                condition.apply(criteria.getCrisisDecisionId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<RemediationActionCriteria> copyFiltersAre(
        RemediationActionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getKind(), copy.getKind()) &&
                condition.apply(criteria.getDescription(), copy.getDescription()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getDueDate(), copy.getDueDate()) &&
                condition.apply(criteria.getRequestedAt(), copy.getRequestedAt()) &&
                condition.apply(criteria.getTaskRef(), copy.getTaskRef()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getCompletedAt(), copy.getCompletedAt()) &&
                condition.apply(criteria.getIncidentId(), copy.getIncidentId()) &&
                condition.apply(criteria.getCrisisDecisionId(), copy.getCrisisDecisionId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
