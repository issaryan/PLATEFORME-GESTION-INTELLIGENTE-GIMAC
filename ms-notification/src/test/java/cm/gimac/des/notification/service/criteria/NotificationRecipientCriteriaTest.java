package cm.gimac.des.notification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class NotificationRecipientCriteriaTest {

    @Test
    void newNotificationRecipientCriteriaHasAllFiltersNullTest() {
        var notificationRecipientCriteria = new NotificationRecipientCriteria();
        assertThat(notificationRecipientCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void notificationRecipientCriteriaFluentMethodsCreatesFiltersTest() {
        var notificationRecipientCriteria = new NotificationRecipientCriteria();

        setAllFilters(notificationRecipientCriteria);

        assertThat(notificationRecipientCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void notificationRecipientCriteriaCopyCreatesNullFilterTest() {
        var notificationRecipientCriteria = new NotificationRecipientCriteria();
        var copy = notificationRecipientCriteria.copy();

        assertThat(notificationRecipientCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(notificationRecipientCriteria)
        );
    }

    @Test
    void notificationRecipientCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var notificationRecipientCriteria = new NotificationRecipientCriteria();
        setAllFilters(notificationRecipientCriteria);

        var copy = notificationRecipientCriteria.copy();

        assertThat(notificationRecipientCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(notificationRecipientCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var notificationRecipientCriteria = new NotificationRecipientCriteria();

        assertThat(notificationRecipientCriteria).hasToString("NotificationRecipientCriteria{}");
    }

    private static void setAllFilters(NotificationRecipientCriteria notificationRecipientCriteria) {
        notificationRecipientCriteria.id();
        notificationRecipientCriteria.userId();
        notificationRecipientCriteria.institutionId();
        notificationRecipientCriteria.resolvedBy();
        notificationRecipientCriteria.channels();
        notificationRecipientCriteria.escalated();
        notificationRecipientCriteria.resolvedAt();
        notificationRecipientCriteria.readAt();
        notificationRecipientCriteria.acknowledgedAt();
        notificationRecipientCriteria.notificationId();
        notificationRecipientCriteria.sourceSubscriptionId();
        notificationRecipientCriteria.distinct();
    }

    private static Condition<NotificationRecipientCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getResolvedBy()) &&
                condition.apply(criteria.getChannels()) &&
                condition.apply(criteria.getEscalated()) &&
                condition.apply(criteria.getResolvedAt()) &&
                condition.apply(criteria.getReadAt()) &&
                condition.apply(criteria.getAcknowledgedAt()) &&
                condition.apply(criteria.getNotificationId()) &&
                condition.apply(criteria.getSourceSubscriptionId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<NotificationRecipientCriteria> copyFiltersAre(
        NotificationRecipientCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getResolvedBy(), copy.getResolvedBy()) &&
                condition.apply(criteria.getChannels(), copy.getChannels()) &&
                condition.apply(criteria.getEscalated(), copy.getEscalated()) &&
                condition.apply(criteria.getResolvedAt(), copy.getResolvedAt()) &&
                condition.apply(criteria.getReadAt(), copy.getReadAt()) &&
                condition.apply(criteria.getAcknowledgedAt(), copy.getAcknowledgedAt()) &&
                condition.apply(criteria.getNotificationId(), copy.getNotificationId()) &&
                condition.apply(criteria.getSourceSubscriptionId(), copy.getSourceSubscriptionId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
