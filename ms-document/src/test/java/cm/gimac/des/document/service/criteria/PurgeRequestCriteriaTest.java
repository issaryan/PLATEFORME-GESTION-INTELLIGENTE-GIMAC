package cm.gimac.des.document.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class PurgeRequestCriteriaTest {

    @Test
    void newPurgeRequestCriteriaHasAllFiltersNullTest() {
        var purgeRequestCriteria = new PurgeRequestCriteria();
        assertThat(purgeRequestCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void purgeRequestCriteriaFluentMethodsCreatesFiltersTest() {
        var purgeRequestCriteria = new PurgeRequestCriteria();

        setAllFilters(purgeRequestCriteria);

        assertThat(purgeRequestCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void purgeRequestCriteriaCopyCreatesNullFilterTest() {
        var purgeRequestCriteria = new PurgeRequestCriteria();
        var copy = purgeRequestCriteria.copy();

        assertThat(purgeRequestCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(purgeRequestCriteria)
        );
    }

    @Test
    void purgeRequestCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var purgeRequestCriteria = new PurgeRequestCriteria();
        setAllFilters(purgeRequestCriteria);

        var copy = purgeRequestCriteria.copy();

        assertThat(purgeRequestCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(purgeRequestCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var purgeRequestCriteria = new PurgeRequestCriteria();

        assertThat(purgeRequestCriteria).hasToString("PurgeRequestCriteria{}");
    }

    private static void setAllFilters(PurgeRequestCriteria purgeRequestCriteria) {
        purgeRequestCriteria.id();
        purgeRequestCriteria.requesterId();
        purgeRequestCriteria.reason();
        purgeRequestCriteria.requestedAt();
        purgeRequestCriteria.approverId();
        purgeRequestCriteria.approvedAt();
        purgeRequestCriteria.state();
        purgeRequestCriteria.rejectionReason();
        purgeRequestCriteria.executedAt();
        purgeRequestCriteria.documentId();
        purgeRequestCriteria.distinct();
    }

    private static Condition<PurgeRequestCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getRequesterId()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getRequestedAt()) &&
                condition.apply(criteria.getApproverId()) &&
                condition.apply(criteria.getApprovedAt()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getRejectionReason()) &&
                condition.apply(criteria.getExecutedAt()) &&
                condition.apply(criteria.getDocumentId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<PurgeRequestCriteria> copyFiltersAre(
        PurgeRequestCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getRequesterId(), copy.getRequesterId()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getRequestedAt(), copy.getRequestedAt()) &&
                condition.apply(criteria.getApproverId(), copy.getApproverId()) &&
                condition.apply(criteria.getApprovedAt(), copy.getApprovedAt()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getRejectionReason(), copy.getRejectionReason()) &&
                condition.apply(criteria.getExecutedAt(), copy.getExecutedAt()) &&
                condition.apply(criteria.getDocumentId(), copy.getDocumentId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
