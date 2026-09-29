package cm.gimac.des.participant.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class SubscribedServiceCriteriaTest {

    @Test
    void newSubscribedServiceCriteriaHasAllFiltersNullTest() {
        var subscribedServiceCriteria = new SubscribedServiceCriteria();
        assertThat(subscribedServiceCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void subscribedServiceCriteriaFluentMethodsCreatesFiltersTest() {
        var subscribedServiceCriteria = new SubscribedServiceCriteria();

        setAllFilters(subscribedServiceCriteria);

        assertThat(subscribedServiceCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void subscribedServiceCriteriaCopyCreatesNullFilterTest() {
        var subscribedServiceCriteria = new SubscribedServiceCriteria();
        var copy = subscribedServiceCriteria.copy();

        assertThat(subscribedServiceCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(subscribedServiceCriteria)
        );
    }

    @Test
    void subscribedServiceCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var subscribedServiceCriteria = new SubscribedServiceCriteria();
        setAllFilters(subscribedServiceCriteria);

        var copy = subscribedServiceCriteria.copy();

        assertThat(subscribedServiceCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(subscribedServiceCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var subscribedServiceCriteria = new SubscribedServiceCriteria();

        assertThat(subscribedServiceCriteria).hasToString("SubscribedServiceCriteria{}");
    }

    private static void setAllFilters(SubscribedServiceCriteria subscribedServiceCriteria) {
        subscribedServiceCriteria.id();
        subscribedServiceCriteria.serviceCode();
        subscribedServiceCriteria.subscribedOn();
        subscribedServiceCriteria.terminatedOn();
        subscribedServiceCriteria.state();
        subscribedServiceCriteria.institutionId();
        subscribedServiceCriteria.distinct();
    }

    private static Condition<SubscribedServiceCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getSubscribedOn()) &&
                condition.apply(criteria.getTerminatedOn()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<SubscribedServiceCriteria> copyFiltersAre(
        SubscribedServiceCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getSubscribedOn(), copy.getSubscribedOn()) &&
                condition.apply(criteria.getTerminatedOn(), copy.getTerminatedOn()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
