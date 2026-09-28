package cm.gimac.des.audit.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class VerificationRecordCriteriaTest {

    @Test
    void newVerificationRecordCriteriaHasAllFiltersNullTest() {
        var verificationRecordCriteria = new VerificationRecordCriteria();
        assertThat(verificationRecordCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void verificationRecordCriteriaFluentMethodsCreatesFiltersTest() {
        var verificationRecordCriteria = new VerificationRecordCriteria();

        setAllFilters(verificationRecordCriteria);

        assertThat(verificationRecordCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void verificationRecordCriteriaCopyCreatesNullFilterTest() {
        var verificationRecordCriteria = new VerificationRecordCriteria();
        var copy = verificationRecordCriteria.copy();

        assertThat(verificationRecordCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(verificationRecordCriteria)
        );
    }

    @Test
    void verificationRecordCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var verificationRecordCriteria = new VerificationRecordCriteria();
        setAllFilters(verificationRecordCriteria);

        var copy = verificationRecordCriteria.copy();

        assertThat(verificationRecordCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(verificationRecordCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var verificationRecordCriteria = new VerificationRecordCriteria();

        assertThat(verificationRecordCriteria).hasToString("VerificationRecordCriteria{}");
    }

    private static void setAllFilters(VerificationRecordCriteria verificationRecordCriteria) {
        verificationRecordCriteria.id();
        verificationRecordCriteria.verifierId();
        verificationRecordCriteria.verifiedAt();
        verificationRecordCriteria.expectedRoot();
        verificationRecordCriteria.recomputedRoot();
        verificationRecordCriteria.tokenValid();
        verificationRecordCriteria.result();
        verificationRecordCriteria.comment();
        verificationRecordCriteria.proofId();
        verificationRecordCriteria.distinct();
    }

    private static Condition<VerificationRecordCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getVerifierId()) &&
                condition.apply(criteria.getVerifiedAt()) &&
                condition.apply(criteria.getExpectedRoot()) &&
                condition.apply(criteria.getRecomputedRoot()) &&
                condition.apply(criteria.getTokenValid()) &&
                condition.apply(criteria.getResult()) &&
                condition.apply(criteria.getComment()) &&
                condition.apply(criteria.getProofId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<VerificationRecordCriteria> copyFiltersAre(
        VerificationRecordCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getVerifierId(), copy.getVerifierId()) &&
                condition.apply(criteria.getVerifiedAt(), copy.getVerifiedAt()) &&
                condition.apply(criteria.getExpectedRoot(), copy.getExpectedRoot()) &&
                condition.apply(criteria.getRecomputedRoot(), copy.getRecomputedRoot()) &&
                condition.apply(criteria.getTokenValid(), copy.getTokenValid()) &&
                condition.apply(criteria.getResult(), copy.getResult()) &&
                condition.apply(criteria.getComment(), copy.getComment()) &&
                condition.apply(criteria.getProofId(), copy.getProofId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
