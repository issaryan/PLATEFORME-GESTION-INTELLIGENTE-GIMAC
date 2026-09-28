package cm.gimac.des.incident.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class EscalationCriteriaTest {

    @Test
    void newEscalationCriteriaHasAllFiltersNullTest() {
        var escalationCriteria = new EscalationCriteria();
        assertThat(escalationCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void escalationCriteriaFluentMethodsCreatesFiltersTest() {
        var escalationCriteria = new EscalationCriteria();

        setAllFilters(escalationCriteria);

        assertThat(escalationCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void escalationCriteriaCopyCreatesNullFilterTest() {
        var escalationCriteria = new EscalationCriteria();
        var copy = escalationCriteria.copy();

        assertThat(escalationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(escalationCriteria)
        );
    }

    @Test
    void escalationCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var escalationCriteria = new EscalationCriteria();
        setAllFilters(escalationCriteria);

        var copy = escalationCriteria.copy();

        assertThat(escalationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(escalationCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var escalationCriteria = new EscalationCriteria();

        assertThat(escalationCriteria).hasToString("EscalationCriteria{}");
    }

    private static void setAllFilters(EscalationCriteria escalationCriteria) {
        escalationCriteria.id();
        escalationCriteria.scope();
        escalationCriteria.level();
        escalationCriteria.targetUserId();
        escalationCriteria.targetContactId();
        escalationCriteria.triggeredAt();
        escalationCriteria.reason();
        escalationCriteria.imputableDelay();
        escalationCriteria.ticketId();
        escalationCriteria.distinct();
    }

    private static Condition<EscalationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getScope()) &&
                condition.apply(criteria.getLevel()) &&
                condition.apply(criteria.getTargetUserId()) &&
                condition.apply(criteria.getTargetContactId()) &&
                condition.apply(criteria.getTriggeredAt()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getImputableDelay()) &&
                condition.apply(criteria.getTicketId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<EscalationCriteria> copyFiltersAre(EscalationCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getScope(), copy.getScope()) &&
                condition.apply(criteria.getLevel(), copy.getLevel()) &&
                condition.apply(criteria.getTargetUserId(), copy.getTargetUserId()) &&
                condition.apply(criteria.getTargetContactId(), copy.getTargetContactId()) &&
                condition.apply(criteria.getTriggeredAt(), copy.getTriggeredAt()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getImputableDelay(), copy.getImputableDelay()) &&
                condition.apply(criteria.getTicketId(), copy.getTicketId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
