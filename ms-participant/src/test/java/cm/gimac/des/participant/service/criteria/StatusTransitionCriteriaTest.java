package cm.gimac.des.participant.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class StatusTransitionCriteriaTest {

    @Test
    void newStatusTransitionCriteriaHasAllFiltersNullTest() {
        var statusTransitionCriteria = new StatusTransitionCriteria();
        assertThat(statusTransitionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void statusTransitionCriteriaFluentMethodsCreatesFiltersTest() {
        var statusTransitionCriteria = new StatusTransitionCriteria();

        setAllFilters(statusTransitionCriteria);

        assertThat(statusTransitionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void statusTransitionCriteriaCopyCreatesNullFilterTest() {
        var statusTransitionCriteria = new StatusTransitionCriteria();
        var copy = statusTransitionCriteria.copy();

        assertThat(statusTransitionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(statusTransitionCriteria)
        );
    }

    @Test
    void statusTransitionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var statusTransitionCriteria = new StatusTransitionCriteria();
        setAllFilters(statusTransitionCriteria);

        var copy = statusTransitionCriteria.copy();

        assertThat(statusTransitionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(statusTransitionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var statusTransitionCriteria = new StatusTransitionCriteria();

        assertThat(statusTransitionCriteria).hasToString("StatusTransitionCriteria{}");
    }

    private static void setAllFilters(StatusTransitionCriteria statusTransitionCriteria) {
        statusTransitionCriteria.id();
        statusTransitionCriteria.fromStatus();
        statusTransitionCriteria.toStatus();
        statusTransitionCriteria.reason();
        statusTransitionCriteria.requesterId();
        statusTransitionCriteria.requestedAt();
        statusTransitionCriteria.automaticTransition();
        statusTransitionCriteria.requiredApprovals();
        statusTransitionCriteria.approverId();
        statusTransitionCriteria.secondApproverId();
        statusTransitionCriteria.secondApprovedAt();
        statusTransitionCriteria.decidedAt();
        statusTransitionCriteria.state();
        statusTransitionCriteria.rejectionReason();
        statusTransitionCriteria.evidenceDocumentId();
        statusTransitionCriteria.institutionId();
        statusTransitionCriteria.distinct();
    }

    private static Condition<StatusTransitionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getFromStatus()) &&
                condition.apply(criteria.getToStatus()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getRequesterId()) &&
                condition.apply(criteria.getRequestedAt()) &&
                condition.apply(criteria.getAutomaticTransition()) &&
                condition.apply(criteria.getRequiredApprovals()) &&
                condition.apply(criteria.getApproverId()) &&
                condition.apply(criteria.getSecondApproverId()) &&
                condition.apply(criteria.getSecondApprovedAt()) &&
                condition.apply(criteria.getDecidedAt()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getRejectionReason()) &&
                condition.apply(criteria.getEvidenceDocumentId()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<StatusTransitionCriteria> copyFiltersAre(
        StatusTransitionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getFromStatus(), copy.getFromStatus()) &&
                condition.apply(criteria.getToStatus(), copy.getToStatus()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getRequesterId(), copy.getRequesterId()) &&
                condition.apply(criteria.getRequestedAt(), copy.getRequestedAt()) &&
                condition.apply(criteria.getAutomaticTransition(), copy.getAutomaticTransition()) &&
                condition.apply(criteria.getRequiredApprovals(), copy.getRequiredApprovals()) &&
                condition.apply(criteria.getApproverId(), copy.getApproverId()) &&
                condition.apply(criteria.getSecondApproverId(), copy.getSecondApproverId()) &&
                condition.apply(criteria.getSecondApprovedAt(), copy.getSecondApprovedAt()) &&
                condition.apply(criteria.getDecidedAt(), copy.getDecidedAt()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getRejectionReason(), copy.getRejectionReason()) &&
                condition.apply(criteria.getEvidenceDocumentId(), copy.getEvidenceDocumentId()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
