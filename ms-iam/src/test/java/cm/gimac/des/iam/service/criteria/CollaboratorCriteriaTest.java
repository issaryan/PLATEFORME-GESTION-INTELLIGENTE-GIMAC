package cm.gimac.des.iam.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CollaboratorCriteriaTest {

    @Test
    void newCollaboratorCriteriaHasAllFiltersNullTest() {
        var collaboratorCriteria = new CollaboratorCriteria();
        assertThat(collaboratorCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void collaboratorCriteriaFluentMethodsCreatesFiltersTest() {
        var collaboratorCriteria = new CollaboratorCriteria();

        setAllFilters(collaboratorCriteria);

        assertThat(collaboratorCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void collaboratorCriteriaCopyCreatesNullFilterTest() {
        var collaboratorCriteria = new CollaboratorCriteria();
        var copy = collaboratorCriteria.copy();

        assertThat(collaboratorCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(collaboratorCriteria)
        );
    }

    @Test
    void collaboratorCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var collaboratorCriteria = new CollaboratorCriteria();
        setAllFilters(collaboratorCriteria);

        var copy = collaboratorCriteria.copy();

        assertThat(collaboratorCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(collaboratorCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var collaboratorCriteria = new CollaboratorCriteria();

        assertThat(collaboratorCriteria).hasToString("CollaboratorCriteria{}");
    }

    private static void setAllFilters(CollaboratorCriteria collaboratorCriteria) {
        collaboratorCriteria.id();
        collaboratorCriteria.federatedId();
        collaboratorCriteria.directoryId();
        collaboratorCriteria.displayName();
        collaboratorCriteria.email();
        collaboratorCriteria.status();
        collaboratorCriteria.level();
        collaboratorCriteria.institutionId();
        collaboratorCriteria.language();
        collaboratorCriteria.failedAttempts();
        collaboratorCriteria.lastLoginAt();
        collaboratorCriteria.directoryCursor();
        collaboratorCriteria.distinct();
    }

    private static Condition<CollaboratorCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getFederatedId()) &&
                condition.apply(criteria.getDirectoryId()) &&
                condition.apply(criteria.getDisplayName()) &&
                condition.apply(criteria.getEmail()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getLevel()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getLanguage()) &&
                condition.apply(criteria.getFailedAttempts()) &&
                condition.apply(criteria.getLastLoginAt()) &&
                condition.apply(criteria.getDirectoryCursor()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CollaboratorCriteria> copyFiltersAre(
        CollaboratorCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getFederatedId(), copy.getFederatedId()) &&
                condition.apply(criteria.getDirectoryId(), copy.getDirectoryId()) &&
                condition.apply(criteria.getDisplayName(), copy.getDisplayName()) &&
                condition.apply(criteria.getEmail(), copy.getEmail()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getLevel(), copy.getLevel()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getLanguage(), copy.getLanguage()) &&
                condition.apply(criteria.getFailedAttempts(), copy.getFailedAttempts()) &&
                condition.apply(criteria.getLastLoginAt(), copy.getLastLoginAt()) &&
                condition.apply(criteria.getDirectoryCursor(), copy.getDirectoryCursor()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
