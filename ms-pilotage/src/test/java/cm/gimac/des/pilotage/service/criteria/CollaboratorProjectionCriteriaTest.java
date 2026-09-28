package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CollaboratorProjectionCriteriaTest {

    @Test
    void newCollaboratorProjectionCriteriaHasAllFiltersNullTest() {
        var collaboratorProjectionCriteria = new CollaboratorProjectionCriteria();
        assertThat(collaboratorProjectionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void collaboratorProjectionCriteriaFluentMethodsCreatesFiltersTest() {
        var collaboratorProjectionCriteria = new CollaboratorProjectionCriteria();

        setAllFilters(collaboratorProjectionCriteria);

        assertThat(collaboratorProjectionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void collaboratorProjectionCriteriaCopyCreatesNullFilterTest() {
        var collaboratorProjectionCriteria = new CollaboratorProjectionCriteria();
        var copy = collaboratorProjectionCriteria.copy();

        assertThat(collaboratorProjectionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(collaboratorProjectionCriteria)
        );
    }

    @Test
    void collaboratorProjectionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var collaboratorProjectionCriteria = new CollaboratorProjectionCriteria();
        setAllFilters(collaboratorProjectionCriteria);

        var copy = collaboratorProjectionCriteria.copy();

        assertThat(collaboratorProjectionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(collaboratorProjectionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var collaboratorProjectionCriteria = new CollaboratorProjectionCriteria();

        assertThat(collaboratorProjectionCriteria).hasToString("CollaboratorProjectionCriteria{}");
    }

    private static void setAllFilters(CollaboratorProjectionCriteria collaboratorProjectionCriteria) {
        collaboratorProjectionCriteria.id();
        collaboratorProjectionCriteria.collaboratorId();
        collaboratorProjectionCriteria.displayName();
        collaboratorProjectionCriteria.active();
        collaboratorProjectionCriteria.serviceCode();
        collaboratorProjectionCriteria.updatedAt();
        collaboratorProjectionCriteria.distinct();
    }

    private static Condition<CollaboratorProjectionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getCollaboratorId()) &&
                condition.apply(criteria.getDisplayName()) &&
                condition.apply(criteria.getActive()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CollaboratorProjectionCriteria> copyFiltersAre(
        CollaboratorProjectionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getCollaboratorId(), copy.getCollaboratorId()) &&
                condition.apply(criteria.getDisplayName(), copy.getDisplayName()) &&
                condition.apply(criteria.getActive(), copy.getActive()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
