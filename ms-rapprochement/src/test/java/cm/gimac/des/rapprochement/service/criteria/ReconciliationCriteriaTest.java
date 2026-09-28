package cm.gimac.des.rapprochement.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ReconciliationCriteriaTest {

    @Test
    void newReconciliationCriteriaHasAllFiltersNullTest() {
        var reconciliationCriteria = new ReconciliationCriteria();
        assertThat(reconciliationCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void reconciliationCriteriaFluentMethodsCreatesFiltersTest() {
        var reconciliationCriteria = new ReconciliationCriteria();

        setAllFilters(reconciliationCriteria);

        assertThat(reconciliationCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void reconciliationCriteriaCopyCreatesNullFilterTest() {
        var reconciliationCriteria = new ReconciliationCriteria();
        var copy = reconciliationCriteria.copy();

        assertThat(reconciliationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(reconciliationCriteria)
        );
    }

    @Test
    void reconciliationCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var reconciliationCriteria = new ReconciliationCriteria();
        setAllFilters(reconciliationCriteria);

        var copy = reconciliationCriteria.copy();

        assertThat(reconciliationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(reconciliationCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var reconciliationCriteria = new ReconciliationCriteria();

        assertThat(reconciliationCriteria).hasToString("ReconciliationCriteria{}");
    }

    private static void setAllFilters(ReconciliationCriteria reconciliationCriteria) {
        reconciliationCriteria.id();
        reconciliationCriteria.reference();
        reconciliationCriteria.businessDate();
        reconciliationCriteria.institutionId();
        reconciliationCriteria.state();
        reconciliationCriteria.timestampTolerance();
        reconciliationCriteria.matchedCount();
        reconciliationCriteria.discrepancyCount();
        reconciliationCriteria.startedAt();
        reconciliationCriteria.completedAt();
        reconciliationCriteria.closedBy();
        reconciliationCriteria.closedAt();
        reconciliationCriteria.switchStatementId();
        reconciliationCriteria.participantStatementId();
        reconciliationCriteria.settlementStatementId();
        reconciliationCriteria.distinct();
    }

    private static Condition<ReconciliationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getBusinessDate()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getTimestampTolerance()) &&
                condition.apply(criteria.getMatchedCount()) &&
                condition.apply(criteria.getDiscrepancyCount()) &&
                condition.apply(criteria.getStartedAt()) &&
                condition.apply(criteria.getCompletedAt()) &&
                condition.apply(criteria.getClosedBy()) &&
                condition.apply(criteria.getClosedAt()) &&
                condition.apply(criteria.getSwitchStatementId()) &&
                condition.apply(criteria.getParticipantStatementId()) &&
                condition.apply(criteria.getSettlementStatementId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ReconciliationCriteria> copyFiltersAre(
        ReconciliationCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getBusinessDate(), copy.getBusinessDate()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getTimestampTolerance(), copy.getTimestampTolerance()) &&
                condition.apply(criteria.getMatchedCount(), copy.getMatchedCount()) &&
                condition.apply(criteria.getDiscrepancyCount(), copy.getDiscrepancyCount()) &&
                condition.apply(criteria.getStartedAt(), copy.getStartedAt()) &&
                condition.apply(criteria.getCompletedAt(), copy.getCompletedAt()) &&
                condition.apply(criteria.getClosedBy(), copy.getClosedBy()) &&
                condition.apply(criteria.getClosedAt(), copy.getClosedAt()) &&
                condition.apply(criteria.getSwitchStatementId(), copy.getSwitchStatementId()) &&
                condition.apply(criteria.getParticipantStatementId(), copy.getParticipantStatementId()) &&
                condition.apply(criteria.getSettlementStatementId(), copy.getSettlementStatementId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
