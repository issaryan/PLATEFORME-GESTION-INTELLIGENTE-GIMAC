package cm.gimac.des.homologation.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class OperationCriteriaTest {

    @Test
    void newOperationCriteriaHasAllFiltersNullTest() {
        var operationCriteria = new OperationCriteria();
        assertThat(operationCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void operationCriteriaFluentMethodsCreatesFiltersTest() {
        var operationCriteria = new OperationCriteria();

        setAllFilters(operationCriteria);

        assertThat(operationCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void operationCriteriaCopyCreatesNullFilterTest() {
        var operationCriteria = new OperationCriteria();
        var copy = operationCriteria.copy();

        assertThat(operationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(operationCriteria)
        );
    }

    @Test
    void operationCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var operationCriteria = new OperationCriteria();
        setAllFilters(operationCriteria);

        var copy = operationCriteria.copy();

        assertThat(operationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(operationCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var operationCriteria = new OperationCriteria();

        assertThat(operationCriteria).hasToString("OperationCriteria{}");
    }

    private static void setAllFilters(OperationCriteria operationCriteria) {
        operationCriteria.id();
        operationCriteria.operationCode();
        operationCriteria.state();
        operationCriteria.ownerId();
        operationCriteria.evidenceDocumentId();
        operationCriteria.evidenceDigest();
        operationCriteria.evidenceSealedAt();
        operationCriteria.evidenceRetention();
        operationCriteria.evidenceSealConfirmed();
        operationCriteria.evidenceTraceRef();
        operationCriteria.timestampImprint();
        operationCriteria.timestampGenTime();
        operationCriteria.validatorId();
        operationCriteria.validatedAt();
        operationCriteria.closedAt();
        operationCriteria.stateReason();
        operationCriteria.liaisonId();
        operationCriteria.referenceId();
        operationCriteria.distinct();
    }

    private static Condition<OperationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getOperationCode()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getEvidenceDocumentId()) &&
                condition.apply(criteria.getEvidenceDigest()) &&
                condition.apply(criteria.getEvidenceSealedAt()) &&
                condition.apply(criteria.getEvidenceRetention()) &&
                condition.apply(criteria.getEvidenceSealConfirmed()) &&
                condition.apply(criteria.getEvidenceTraceRef()) &&
                condition.apply(criteria.getTimestampImprint()) &&
                condition.apply(criteria.getTimestampGenTime()) &&
                condition.apply(criteria.getValidatorId()) &&
                condition.apply(criteria.getValidatedAt()) &&
                condition.apply(criteria.getClosedAt()) &&
                condition.apply(criteria.getStateReason()) &&
                condition.apply(criteria.getLiaisonId()) &&
                condition.apply(criteria.getReferenceId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<OperationCriteria> copyFiltersAre(OperationCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getOperationCode(), copy.getOperationCode()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getEvidenceDocumentId(), copy.getEvidenceDocumentId()) &&
                condition.apply(criteria.getEvidenceDigest(), copy.getEvidenceDigest()) &&
                condition.apply(criteria.getEvidenceSealedAt(), copy.getEvidenceSealedAt()) &&
                condition.apply(criteria.getEvidenceRetention(), copy.getEvidenceRetention()) &&
                condition.apply(criteria.getEvidenceSealConfirmed(), copy.getEvidenceSealConfirmed()) &&
                condition.apply(criteria.getEvidenceTraceRef(), copy.getEvidenceTraceRef()) &&
                condition.apply(criteria.getTimestampImprint(), copy.getTimestampImprint()) &&
                condition.apply(criteria.getTimestampGenTime(), copy.getTimestampGenTime()) &&
                condition.apply(criteria.getValidatorId(), copy.getValidatorId()) &&
                condition.apply(criteria.getValidatedAt(), copy.getValidatedAt()) &&
                condition.apply(criteria.getClosedAt(), copy.getClosedAt()) &&
                condition.apply(criteria.getStateReason(), copy.getStateReason()) &&
                condition.apply(criteria.getLiaisonId(), copy.getLiaisonId()) &&
                condition.apply(criteria.getReferenceId(), copy.getReferenceId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
