package cm.gimac.des.certification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class QuitusCriteriaTest {

    @Test
    void newQuitusCriteriaHasAllFiltersNullTest() {
        var quitusCriteria = new QuitusCriteria();
        assertThat(quitusCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void quitusCriteriaFluentMethodsCreatesFiltersTest() {
        var quitusCriteria = new QuitusCriteria();

        setAllFilters(quitusCriteria);

        assertThat(quitusCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void quitusCriteriaCopyCreatesNullFilterTest() {
        var quitusCriteria = new QuitusCriteria();
        var copy = quitusCriteria.copy();

        assertThat(quitusCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(quitusCriteria)
        );
    }

    @Test
    void quitusCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var quitusCriteria = new QuitusCriteria();
        setAllFilters(quitusCriteria);

        var copy = quitusCriteria.copy();

        assertThat(quitusCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(quitusCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var quitusCriteria = new QuitusCriteria();

        assertThat(quitusCriteria).hasToString("QuitusCriteria{}");
    }

    private static void setAllFilters(QuitusCriteria quitusCriteria) {
        quitusCriteria.id();
        quitusCriteria.type();
        quitusCriteria.status();
        quitusCriteria.decision();
        quitusCriteria.signerId();
        quitusCriteria.reason();
        quitusCriteria.basisDigest();
        quitusCriteria.decisionDigest();
        quitusCriteria.credentialId();
        quitusCriteria.signCount();
        quitusCriteria.signedAt();
        quitusCriteria.dueAt();
        quitusCriteria.invalidatedAt();
        quitusCriteria.fileId();
        quitusCriteria.distinct();
    }

    private static Condition<QuitusCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getDecision()) &&
                condition.apply(criteria.getSignerId()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getBasisDigest()) &&
                condition.apply(criteria.getDecisionDigest()) &&
                condition.apply(criteria.getCredentialId()) &&
                condition.apply(criteria.getSignCount()) &&
                condition.apply(criteria.getSignedAt()) &&
                condition.apply(criteria.getDueAt()) &&
                condition.apply(criteria.getInvalidatedAt()) &&
                condition.apply(criteria.getFileId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<QuitusCriteria> copyFiltersAre(QuitusCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getDecision(), copy.getDecision()) &&
                condition.apply(criteria.getSignerId(), copy.getSignerId()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getBasisDigest(), copy.getBasisDigest()) &&
                condition.apply(criteria.getDecisionDigest(), copy.getDecisionDigest()) &&
                condition.apply(criteria.getCredentialId(), copy.getCredentialId()) &&
                condition.apply(criteria.getSignCount(), copy.getSignCount()) &&
                condition.apply(criteria.getSignedAt(), copy.getSignedAt()) &&
                condition.apply(criteria.getDueAt(), copy.getDueAt()) &&
                condition.apply(criteria.getInvalidatedAt(), copy.getInvalidatedAt()) &&
                condition.apply(criteria.getFileId(), copy.getFileId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
