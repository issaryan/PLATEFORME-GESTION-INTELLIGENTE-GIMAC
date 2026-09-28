package cm.gimac.des.participant.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CorrespondentCriteriaTest {

    @Test
    void newCorrespondentCriteriaHasAllFiltersNullTest() {
        var correspondentCriteria = new CorrespondentCriteria();
        assertThat(correspondentCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void correspondentCriteriaFluentMethodsCreatesFiltersTest() {
        var correspondentCriteria = new CorrespondentCriteria();

        setAllFilters(correspondentCriteria);

        assertThat(correspondentCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void correspondentCriteriaCopyCreatesNullFilterTest() {
        var correspondentCriteria = new CorrespondentCriteria();
        var copy = correspondentCriteria.copy();

        assertThat(correspondentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(correspondentCriteria)
        );
    }

    @Test
    void correspondentCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var correspondentCriteria = new CorrespondentCriteria();
        setAllFilters(correspondentCriteria);

        var copy = correspondentCriteria.copy();

        assertThat(correspondentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(correspondentCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var correspondentCriteria = new CorrespondentCriteria();

        assertThat(correspondentCriteria).hasToString("CorrespondentCriteria{}");
    }

    private static void setAllFilters(CorrespondentCriteria correspondentCriteria) {
        correspondentCriteria.id();
        correspondentCriteria.fullName();
        correspondentCriteria.duty();
        correspondentCriteria.channels();
        correspondentCriteria.email();
        correspondentCriteria.phone();
        correspondentCriteria.publicKeyRef();
        correspondentCriteria.reachability();
        correspondentCriteria.lastReachabilityCheckAt();
        correspondentCriteria.onCall247();
        correspondentCriteria.userId();
        correspondentCriteria.active();
        correspondentCriteria.proposedBy();
        correspondentCriteria.validatedBy();
        correspondentCriteria.validatedAt();
        correspondentCriteria.institutionId();
        correspondentCriteria.distinct();
    }

    private static Condition<CorrespondentCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getFullName()) &&
                condition.apply(criteria.getDuty()) &&
                condition.apply(criteria.getChannels()) &&
                condition.apply(criteria.getEmail()) &&
                condition.apply(criteria.getPhone()) &&
                condition.apply(criteria.getPublicKeyRef()) &&
                condition.apply(criteria.getReachability()) &&
                condition.apply(criteria.getLastReachabilityCheckAt()) &&
                condition.apply(criteria.getOnCall247()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getActive()) &&
                condition.apply(criteria.getProposedBy()) &&
                condition.apply(criteria.getValidatedBy()) &&
                condition.apply(criteria.getValidatedAt()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CorrespondentCriteria> copyFiltersAre(
        CorrespondentCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getFullName(), copy.getFullName()) &&
                condition.apply(criteria.getDuty(), copy.getDuty()) &&
                condition.apply(criteria.getChannels(), copy.getChannels()) &&
                condition.apply(criteria.getEmail(), copy.getEmail()) &&
                condition.apply(criteria.getPhone(), copy.getPhone()) &&
                condition.apply(criteria.getPublicKeyRef(), copy.getPublicKeyRef()) &&
                condition.apply(criteria.getReachability(), copy.getReachability()) &&
                condition.apply(criteria.getLastReachabilityCheckAt(), copy.getLastReachabilityCheckAt()) &&
                condition.apply(criteria.getOnCall247(), copy.getOnCall247()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getActive(), copy.getActive()) &&
                condition.apply(criteria.getProposedBy(), copy.getProposedBy()) &&
                condition.apply(criteria.getValidatedBy(), copy.getValidatedBy()) &&
                condition.apply(criteria.getValidatedAt(), copy.getValidatedAt()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
