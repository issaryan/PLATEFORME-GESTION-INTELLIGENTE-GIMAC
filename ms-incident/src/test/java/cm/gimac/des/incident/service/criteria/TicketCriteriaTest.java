package cm.gimac.des.incident.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class TicketCriteriaTest {

    @Test
    void newTicketCriteriaHasAllFiltersNullTest() {
        var ticketCriteria = new TicketCriteria();
        assertThat(ticketCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void ticketCriteriaFluentMethodsCreatesFiltersTest() {
        var ticketCriteria = new TicketCriteria();

        setAllFilters(ticketCriteria);

        assertThat(ticketCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void ticketCriteriaCopyCreatesNullFilterTest() {
        var ticketCriteria = new TicketCriteria();
        var copy = ticketCriteria.copy();

        assertThat(ticketCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(ticketCriteria)
        );
    }

    @Test
    void ticketCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var ticketCriteria = new TicketCriteria();
        setAllFilters(ticketCriteria);

        var copy = ticketCriteria.copy();

        assertThat(ticketCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(ticketCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var ticketCriteria = new TicketCriteria();

        assertThat(ticketCriteria).hasToString("TicketCriteria{}");
    }

    private static void setAllFilters(TicketCriteria ticketCriteria) {
        ticketCriteria.id();
        ticketCriteria.ticketNumber();
        ticketCriteria.type();
        ticketCriteria.subject();
        ticketCriteria.origin();
        ticketCriteria.originRef();
        ticketCriteria.requesterId();
        ticketCriteria.institutionId();
        ticketCriteria.linkId();
        ticketCriteria.alertId();
        ticketCriteria.priority();
        ticketCriteria.state();
        ticketCriteria.assigneeId();
        ticketCriteria.openedAt();
        ticketCriteria.resolvedAt();
        ticketCriteria.resolverId();
        ticketCriteria.closedAt();
        ticketCriteria.closedBy();
        ticketCriteria.version();
        ticketCriteria.relatedTicketId();
        ticketCriteria.incidentId();
        ticketCriteria.distinct();
    }

    private static Condition<TicketCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTicketNumber()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getSubject()) &&
                condition.apply(criteria.getOrigin()) &&
                condition.apply(criteria.getOriginRef()) &&
                condition.apply(criteria.getRequesterId()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getLinkId()) &&
                condition.apply(criteria.getAlertId()) &&
                condition.apply(criteria.getPriority()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getAssigneeId()) &&
                condition.apply(criteria.getOpenedAt()) &&
                condition.apply(criteria.getResolvedAt()) &&
                condition.apply(criteria.getResolverId()) &&
                condition.apply(criteria.getClosedAt()) &&
                condition.apply(criteria.getClosedBy()) &&
                condition.apply(criteria.getVersion()) &&
                condition.apply(criteria.getRelatedTicketId()) &&
                condition.apply(criteria.getIncidentId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<TicketCriteria> copyFiltersAre(TicketCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTicketNumber(), copy.getTicketNumber()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getSubject(), copy.getSubject()) &&
                condition.apply(criteria.getOrigin(), copy.getOrigin()) &&
                condition.apply(criteria.getOriginRef(), copy.getOriginRef()) &&
                condition.apply(criteria.getRequesterId(), copy.getRequesterId()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getLinkId(), copy.getLinkId()) &&
                condition.apply(criteria.getAlertId(), copy.getAlertId()) &&
                condition.apply(criteria.getPriority(), copy.getPriority()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getAssigneeId(), copy.getAssigneeId()) &&
                condition.apply(criteria.getOpenedAt(), copy.getOpenedAt()) &&
                condition.apply(criteria.getResolvedAt(), copy.getResolvedAt()) &&
                condition.apply(criteria.getResolverId(), copy.getResolverId()) &&
                condition.apply(criteria.getClosedAt(), copy.getClosedAt()) &&
                condition.apply(criteria.getClosedBy(), copy.getClosedBy()) &&
                condition.apply(criteria.getVersion(), copy.getVersion()) &&
                condition.apply(criteria.getRelatedTicketId(), copy.getRelatedTicketId()) &&
                condition.apply(criteria.getIncidentId(), copy.getIncidentId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
