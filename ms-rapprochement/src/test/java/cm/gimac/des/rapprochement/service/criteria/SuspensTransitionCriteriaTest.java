package cm.gimac.des.rapprochement.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class SuspensTransitionCriteriaTest {

    @Test
    void newSuspensTransitionCriteriaHasAllFiltersNullTest() {
        var suspensTransitionCriteria = new SuspensTransitionCriteria();
        assertThat(suspensTransitionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void suspensTransitionCriteriaFluentMethodsCreatesFiltersTest() {
        var suspensTransitionCriteria = new SuspensTransitionCriteria();

        setAllFilters(suspensTransitionCriteria);

        assertThat(suspensTransitionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void suspensTransitionCriteriaCopyCreatesNullFilterTest() {
        var suspensTransitionCriteria = new SuspensTransitionCriteria();
        var copy = suspensTransitionCriteria.copy();

        assertThat(suspensTransitionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(suspensTransitionCriteria)
        );
    }

    @Test
    void suspensTransitionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var suspensTransitionCriteria = new SuspensTransitionCriteria();
        setAllFilters(suspensTransitionCriteria);

        var copy = suspensTransitionCriteria.copy();

        assertThat(suspensTransitionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(suspensTransitionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var suspensTransitionCriteria = new SuspensTransitionCriteria();

        assertThat(suspensTransitionCriteria).hasToString("SuspensTransitionCriteria{}");
    }

    private static void setAllFilters(SuspensTransitionCriteria suspensTransitionCriteria) {
        suspensTransitionCriteria.id();
        suspensTransitionCriteria.fromState();
        suspensTransitionCriteria.toState();
        suspensTransitionCriteria.actorId();
        suspensTransitionCriteria.occurredAt();
        suspensTransitionCriteria.justification();
        suspensTransitionCriteria.suspensId();
        suspensTransitionCriteria.distinct();
    }

    private static Condition<SuspensTransitionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getFromState()) &&
                condition.apply(criteria.getToState()) &&
                condition.apply(criteria.getActorId()) &&
                condition.apply(criteria.getOccurredAt()) &&
                condition.apply(criteria.getJustification()) &&
                condition.apply(criteria.getSuspensId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<SuspensTransitionCriteria> copyFiltersAre(
        SuspensTransitionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getFromState(), copy.getFromState()) &&
                condition.apply(criteria.getToState(), copy.getToState()) &&
                condition.apply(criteria.getActorId(), copy.getActorId()) &&
                condition.apply(criteria.getOccurredAt(), copy.getOccurredAt()) &&
                condition.apply(criteria.getJustification(), copy.getJustification()) &&
                condition.apply(criteria.getSuspensId(), copy.getSuspensId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
