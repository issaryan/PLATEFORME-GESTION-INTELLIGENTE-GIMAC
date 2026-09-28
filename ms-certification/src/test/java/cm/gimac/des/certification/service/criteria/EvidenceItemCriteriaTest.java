package cm.gimac.des.certification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class EvidenceItemCriteriaTest {

    @Test
    void newEvidenceItemCriteriaHasAllFiltersNullTest() {
        var evidenceItemCriteria = new EvidenceItemCriteria();
        assertThat(evidenceItemCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void evidenceItemCriteriaFluentMethodsCreatesFiltersTest() {
        var evidenceItemCriteria = new EvidenceItemCriteria();

        setAllFilters(evidenceItemCriteria);

        assertThat(evidenceItemCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void evidenceItemCriteriaCopyCreatesNullFilterTest() {
        var evidenceItemCriteria = new EvidenceItemCriteria();
        var copy = evidenceItemCriteria.copy();

        assertThat(evidenceItemCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(evidenceItemCriteria)
        );
    }

    @Test
    void evidenceItemCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var evidenceItemCriteria = new EvidenceItemCriteria();
        setAllFilters(evidenceItemCriteria);

        var copy = evidenceItemCriteria.copy();

        assertThat(evidenceItemCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(evidenceItemCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var evidenceItemCriteria = new EvidenceItemCriteria();

        assertThat(evidenceItemCriteria).hasToString("EvidenceItemCriteria{}");
    }

    private static void setAllFilters(EvidenceItemCriteria evidenceItemCriteria) {
        evidenceItemCriteria.id();
        evidenceItemCriteria.documentId();
        evidenceItemCriteria.kind();
        evidenceItemCriteria.digest();
        evidenceItemCriteria.sealedAt();
        evidenceItemCriteria.sealConfirmed();
        evidenceItemCriteria.lastVerifiedAt();
        evidenceItemCriteria.attachedBy();
        evidenceItemCriteria.attachedAt();
        evidenceItemCriteria.fileId();
        evidenceItemCriteria.commissioningFileId();
        evidenceItemCriteria.distinct();
    }

    private static Condition<EvidenceItemCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getDocumentId()) &&
                condition.apply(criteria.getKind()) &&
                condition.apply(criteria.getDigest()) &&
                condition.apply(criteria.getSealedAt()) &&
                condition.apply(criteria.getSealConfirmed()) &&
                condition.apply(criteria.getLastVerifiedAt()) &&
                condition.apply(criteria.getAttachedBy()) &&
                condition.apply(criteria.getAttachedAt()) &&
                condition.apply(criteria.getFileId()) &&
                condition.apply(criteria.getCommissioningFileId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<EvidenceItemCriteria> copyFiltersAre(
        EvidenceItemCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getDocumentId(), copy.getDocumentId()) &&
                condition.apply(criteria.getKind(), copy.getKind()) &&
                condition.apply(criteria.getDigest(), copy.getDigest()) &&
                condition.apply(criteria.getSealedAt(), copy.getSealedAt()) &&
                condition.apply(criteria.getSealConfirmed(), copy.getSealConfirmed()) &&
                condition.apply(criteria.getLastVerifiedAt(), copy.getLastVerifiedAt()) &&
                condition.apply(criteria.getAttachedBy(), copy.getAttachedBy()) &&
                condition.apply(criteria.getAttachedAt(), copy.getAttachedAt()) &&
                condition.apply(criteria.getFileId(), copy.getFileId()) &&
                condition.apply(criteria.getCommissioningFileId(), copy.getCommissioningFileId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
