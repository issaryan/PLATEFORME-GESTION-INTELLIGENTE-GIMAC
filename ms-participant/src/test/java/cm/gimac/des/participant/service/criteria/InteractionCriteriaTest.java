package cm.gimac.des.participant.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class InteractionCriteriaTest {

    @Test
    void newInteractionCriteriaHasAllFiltersNullTest() {
        var interactionCriteria = new InteractionCriteria();
        assertThat(interactionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void interactionCriteriaFluentMethodsCreatesFiltersTest() {
        var interactionCriteria = new InteractionCriteria();

        setAllFilters(interactionCriteria);

        assertThat(interactionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void interactionCriteriaCopyCreatesNullFilterTest() {
        var interactionCriteria = new InteractionCriteria();
        var copy = interactionCriteria.copy();

        assertThat(interactionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(interactionCriteria)
        );
    }

    @Test
    void interactionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var interactionCriteria = new InteractionCriteria();
        setAllFilters(interactionCriteria);

        var copy = interactionCriteria.copy();

        assertThat(interactionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(interactionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var interactionCriteria = new InteractionCriteria();

        assertThat(interactionCriteria).hasToString("InteractionCriteria{}");
    }

    private static void setAllFilters(InteractionCriteria interactionCriteria) {
        interactionCriteria.id();
        interactionCriteria.nature();
        interactionCriteria.summary();
        interactionCriteria.occurredAt();
        interactionCriteria.authorId();
        interactionCriteria.reference();
        interactionCriteria.notificationId();
        interactionCriteria.sourceEventId();
        interactionCriteria.institutionId();
        interactionCriteria.distinct();
    }

    private static Condition<InteractionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getNature()) &&
                condition.apply(criteria.getSummary()) &&
                condition.apply(criteria.getOccurredAt()) &&
                condition.apply(criteria.getAuthorId()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getNotificationId()) &&
                condition.apply(criteria.getSourceEventId()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<InteractionCriteria> copyFiltersAre(InteractionCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getNature(), copy.getNature()) &&
                condition.apply(criteria.getSummary(), copy.getSummary()) &&
                condition.apply(criteria.getOccurredAt(), copy.getOccurredAt()) &&
                condition.apply(criteria.getAuthorId(), copy.getAuthorId()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getNotificationId(), copy.getNotificationId()) &&
                condition.apply(criteria.getSourceEventId(), copy.getSourceEventId()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
