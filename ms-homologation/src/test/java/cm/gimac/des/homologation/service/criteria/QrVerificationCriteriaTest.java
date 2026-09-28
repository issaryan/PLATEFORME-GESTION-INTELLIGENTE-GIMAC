package cm.gimac.des.homologation.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class QrVerificationCriteriaTest {

    @Test
    void newQrVerificationCriteriaHasAllFiltersNullTest() {
        var qrVerificationCriteria = new QrVerificationCriteria();
        assertThat(qrVerificationCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void qrVerificationCriteriaFluentMethodsCreatesFiltersTest() {
        var qrVerificationCriteria = new QrVerificationCriteria();

        setAllFilters(qrVerificationCriteria);

        assertThat(qrVerificationCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void qrVerificationCriteriaCopyCreatesNullFilterTest() {
        var qrVerificationCriteria = new QrVerificationCriteria();
        var copy = qrVerificationCriteria.copy();

        assertThat(qrVerificationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(qrVerificationCriteria)
        );
    }

    @Test
    void qrVerificationCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var qrVerificationCriteria = new QrVerificationCriteria();
        setAllFilters(qrVerificationCriteria);

        var copy = qrVerificationCriteria.copy();

        assertThat(qrVerificationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(qrVerificationCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var qrVerificationCriteria = new QrVerificationCriteria();

        assertThat(qrVerificationCriteria).hasToString("QrVerificationCriteria{}");
    }

    private static void setAllFilters(QrVerificationCriteria qrVerificationCriteria) {
        qrVerificationCriteria.id();
        qrVerificationCriteria.form();
        qrVerificationCriteria.outcome();
        qrVerificationCriteria.verifiedOn();
        qrVerificationCriteria.verifierId();
        qrVerificationCriteria.evidenceDocumentId();
        qrVerificationCriteria.evidenceDigest();
        qrVerificationCriteria.evidenceSealedAt();
        qrVerificationCriteria.liaisonId();
        qrVerificationCriteria.operationId();
        qrVerificationCriteria.distinct();
    }

    private static Condition<QrVerificationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getForm()) &&
                condition.apply(criteria.getOutcome()) &&
                condition.apply(criteria.getVerifiedOn()) &&
                condition.apply(criteria.getVerifierId()) &&
                condition.apply(criteria.getEvidenceDocumentId()) &&
                condition.apply(criteria.getEvidenceDigest()) &&
                condition.apply(criteria.getEvidenceSealedAt()) &&
                condition.apply(criteria.getLiaisonId()) &&
                condition.apply(criteria.getOperationId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<QrVerificationCriteria> copyFiltersAre(
        QrVerificationCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getForm(), copy.getForm()) &&
                condition.apply(criteria.getOutcome(), copy.getOutcome()) &&
                condition.apply(criteria.getVerifiedOn(), copy.getVerifiedOn()) &&
                condition.apply(criteria.getVerifierId(), copy.getVerifierId()) &&
                condition.apply(criteria.getEvidenceDocumentId(), copy.getEvidenceDocumentId()) &&
                condition.apply(criteria.getEvidenceDigest(), copy.getEvidenceDigest()) &&
                condition.apply(criteria.getEvidenceSealedAt(), copy.getEvidenceSealedAt()) &&
                condition.apply(criteria.getLiaisonId(), copy.getLiaisonId()) &&
                condition.apply(criteria.getOperationId(), copy.getOperationId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
