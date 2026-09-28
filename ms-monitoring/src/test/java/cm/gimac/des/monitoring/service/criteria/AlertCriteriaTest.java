package cm.gimac.des.monitoring.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class AlertCriteriaTest {

    @Test
    void newAlertCriteriaHasAllFiltersNullTest() {
        var alertCriteria = new AlertCriteria();
        assertThat(alertCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void alertCriteriaFluentMethodsCreatesFiltersTest() {
        var alertCriteria = new AlertCriteria();

        setAllFilters(alertCriteria);

        assertThat(alertCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void alertCriteriaCopyCreatesNullFilterTest() {
        var alertCriteria = new AlertCriteria();
        var copy = alertCriteria.copy();

        assertThat(alertCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(alertCriteria)
        );
    }

    @Test
    void alertCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var alertCriteria = new AlertCriteria();
        setAllFilters(alertCriteria);

        var copy = alertCriteria.copy();

        assertThat(alertCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(alertCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var alertCriteria = new AlertCriteria();

        assertThat(alertCriteria).hasToString("AlertCriteria{}");
    }

    private static void setAllFilters(AlertCriteria alertCriteria) {
        alertCriteria.id();
        alertCriteria.reference();
        alertCriteria.source();
        alertCriteria.rule();
        alertCriteria.fingerprint();
        alertCriteria.severity();
        alertCriteria.state();
        alertCriteria.firstSeenAt();
        alertCriteria.lastSeenAt();
        alertCriteria.occurrences();
        alertCriteria.ackDeadline();
        alertCriteria.overdue();
        alertCriteria.acknowledgedBy();
        alertCriteria.acknowledgedAt();
        alertCriteria.ticketRequested();
        alertCriteria.ackComment();
        alertCriteria.ticketId();
        alertCriteria.resolvedAt();
        alertCriteria.linkId();
        alertCriteria.correlationRootId();
        alertCriteria.distinct();
    }

    private static Condition<AlertCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getSource()) &&
                condition.apply(criteria.getRule()) &&
                condition.apply(criteria.getFingerprint()) &&
                condition.apply(criteria.getSeverity()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getFirstSeenAt()) &&
                condition.apply(criteria.getLastSeenAt()) &&
                condition.apply(criteria.getOccurrences()) &&
                condition.apply(criteria.getAckDeadline()) &&
                condition.apply(criteria.getOverdue()) &&
                condition.apply(criteria.getAcknowledgedBy()) &&
                condition.apply(criteria.getAcknowledgedAt()) &&
                condition.apply(criteria.getTicketRequested()) &&
                condition.apply(criteria.getAckComment()) &&
                condition.apply(criteria.getTicketId()) &&
                condition.apply(criteria.getResolvedAt()) &&
                condition.apply(criteria.getLinkId()) &&
                condition.apply(criteria.getCorrelationRootId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<AlertCriteria> copyFiltersAre(AlertCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getSource(), copy.getSource()) &&
                condition.apply(criteria.getRule(), copy.getRule()) &&
                condition.apply(criteria.getFingerprint(), copy.getFingerprint()) &&
                condition.apply(criteria.getSeverity(), copy.getSeverity()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getFirstSeenAt(), copy.getFirstSeenAt()) &&
                condition.apply(criteria.getLastSeenAt(), copy.getLastSeenAt()) &&
                condition.apply(criteria.getOccurrences(), copy.getOccurrences()) &&
                condition.apply(criteria.getAckDeadline(), copy.getAckDeadline()) &&
                condition.apply(criteria.getOverdue(), copy.getOverdue()) &&
                condition.apply(criteria.getAcknowledgedBy(), copy.getAcknowledgedBy()) &&
                condition.apply(criteria.getAcknowledgedAt(), copy.getAcknowledgedAt()) &&
                condition.apply(criteria.getTicketRequested(), copy.getTicketRequested()) &&
                condition.apply(criteria.getAckComment(), copy.getAckComment()) &&
                condition.apply(criteria.getTicketId(), copy.getTicketId()) &&
                condition.apply(criteria.getResolvedAt(), copy.getResolvedAt()) &&
                condition.apply(criteria.getLinkId(), copy.getLinkId()) &&
                condition.apply(criteria.getCorrelationRootId(), copy.getCorrelationRootId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
