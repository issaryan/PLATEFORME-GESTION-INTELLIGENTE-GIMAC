package cm.gimac.des.notification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class NotificationCriteriaTest {

    @Test
    void newNotificationCriteriaHasAllFiltersNullTest() {
        var notificationCriteria = new NotificationCriteria();
        assertThat(notificationCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void notificationCriteriaFluentMethodsCreatesFiltersTest() {
        var notificationCriteria = new NotificationCriteria();

        setAllFilters(notificationCriteria);

        assertThat(notificationCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void notificationCriteriaCopyCreatesNullFilterTest() {
        var notificationCriteria = new NotificationCriteria();
        var copy = notificationCriteria.copy();

        assertThat(notificationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(notificationCriteria)
        );
    }

    @Test
    void notificationCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var notificationCriteria = new NotificationCriteria();
        setAllFilters(notificationCriteria);

        var copy = notificationCriteria.copy();

        assertThat(notificationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(notificationCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var notificationCriteria = new NotificationCriteria();

        assertThat(notificationCriteria).hasToString("NotificationCriteria{}");
    }

    private static void setAllFilters(NotificationCriteria notificationCriteria) {
        notificationCriteria.id();
        notificationCriteria.notificationType();
        notificationCriteria.urgency();
        notificationCriteria.subject();
        notificationCriteria.objectType();
        notificationCriteria.objectId();
        notificationCriteria.institutionId();
        notificationCriteria.sourceEventId();
        notificationCriteria.sourceService();
        notificationCriteria.urgentPort();
        notificationCriteria.correlationId();
        notificationCriteria.groupKey();
        notificationCriteria.requestedAt();
        notificationCriteria.ackDeadline();
        notificationCriteria.escalationLevel();
        notificationCriteria.state();
        notificationCriteria.acknowledgedBy();
        notificationCriteria.acknowledgedAt();
        notificationCriteria.summary();
        notificationCriteria.occurrenceCount();
        notificationCriteria.consolidationWindowId();
        notificationCriteria.summarizedWindowId();
        notificationCriteria.distinct();
    }

    private static Condition<NotificationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getNotificationType()) &&
                condition.apply(criteria.getUrgency()) &&
                condition.apply(criteria.getSubject()) &&
                condition.apply(criteria.getObjectType()) &&
                condition.apply(criteria.getObjectId()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getSourceEventId()) &&
                condition.apply(criteria.getSourceService()) &&
                condition.apply(criteria.getUrgentPort()) &&
                condition.apply(criteria.getCorrelationId()) &&
                condition.apply(criteria.getGroupKey()) &&
                condition.apply(criteria.getRequestedAt()) &&
                condition.apply(criteria.getAckDeadline()) &&
                condition.apply(criteria.getEscalationLevel()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getAcknowledgedBy()) &&
                condition.apply(criteria.getAcknowledgedAt()) &&
                condition.apply(criteria.getSummary()) &&
                condition.apply(criteria.getOccurrenceCount()) &&
                condition.apply(criteria.getConsolidationWindowId()) &&
                condition.apply(criteria.getSummarizedWindowId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<NotificationCriteria> copyFiltersAre(
        NotificationCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getNotificationType(), copy.getNotificationType()) &&
                condition.apply(criteria.getUrgency(), copy.getUrgency()) &&
                condition.apply(criteria.getSubject(), copy.getSubject()) &&
                condition.apply(criteria.getObjectType(), copy.getObjectType()) &&
                condition.apply(criteria.getObjectId(), copy.getObjectId()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getSourceEventId(), copy.getSourceEventId()) &&
                condition.apply(criteria.getSourceService(), copy.getSourceService()) &&
                condition.apply(criteria.getUrgentPort(), copy.getUrgentPort()) &&
                condition.apply(criteria.getCorrelationId(), copy.getCorrelationId()) &&
                condition.apply(criteria.getGroupKey(), copy.getGroupKey()) &&
                condition.apply(criteria.getRequestedAt(), copy.getRequestedAt()) &&
                condition.apply(criteria.getAckDeadline(), copy.getAckDeadline()) &&
                condition.apply(criteria.getEscalationLevel(), copy.getEscalationLevel()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getAcknowledgedBy(), copy.getAcknowledgedBy()) &&
                condition.apply(criteria.getAcknowledgedAt(), copy.getAcknowledgedAt()) &&
                condition.apply(criteria.getSummary(), copy.getSummary()) &&
                condition.apply(criteria.getOccurrenceCount(), copy.getOccurrenceCount()) &&
                condition.apply(criteria.getConsolidationWindowId(), copy.getConsolidationWindowId()) &&
                condition.apply(criteria.getSummarizedWindowId(), copy.getSummarizedWindowId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
