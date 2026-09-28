package cm.gimac.des.iam.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class RoleGrantCriteriaTest {

    @Test
    void newRoleGrantCriteriaHasAllFiltersNullTest() {
        var roleGrantCriteria = new RoleGrantCriteria();
        assertThat(roleGrantCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void roleGrantCriteriaFluentMethodsCreatesFiltersTest() {
        var roleGrantCriteria = new RoleGrantCriteria();

        setAllFilters(roleGrantCriteria);

        assertThat(roleGrantCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void roleGrantCriteriaCopyCreatesNullFilterTest() {
        var roleGrantCriteria = new RoleGrantCriteria();
        var copy = roleGrantCriteria.copy();

        assertThat(roleGrantCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(roleGrantCriteria)
        );
    }

    @Test
    void roleGrantCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var roleGrantCriteria = new RoleGrantCriteria();
        setAllFilters(roleGrantCriteria);

        var copy = roleGrantCriteria.copy();

        assertThat(roleGrantCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(roleGrantCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var roleGrantCriteria = new RoleGrantCriteria();

        assertThat(roleGrantCriteria).hasToString("RoleGrantCriteria{}");
    }

    private static void setAllFilters(RoleGrantCriteria roleGrantCriteria) {
        roleGrantCriteria.id();
        roleGrantCriteria.state();
        roleGrantCriteria.requestedBy();
        roleGrantCriteria.requestedAt();
        roleGrantCriteria.approvedBy();
        roleGrantCriteria.approvedAt();
        roleGrantCriteria.revokedAt();
        roleGrantCriteria.revocationReason();
        roleGrantCriteria.collaboratorId();
        roleGrantCriteria.roleId();
        roleGrantCriteria.distinct();
    }

    private static Condition<RoleGrantCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getRequestedBy()) &&
                condition.apply(criteria.getRequestedAt()) &&
                condition.apply(criteria.getApprovedBy()) &&
                condition.apply(criteria.getApprovedAt()) &&
                condition.apply(criteria.getRevokedAt()) &&
                condition.apply(criteria.getRevocationReason()) &&
                condition.apply(criteria.getCollaboratorId()) &&
                condition.apply(criteria.getRoleId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<RoleGrantCriteria> copyFiltersAre(RoleGrantCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getRequestedBy(), copy.getRequestedBy()) &&
                condition.apply(criteria.getRequestedAt(), copy.getRequestedAt()) &&
                condition.apply(criteria.getApprovedBy(), copy.getApprovedBy()) &&
                condition.apply(criteria.getApprovedAt(), copy.getApprovedAt()) &&
                condition.apply(criteria.getRevokedAt(), copy.getRevokedAt()) &&
                condition.apply(criteria.getRevocationReason(), copy.getRevocationReason()) &&
                condition.apply(criteria.getCollaboratorId(), copy.getCollaboratorId()) &&
                condition.apply(criteria.getRoleId(), copy.getRoleId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
