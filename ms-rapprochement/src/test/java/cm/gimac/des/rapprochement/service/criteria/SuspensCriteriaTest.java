package cm.gimac.des.rapprochement.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class SuspensCriteriaTest {

    @Test
    void newSuspensCriteriaHasAllFiltersNullTest() {
        var suspensCriteria = new SuspensCriteria();
        assertThat(suspensCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void suspensCriteriaFluentMethodsCreatesFiltersTest() {
        var suspensCriteria = new SuspensCriteria();

        setAllFilters(suspensCriteria);

        assertThat(suspensCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void suspensCriteriaCopyCreatesNullFilterTest() {
        var suspensCriteria = new SuspensCriteria();
        var copy = suspensCriteria.copy();

        assertThat(suspensCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(suspensCriteria)
        );
    }

    @Test
    void suspensCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var suspensCriteria = new SuspensCriteria();
        setAllFilters(suspensCriteria);

        var copy = suspensCriteria.copy();

        assertThat(suspensCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(suspensCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var suspensCriteria = new SuspensCriteria();

        assertThat(suspensCriteria).hasToString("SuspensCriteria{}");
    }

    private static void setAllFilters(SuspensCriteria suspensCriteria) {
        suspensCriteria.id();
        suspensCriteria.reference();
        suspensCriteria.institutionId();
        suspensCriteria.type();
        suspensCriteria.pivotTransactionRef();
        suspensCriteria.pivotAuthorizationRef();
        suspensCriteria.pivotOperationAt();
        suspensCriteria.pivotAmount();
        suspensCriteria.pivotParticipantCode();
        suspensCriteria.amountSwitch();
        suspensCriteria.amountParticipant();
        suspensCriteria.state();
        suspensCriteria.assigneeId();
        suspensCriteria.dueDate();
        suspensCriteria.openedAt();
        suspensCriteria.disputeTicketId();
        suspensCriteria.closedAt();
        suspensCriteria.reconciliationId();
        suspensCriteria.regularizationAttestationId();
        suspensCriteria.distinct();
    }

    private static Condition<SuspensCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getPivotTransactionRef()) &&
                condition.apply(criteria.getPivotAuthorizationRef()) &&
                condition.apply(criteria.getPivotOperationAt()) &&
                condition.apply(criteria.getPivotAmount()) &&
                condition.apply(criteria.getPivotParticipantCode()) &&
                condition.apply(criteria.getAmountSwitch()) &&
                condition.apply(criteria.getAmountParticipant()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getAssigneeId()) &&
                condition.apply(criteria.getDueDate()) &&
                condition.apply(criteria.getOpenedAt()) &&
                condition.apply(criteria.getDisputeTicketId()) &&
                condition.apply(criteria.getClosedAt()) &&
                condition.apply(criteria.getReconciliationId()) &&
                condition.apply(criteria.getRegularizationAttestationId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<SuspensCriteria> copyFiltersAre(SuspensCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getPivotTransactionRef(), copy.getPivotTransactionRef()) &&
                condition.apply(criteria.getPivotAuthorizationRef(), copy.getPivotAuthorizationRef()) &&
                condition.apply(criteria.getPivotOperationAt(), copy.getPivotOperationAt()) &&
                condition.apply(criteria.getPivotAmount(), copy.getPivotAmount()) &&
                condition.apply(criteria.getPivotParticipantCode(), copy.getPivotParticipantCode()) &&
                condition.apply(criteria.getAmountSwitch(), copy.getAmountSwitch()) &&
                condition.apply(criteria.getAmountParticipant(), copy.getAmountParticipant()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getAssigneeId(), copy.getAssigneeId()) &&
                condition.apply(criteria.getDueDate(), copy.getDueDate()) &&
                condition.apply(criteria.getOpenedAt(), copy.getOpenedAt()) &&
                condition.apply(criteria.getDisputeTicketId(), copy.getDisputeTicketId()) &&
                condition.apply(criteria.getClosedAt(), copy.getClosedAt()) &&
                condition.apply(criteria.getReconciliationId(), copy.getReconciliationId()) &&
                condition.apply(criteria.getRegularizationAttestationId(), copy.getRegularizationAttestationId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
