package cm.gimac.des.participant.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class LegalDocumentCriteriaTest {

    @Test
    void newLegalDocumentCriteriaHasAllFiltersNullTest() {
        var legalDocumentCriteria = new LegalDocumentCriteria();
        assertThat(legalDocumentCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void legalDocumentCriteriaFluentMethodsCreatesFiltersTest() {
        var legalDocumentCriteria = new LegalDocumentCriteria();

        setAllFilters(legalDocumentCriteria);

        assertThat(legalDocumentCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void legalDocumentCriteriaCopyCreatesNullFilterTest() {
        var legalDocumentCriteria = new LegalDocumentCriteria();
        var copy = legalDocumentCriteria.copy();

        assertThat(legalDocumentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(legalDocumentCriteria)
        );
    }

    @Test
    void legalDocumentCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var legalDocumentCriteria = new LegalDocumentCriteria();
        setAllFilters(legalDocumentCriteria);

        var copy = legalDocumentCriteria.copy();

        assertThat(legalDocumentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(legalDocumentCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var legalDocumentCriteria = new LegalDocumentCriteria();

        assertThat(legalDocumentCriteria).hasToString("LegalDocumentCriteria{}");
    }

    private static void setAllFilters(LegalDocumentCriteria legalDocumentCriteria) {
        legalDocumentCriteria.id();
        legalDocumentCriteria.type();
        legalDocumentCriteria.reference();
        legalDocumentCriteria.issuingAuthority();
        legalDocumentCriteria.issuedOn();
        legalDocumentCriteria.expiresOn();
        legalDocumentCriteria.documentId();
        legalDocumentCriteria.documentVersion();
        legalDocumentCriteria.digest();
        legalDocumentCriteria.sealed();
        legalDocumentCriteria.state();
        legalDocumentCriteria.previousDocumentId();
        legalDocumentCriteria.institutionId();
        legalDocumentCriteria.legalDocumentId();
        legalDocumentCriteria.distinct();
    }

    private static Condition<LegalDocumentCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getIssuingAuthority()) &&
                condition.apply(criteria.getIssuedOn()) &&
                condition.apply(criteria.getExpiresOn()) &&
                condition.apply(criteria.getDocumentId()) &&
                condition.apply(criteria.getDocumentVersion()) &&
                condition.apply(criteria.getDigest()) &&
                condition.apply(criteria.getSealed()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getPreviousDocumentId()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getLegalDocumentId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<LegalDocumentCriteria> copyFiltersAre(
        LegalDocumentCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getIssuingAuthority(), copy.getIssuingAuthority()) &&
                condition.apply(criteria.getIssuedOn(), copy.getIssuedOn()) &&
                condition.apply(criteria.getExpiresOn(), copy.getExpiresOn()) &&
                condition.apply(criteria.getDocumentId(), copy.getDocumentId()) &&
                condition.apply(criteria.getDocumentVersion(), copy.getDocumentVersion()) &&
                condition.apply(criteria.getDigest(), copy.getDigest()) &&
                condition.apply(criteria.getSealed(), copy.getSealed()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getPreviousDocumentId(), copy.getPreviousDocumentId()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getLegalDocumentId(), copy.getLegalDocumentId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
