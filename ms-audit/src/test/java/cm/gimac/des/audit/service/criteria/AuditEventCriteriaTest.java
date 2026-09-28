package cm.gimac.des.audit.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class AuditEventCriteriaTest {

    @Test
    void newAuditEventCriteriaHasAllFiltersNullTest() {
        var auditEventCriteria = new AuditEventCriteria();
        assertThat(auditEventCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void auditEventCriteriaFluentMethodsCreatesFiltersTest() {
        var auditEventCriteria = new AuditEventCriteria();

        setAllFilters(auditEventCriteria);

        assertThat(auditEventCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void auditEventCriteriaCopyCreatesNullFilterTest() {
        var auditEventCriteria = new AuditEventCriteria();
        var copy = auditEventCriteria.copy();

        assertThat(auditEventCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(auditEventCriteria)
        );
    }

    @Test
    void auditEventCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var auditEventCriteria = new AuditEventCriteria();
        setAllFilters(auditEventCriteria);

        var copy = auditEventCriteria.copy();

        assertThat(auditEventCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(auditEventCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var auditEventCriteria = new AuditEventCriteria();

        assertThat(auditEventCriteria).hasToString("AuditEventCriteria{}");
    }

    private static void setAllFilters(AuditEventCriteria auditEventCriteria) {
        auditEventCriteria.id();
        auditEventCriteria.logIndex();
        auditEventCriteria.eventId();
        auditEventCriteria.occurredAt();
        auditEventCriteria.recordedAt();
        auditEventCriteria.actorId();
        auditEventCriteria.actionType();
        auditEventCriteria.domain();
        auditEventCriteria.subject();
        auditEventCriteria.correlationId();
        auditEventCriteria.leafHash();
        auditEventCriteria.distinct();
    }

    private static Condition<AuditEventCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getLogIndex()) &&
                condition.apply(criteria.getEventId()) &&
                condition.apply(criteria.getOccurredAt()) &&
                condition.apply(criteria.getRecordedAt()) &&
                condition.apply(criteria.getActorId()) &&
                condition.apply(criteria.getActionType()) &&
                condition.apply(criteria.getDomain()) &&
                condition.apply(criteria.getSubject()) &&
                condition.apply(criteria.getCorrelationId()) &&
                condition.apply(criteria.getLeafHash()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<AuditEventCriteria> copyFiltersAre(AuditEventCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getLogIndex(), copy.getLogIndex()) &&
                condition.apply(criteria.getEventId(), copy.getEventId()) &&
                condition.apply(criteria.getOccurredAt(), copy.getOccurredAt()) &&
                condition.apply(criteria.getRecordedAt(), copy.getRecordedAt()) &&
                condition.apply(criteria.getActorId(), copy.getActorId()) &&
                condition.apply(criteria.getActionType(), copy.getActionType()) &&
                condition.apply(criteria.getDomain(), copy.getDomain()) &&
                condition.apply(criteria.getSubject(), copy.getSubject()) &&
                condition.apply(criteria.getCorrelationId(), copy.getCorrelationId()) &&
                condition.apply(criteria.getLeafHash(), copy.getLeafHash()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
