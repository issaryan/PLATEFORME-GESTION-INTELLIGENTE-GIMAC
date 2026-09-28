package cm.gimac.des.notification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class NotificationSubscriptionCriteriaTest {

    @Test
    void newNotificationSubscriptionCriteriaHasAllFiltersNullTest() {
        var notificationSubscriptionCriteria = new NotificationSubscriptionCriteria();
        assertThat(notificationSubscriptionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void notificationSubscriptionCriteriaFluentMethodsCreatesFiltersTest() {
        var notificationSubscriptionCriteria = new NotificationSubscriptionCriteria();

        setAllFilters(notificationSubscriptionCriteria);

        assertThat(notificationSubscriptionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void notificationSubscriptionCriteriaCopyCreatesNullFilterTest() {
        var notificationSubscriptionCriteria = new NotificationSubscriptionCriteria();
        var copy = notificationSubscriptionCriteria.copy();

        assertThat(notificationSubscriptionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(notificationSubscriptionCriteria)
        );
    }

    @Test
    void notificationSubscriptionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var notificationSubscriptionCriteria = new NotificationSubscriptionCriteria();
        setAllFilters(notificationSubscriptionCriteria);

        var copy = notificationSubscriptionCriteria.copy();

        assertThat(notificationSubscriptionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(notificationSubscriptionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var notificationSubscriptionCriteria = new NotificationSubscriptionCriteria();

        assertThat(notificationSubscriptionCriteria).hasToString("NotificationSubscriptionCriteria{}");
    }

    private static void setAllFilters(NotificationSubscriptionCriteria notificationSubscriptionCriteria) {
        notificationSubscriptionCriteria.id();
        notificationSubscriptionCriteria.notificationType();
        notificationSubscriptionCriteria.ruleType();
        notificationSubscriptionCriteria.ruleValue();
        notificationSubscriptionCriteria.minimumUrgency();
        notificationSubscriptionCriteria.onDutyOnly();
        notificationSubscriptionCriteria.active();
        notificationSubscriptionCriteria.ruleVersion();
        notificationSubscriptionCriteria.updatedBy();
        notificationSubscriptionCriteria.updatedAt();
        notificationSubscriptionCriteria.distinct();
    }

    private static Condition<NotificationSubscriptionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getNotificationType()) &&
                condition.apply(criteria.getRuleType()) &&
                condition.apply(criteria.getRuleValue()) &&
                condition.apply(criteria.getMinimumUrgency()) &&
                condition.apply(criteria.getOnDutyOnly()) &&
                condition.apply(criteria.getActive()) &&
                condition.apply(criteria.getRuleVersion()) &&
                condition.apply(criteria.getUpdatedBy()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<NotificationSubscriptionCriteria> copyFiltersAre(
        NotificationSubscriptionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getNotificationType(), copy.getNotificationType()) &&
                condition.apply(criteria.getRuleType(), copy.getRuleType()) &&
                condition.apply(criteria.getRuleValue(), copy.getRuleValue()) &&
                condition.apply(criteria.getMinimumUrgency(), copy.getMinimumUrgency()) &&
                condition.apply(criteria.getOnDutyOnly(), copy.getOnDutyOnly()) &&
                condition.apply(criteria.getActive(), copy.getActive()) &&
                condition.apply(criteria.getRuleVersion(), copy.getRuleVersion()) &&
                condition.apply(criteria.getUpdatedBy(), copy.getUpdatedBy()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
