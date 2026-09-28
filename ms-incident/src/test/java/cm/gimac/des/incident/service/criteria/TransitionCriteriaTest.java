package cm.gimac.des.incident.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class TransitionCriteriaTest {

    @Test
    void newTransitionCriteriaHasAllFiltersNullTest() {
        var transitionCriteria = new TransitionCriteria();
        assertThat(transitionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void transitionCriteriaFluentMethodsCreatesFiltersTest() {
        var transitionCriteria = new TransitionCriteria();

        setAllFilters(transitionCriteria);

        assertThat(transitionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void transitionCriteriaCopyCreatesNullFilterTest() {
        var transitionCriteria = new TransitionCriteria();
        var copy = transitionCriteria.copy();

        assertThat(transitionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(transitionCriteria)
        );
    }

    @Test
    void transitionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var transitionCriteria = new TransitionCriteria();
        setAllFilters(transitionCriteria);

        var copy = transitionCriteria.copy();

        assertThat(transitionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(transitionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var transitionCriteria = new TransitionCriteria();

        assertThat(transitionCriteria).hasToString("TransitionCriteria{}");
    }

    private static void setAllFilters(TransitionCriteria transitionCriteria) {
        transitionCriteria.id();
        transitionCriteria.chainIndex();
        transitionCriteria.fromState();
        transitionCriteria.toState();
        transitionCriteria.actorId();
        transitionCriteria.occurredAt();
        transitionCriteria.justification();
        transitionCriteria.previousHash();
        transitionCriteria.hash();
        transitionCriteria.ticketId();
        transitionCriteria.distinct();
    }

    private static Condition<TransitionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getChainIndex()) &&
                condition.apply(criteria.getFromState()) &&
                condition.apply(criteria.getToState()) &&
                condition.apply(criteria.getActorId()) &&
                condition.apply(criteria.getOccurredAt()) &&
                condition.apply(criteria.getJustification()) &&
                condition.apply(criteria.getPreviousHash()) &&
                condition.apply(criteria.getHash()) &&
                condition.apply(criteria.getTicketId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<TransitionCriteria> copyFiltersAre(TransitionCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getChainIndex(), copy.getChainIndex()) &&
                condition.apply(criteria.getFromState(), copy.getFromState()) &&
                condition.apply(criteria.getToState(), copy.getToState()) &&
                condition.apply(criteria.getActorId(), copy.getActorId()) &&
                condition.apply(criteria.getOccurredAt(), copy.getOccurredAt()) &&
                condition.apply(criteria.getJustification(), copy.getJustification()) &&
                condition.apply(criteria.getPreviousHash(), copy.getPreviousHash()) &&
                condition.apply(criteria.getHash(), copy.getHash()) &&
                condition.apply(criteria.getTicketId(), copy.getTicketId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
