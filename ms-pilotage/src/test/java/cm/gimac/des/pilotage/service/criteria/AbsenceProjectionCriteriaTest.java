package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class AbsenceProjectionCriteriaTest {

    @Test
    void newAbsenceProjectionCriteriaHasAllFiltersNullTest() {
        var absenceProjectionCriteria = new AbsenceProjectionCriteria();
        assertThat(absenceProjectionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void absenceProjectionCriteriaFluentMethodsCreatesFiltersTest() {
        var absenceProjectionCriteria = new AbsenceProjectionCriteria();

        setAllFilters(absenceProjectionCriteria);

        assertThat(absenceProjectionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void absenceProjectionCriteriaCopyCreatesNullFilterTest() {
        var absenceProjectionCriteria = new AbsenceProjectionCriteria();
        var copy = absenceProjectionCriteria.copy();

        assertThat(absenceProjectionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(absenceProjectionCriteria)
        );
    }

    @Test
    void absenceProjectionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var absenceProjectionCriteria = new AbsenceProjectionCriteria();
        setAllFilters(absenceProjectionCriteria);

        var copy = absenceProjectionCriteria.copy();

        assertThat(absenceProjectionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(absenceProjectionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var absenceProjectionCriteria = new AbsenceProjectionCriteria();

        assertThat(absenceProjectionCriteria).hasToString("AbsenceProjectionCriteria{}");
    }

    private static void setAllFilters(AbsenceProjectionCriteria absenceProjectionCriteria) {
        absenceProjectionCriteria.id();
        absenceProjectionCriteria.availabilityId();
        absenceProjectionCriteria.collaboratorId();
        absenceProjectionCriteria.startsAt();
        absenceProjectionCriteria.endsAt();
        absenceProjectionCriteria.updatedAt();
        absenceProjectionCriteria.distinct();
    }

    private static Condition<AbsenceProjectionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getAvailabilityId()) &&
                condition.apply(criteria.getCollaboratorId()) &&
                condition.apply(criteria.getStartsAt()) &&
                condition.apply(criteria.getEndsAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<AbsenceProjectionCriteria> copyFiltersAre(
        AbsenceProjectionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getAvailabilityId(), copy.getAvailabilityId()) &&
                condition.apply(criteria.getCollaboratorId(), copy.getCollaboratorId()) &&
                condition.apply(criteria.getStartsAt(), copy.getStartsAt()) &&
                condition.apply(criteria.getEndsAt(), copy.getEndsAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
