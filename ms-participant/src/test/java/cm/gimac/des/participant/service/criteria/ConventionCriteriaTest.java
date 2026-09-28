package cm.gimac.des.participant.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ConventionCriteriaTest {

    @Test
    void newConventionCriteriaHasAllFiltersNullTest() {
        var conventionCriteria = new ConventionCriteria();
        assertThat(conventionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void conventionCriteriaFluentMethodsCreatesFiltersTest() {
        var conventionCriteria = new ConventionCriteria();

        setAllFilters(conventionCriteria);

        assertThat(conventionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void conventionCriteriaCopyCreatesNullFilterTest() {
        var conventionCriteria = new ConventionCriteria();
        var copy = conventionCriteria.copy();

        assertThat(conventionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(conventionCriteria)
        );
    }

    @Test
    void conventionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var conventionCriteria = new ConventionCriteria();
        setAllFilters(conventionCriteria);

        var copy = conventionCriteria.copy();

        assertThat(conventionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(conventionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var conventionCriteria = new ConventionCriteria();

        assertThat(conventionCriteria).hasToString("ConventionCriteria{}");
    }

    private static void setAllFilters(ConventionCriteria conventionCriteria) {
        conventionCriteria.id();
        conventionCriteria.type();
        conventionCriteria.reference();
        conventionCriteria.signedOn();
        conventionCriteria.expiresOn();
        conventionCriteria.documentId();
        conventionCriteria.documentSha256();
        conventionCriteria.institutionId();
        conventionCriteria.distinct();
    }

    private static Condition<ConventionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getSignedOn()) &&
                condition.apply(criteria.getExpiresOn()) &&
                condition.apply(criteria.getDocumentId()) &&
                condition.apply(criteria.getDocumentSha256()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ConventionCriteria> copyFiltersAre(ConventionCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getSignedOn(), copy.getSignedOn()) &&
                condition.apply(criteria.getExpiresOn(), copy.getExpiresOn()) &&
                condition.apply(criteria.getDocumentId(), copy.getDocumentId()) &&
                condition.apply(criteria.getDocumentSha256(), copy.getDocumentSha256()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
