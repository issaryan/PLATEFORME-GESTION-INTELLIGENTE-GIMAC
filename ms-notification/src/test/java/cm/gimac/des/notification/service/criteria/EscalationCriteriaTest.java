package cm.gimac.des.notification.service.criteria;

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
        escalationCriteria.fromUserId();
        escalationCriteria.toUserId();
        escalationCriteria.level();
        escalationCriteria.source();
        escalationCriteria.triggeredAt();
        escalationCriteria.reason();
        escalationCriteria.notificationId();
        escalationCriteria.distinct();
    }

    private static Condition<EscalationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getFromUserId()) &&
                condition.apply(criteria.getToUserId()) &&
                condition.apply(criteria.getLevel()) &&
                condition.apply(criteria.getSource()) &&
                condition.apply(criteria.getTriggeredAt()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getNotificationId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<EscalationCriteria> copyFiltersAre(EscalationCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getFromUserId(), copy.getFromUserId()) &&
                condition.apply(criteria.getToUserId(), copy.getToUserId()) &&
                condition.apply(criteria.getLevel(), copy.getLevel()) &&
                condition.apply(criteria.getSource(), copy.getSource()) &&
                condition.apply(criteria.getTriggeredAt(), copy.getTriggeredAt()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getNotificationId(), copy.getNotificationId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
