package cm.gimac.des.iam.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DelegationCriteriaTest {

    @Test
    void newDelegationCriteriaHasAllFiltersNullTest() {
        var delegationCriteria = new DelegationCriteria();
        assertThat(delegationCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void delegationCriteriaFluentMethodsCreatesFiltersTest() {
        var delegationCriteria = new DelegationCriteria();

        setAllFilters(delegationCriteria);

        assertThat(delegationCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void delegationCriteriaCopyCreatesNullFilterTest() {
        var delegationCriteria = new DelegationCriteria();
        var copy = delegationCriteria.copy();

        assertThat(delegationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(delegationCriteria)
        );
    }

    @Test
    void delegationCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var delegationCriteria = new DelegationCriteria();
        setAllFilters(delegationCriteria);

        var copy = delegationCriteria.copy();

        assertThat(delegationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(delegationCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var delegationCriteria = new DelegationCriteria();

        assertThat(delegationCriteria).hasToString("DelegationCriteria{}");
    }

    private static void setAllFilters(DelegationCriteria delegationCriteria) {
        delegationCriteria.id();
        delegationCriteria.type();
        delegationCriteria.reason();
        delegationCriteria.delegatedLevel();
        delegationCriteria.requesterId();
        delegationCriteria.approverId();
        delegationCriteria.startsAt();
        delegationCriteria.endsAt();
        delegationCriteria.state();
        delegationCriteria.collaboratorId();
        delegationCriteria.distinct();
    }

    private static Condition<DelegationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getDelegatedLevel()) &&
                condition.apply(criteria.getRequesterId()) &&
                condition.apply(criteria.getApproverId()) &&
                condition.apply(criteria.getStartsAt()) &&
                condition.apply(criteria.getEndsAt()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getCollaboratorId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DelegationCriteria> copyFiltersAre(DelegationCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getDelegatedLevel(), copy.getDelegatedLevel()) &&
                condition.apply(criteria.getRequesterId(), copy.getRequesterId()) &&
                condition.apply(criteria.getApproverId(), copy.getApproverId()) &&
                condition.apply(criteria.getStartsAt(), copy.getStartsAt()) &&
                condition.apply(criteria.getEndsAt(), copy.getEndsAt()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getCollaboratorId(), copy.getCollaboratorId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
