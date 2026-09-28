package cm.gimac.des.iam.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class UserSessionCriteriaTest {

    @Test
    void newUserSessionCriteriaHasAllFiltersNullTest() {
        var userSessionCriteria = new UserSessionCriteria();
        assertThat(userSessionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void userSessionCriteriaFluentMethodsCreatesFiltersTest() {
        var userSessionCriteria = new UserSessionCriteria();

        setAllFilters(userSessionCriteria);

        assertThat(userSessionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void userSessionCriteriaCopyCreatesNullFilterTest() {
        var userSessionCriteria = new UserSessionCriteria();
        var copy = userSessionCriteria.copy();

        assertThat(userSessionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(userSessionCriteria)
        );
    }

    @Test
    void userSessionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var userSessionCriteria = new UserSessionCriteria();
        setAllFilters(userSessionCriteria);

        var copy = userSessionCriteria.copy();

        assertThat(userSessionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(userSessionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var userSessionCriteria = new UserSessionCriteria();

        assertThat(userSessionCriteria).hasToString("UserSessionCriteria{}");
    }

    private static void setAllFilters(UserSessionCriteria userSessionCriteria) {
        userSessionCriteria.id();
        userSessionCriteria.certificateThumbprint();
        userSessionCriteria.authMethods();
        userSessionCriteria.issuedAt();
        userSessionCriteria.expiresAt();
        userSessionCriteria.state();
        userSessionCriteria.revokedAt();
        userSessionCriteria.revocationReason();
        userSessionCriteria.collaboratorId();
        userSessionCriteria.distinct();
    }

    private static Condition<UserSessionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getCertificateThumbprint()) &&
                condition.apply(criteria.getAuthMethods()) &&
                condition.apply(criteria.getIssuedAt()) &&
                condition.apply(criteria.getExpiresAt()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getRevokedAt()) &&
                condition.apply(criteria.getRevocationReason()) &&
                condition.apply(criteria.getCollaboratorId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<UserSessionCriteria> copyFiltersAre(UserSessionCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getCertificateThumbprint(), copy.getCertificateThumbprint()) &&
                condition.apply(criteria.getAuthMethods(), copy.getAuthMethods()) &&
                condition.apply(criteria.getIssuedAt(), copy.getIssuedAt()) &&
                condition.apply(criteria.getExpiresAt(), copy.getExpiresAt()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getRevokedAt(), copy.getRevokedAt()) &&
                condition.apply(criteria.getRevocationReason(), copy.getRevocationReason()) &&
                condition.apply(criteria.getCollaboratorId(), copy.getCollaboratorId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
