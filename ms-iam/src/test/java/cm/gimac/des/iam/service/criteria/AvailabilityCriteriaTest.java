package cm.gimac.des.iam.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class AvailabilityCriteriaTest {

    @Test
    void newAvailabilityCriteriaHasAllFiltersNullTest() {
        var availabilityCriteria = new AvailabilityCriteria();
        assertThat(availabilityCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void availabilityCriteriaFluentMethodsCreatesFiltersTest() {
        var availabilityCriteria = new AvailabilityCriteria();

        setAllFilters(availabilityCriteria);

        assertThat(availabilityCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void availabilityCriteriaCopyCreatesNullFilterTest() {
        var availabilityCriteria = new AvailabilityCriteria();
        var copy = availabilityCriteria.copy();

        assertThat(availabilityCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(availabilityCriteria)
        );
    }

    @Test
    void availabilityCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var availabilityCriteria = new AvailabilityCriteria();
        setAllFilters(availabilityCriteria);

        var copy = availabilityCriteria.copy();

        assertThat(availabilityCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(availabilityCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var availabilityCriteria = new AvailabilityCriteria();

        assertThat(availabilityCriteria).hasToString("AvailabilityCriteria{}");
    }

    private static void setAllFilters(AvailabilityCriteria availabilityCriteria) {
        availabilityCriteria.id();
        availabilityCriteria.type();
        availabilityCriteria.startsAt();
        availabilityCriteria.endsAt();
        availabilityCriteria.reason();
        availabilityCriteria.justificationDocumentId();
        availabilityCriteria.validationState();
        availabilityCriteria.validatorId();
        availabilityCriteria.validatedAt();
        availabilityCriteria.collaboratorId();
        availabilityCriteria.distinct();
    }

    private static Condition<AvailabilityCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getStartsAt()) &&
                condition.apply(criteria.getEndsAt()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getJustificationDocumentId()) &&
                condition.apply(criteria.getValidationState()) &&
                condition.apply(criteria.getValidatorId()) &&
                condition.apply(criteria.getValidatedAt()) &&
                condition.apply(criteria.getCollaboratorId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<AvailabilityCriteria> copyFiltersAre(
        AvailabilityCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getStartsAt(), copy.getStartsAt()) &&
                condition.apply(criteria.getEndsAt(), copy.getEndsAt()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getJustificationDocumentId(), copy.getJustificationDocumentId()) &&
                condition.apply(criteria.getValidationState(), copy.getValidationState()) &&
                condition.apply(criteria.getValidatorId(), copy.getValidatorId()) &&
                condition.apply(criteria.getValidatedAt(), copy.getValidatedAt()) &&
                condition.apply(criteria.getCollaboratorId(), copy.getCollaboratorId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
