package cm.gimac.des.certification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ClosedOperationSnapshotCriteriaTest {

    @Test
    void newClosedOperationSnapshotCriteriaHasAllFiltersNullTest() {
        var closedOperationSnapshotCriteria = new ClosedOperationSnapshotCriteria();
        assertThat(closedOperationSnapshotCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void closedOperationSnapshotCriteriaFluentMethodsCreatesFiltersTest() {
        var closedOperationSnapshotCriteria = new ClosedOperationSnapshotCriteria();

        setAllFilters(closedOperationSnapshotCriteria);

        assertThat(closedOperationSnapshotCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void closedOperationSnapshotCriteriaCopyCreatesNullFilterTest() {
        var closedOperationSnapshotCriteria = new ClosedOperationSnapshotCriteria();
        var copy = closedOperationSnapshotCriteria.copy();

        assertThat(closedOperationSnapshotCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(closedOperationSnapshotCriteria)
        );
    }

    @Test
    void closedOperationSnapshotCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var closedOperationSnapshotCriteria = new ClosedOperationSnapshotCriteria();
        setAllFilters(closedOperationSnapshotCriteria);

        var copy = closedOperationSnapshotCriteria.copy();

        assertThat(closedOperationSnapshotCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(closedOperationSnapshotCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var closedOperationSnapshotCriteria = new ClosedOperationSnapshotCriteria();

        assertThat(closedOperationSnapshotCriteria).hasToString("ClosedOperationSnapshotCriteria{}");
    }

    private static void setAllFilters(ClosedOperationSnapshotCriteria closedOperationSnapshotCriteria) {
        closedOperationSnapshotCriteria.id();
        closedOperationSnapshotCriteria.liaisonId();
        closedOperationSnapshotCriteria.operationId();
        closedOperationSnapshotCriteria.operationCode();
        closedOperationSnapshotCriteria.referenceCode();
        closedOperationSnapshotCriteria.serviceCode();
        closedOperationSnapshotCriteria.ownerId();
        closedOperationSnapshotCriteria.validatorId();
        closedOperationSnapshotCriteria.evidenceDocumentId();
        closedOperationSnapshotCriteria.evidenceDigest();
        closedOperationSnapshotCriteria.closedAt();
        closedOperationSnapshotCriteria.sourceEventId();
        closedOperationSnapshotCriteria.distinct();
    }

    private static Condition<ClosedOperationSnapshotCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getLiaisonId()) &&
                condition.apply(criteria.getOperationId()) &&
                condition.apply(criteria.getOperationCode()) &&
                condition.apply(criteria.getReferenceCode()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getValidatorId()) &&
                condition.apply(criteria.getEvidenceDocumentId()) &&
                condition.apply(criteria.getEvidenceDigest()) &&
                condition.apply(criteria.getClosedAt()) &&
                condition.apply(criteria.getSourceEventId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ClosedOperationSnapshotCriteria> copyFiltersAre(
        ClosedOperationSnapshotCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getLiaisonId(), copy.getLiaisonId()) &&
                condition.apply(criteria.getOperationId(), copy.getOperationId()) &&
                condition.apply(criteria.getOperationCode(), copy.getOperationCode()) &&
                condition.apply(criteria.getReferenceCode(), copy.getReferenceCode()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getValidatorId(), copy.getValidatorId()) &&
                condition.apply(criteria.getEvidenceDocumentId(), copy.getEvidenceDocumentId()) &&
                condition.apply(criteria.getEvidenceDigest(), copy.getEvidenceDigest()) &&
                condition.apply(criteria.getClosedAt(), copy.getClosedAt()) &&
                condition.apply(criteria.getSourceEventId(), copy.getSourceEventId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
