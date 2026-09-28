package cm.gimac.des.notification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DeliveryTraceCriteriaTest {

    @Test
    void newDeliveryTraceCriteriaHasAllFiltersNullTest() {
        var deliveryTraceCriteria = new DeliveryTraceCriteria();
        assertThat(deliveryTraceCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void deliveryTraceCriteriaFluentMethodsCreatesFiltersTest() {
        var deliveryTraceCriteria = new DeliveryTraceCriteria();

        setAllFilters(deliveryTraceCriteria);

        assertThat(deliveryTraceCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void deliveryTraceCriteriaCopyCreatesNullFilterTest() {
        var deliveryTraceCriteria = new DeliveryTraceCriteria();
        var copy = deliveryTraceCriteria.copy();

        assertThat(deliveryTraceCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(deliveryTraceCriteria)
        );
    }

    @Test
    void deliveryTraceCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var deliveryTraceCriteria = new DeliveryTraceCriteria();
        setAllFilters(deliveryTraceCriteria);

        var copy = deliveryTraceCriteria.copy();

        assertThat(deliveryTraceCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(deliveryTraceCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var deliveryTraceCriteria = new DeliveryTraceCriteria();

        assertThat(deliveryTraceCriteria).hasToString("DeliveryTraceCriteria{}");
    }

    private static void setAllFilters(DeliveryTraceCriteria deliveryTraceCriteria) {
        deliveryTraceCriteria.id();
        deliveryTraceCriteria.userId();
        deliveryTraceCriteria.channel();
        deliveryTraceCriteria.state();
        deliveryTraceCriteria.attempts();
        deliveryTraceCriteria.nextAttemptAt();
        deliveryTraceCriteria.lastError();
        deliveryTraceCriteria.providerMessageId();
        deliveryTraceCriteria.deliveredAt();
        deliveryTraceCriteria.acknowledgedAt();
        deliveryTraceCriteria.notificationId();
        deliveryTraceCriteria.templateId();
        deliveryTraceCriteria.distinct();
    }

    private static Condition<DeliveryTraceCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getChannel()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getAttempts()) &&
                condition.apply(criteria.getNextAttemptAt()) &&
                condition.apply(criteria.getLastError()) &&
                condition.apply(criteria.getProviderMessageId()) &&
                condition.apply(criteria.getDeliveredAt()) &&
                condition.apply(criteria.getAcknowledgedAt()) &&
                condition.apply(criteria.getNotificationId()) &&
                condition.apply(criteria.getTemplateId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DeliveryTraceCriteria> copyFiltersAre(
        DeliveryTraceCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getChannel(), copy.getChannel()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getAttempts(), copy.getAttempts()) &&
                condition.apply(criteria.getNextAttemptAt(), copy.getNextAttemptAt()) &&
                condition.apply(criteria.getLastError(), copy.getLastError()) &&
                condition.apply(criteria.getProviderMessageId(), copy.getProviderMessageId()) &&
                condition.apply(criteria.getDeliveredAt(), copy.getDeliveredAt()) &&
                condition.apply(criteria.getAcknowledgedAt(), copy.getAcknowledgedAt()) &&
                condition.apply(criteria.getNotificationId(), copy.getNotificationId()) &&
                condition.apply(criteria.getTemplateId(), copy.getTemplateId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
