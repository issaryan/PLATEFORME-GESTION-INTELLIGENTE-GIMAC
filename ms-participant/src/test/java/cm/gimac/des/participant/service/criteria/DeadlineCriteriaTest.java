package cm.gimac.des.participant.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DeadlineCriteriaTest {

    @Test
    void newDeadlineCriteriaHasAllFiltersNullTest() {
        var deadlineCriteria = new DeadlineCriteria();
        assertThat(deadlineCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void deadlineCriteriaFluentMethodsCreatesFiltersTest() {
        var deadlineCriteria = new DeadlineCriteria();

        setAllFilters(deadlineCriteria);

        assertThat(deadlineCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void deadlineCriteriaCopyCreatesNullFilterTest() {
        var deadlineCriteria = new DeadlineCriteria();
        var copy = deadlineCriteria.copy();

        assertThat(deadlineCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(deadlineCriteria)
        );
    }

    @Test
    void deadlineCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var deadlineCriteria = new DeadlineCriteria();
        setAllFilters(deadlineCriteria);

        var copy = deadlineCriteria.copy();

        assertThat(deadlineCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(deadlineCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var deadlineCriteria = new DeadlineCriteria();

        assertThat(deadlineCriteria).hasToString("DeadlineCriteria{}");
    }

    private static void setAllFilters(DeadlineCriteria deadlineCriteria) {
        deadlineCriteria.id();
        deadlineCriteria.threshold();
        deadlineCriteria.dueOn();
        deadlineCriteria.state();
        deadlineCriteria.triggeredAction();
        deadlineCriteria.triggeredAt();
        deadlineCriteria.acknowledgementRequired();
        deadlineCriteria.notificationId();
        deadlineCriteria.acknowledgedAt();
        deadlineCriteria.institutionId();
        deadlineCriteria.legalDocumentId();
        deadlineCriteria.conventionId();
        deadlineCriteria.distinct();
    }

    private static Condition<DeadlineCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getThreshold()) &&
                condition.apply(criteria.getDueOn()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getTriggeredAction()) &&
                condition.apply(criteria.getTriggeredAt()) &&
                condition.apply(criteria.getAcknowledgementRequired()) &&
                condition.apply(criteria.getNotificationId()) &&
                condition.apply(criteria.getAcknowledgedAt()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getLegalDocumentId()) &&
                condition.apply(criteria.getConventionId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DeadlineCriteria> copyFiltersAre(DeadlineCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getThreshold(), copy.getThreshold()) &&
                condition.apply(criteria.getDueOn(), copy.getDueOn()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getTriggeredAction(), copy.getTriggeredAction()) &&
                condition.apply(criteria.getTriggeredAt(), copy.getTriggeredAt()) &&
                condition.apply(criteria.getAcknowledgementRequired(), copy.getAcknowledgementRequired()) &&
                condition.apply(criteria.getNotificationId(), copy.getNotificationId()) &&
                condition.apply(criteria.getAcknowledgedAt(), copy.getAcknowledgedAt()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getLegalDocumentId(), copy.getLegalDocumentId()) &&
                condition.apply(criteria.getConventionId(), copy.getConventionId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
