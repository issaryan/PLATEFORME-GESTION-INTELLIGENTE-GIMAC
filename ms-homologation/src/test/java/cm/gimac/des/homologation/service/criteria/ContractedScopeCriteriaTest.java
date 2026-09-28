package cm.gimac.des.homologation.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ContractedScopeCriteriaTest {

    @Test
    void newContractedScopeCriteriaHasAllFiltersNullTest() {
        var contractedScopeCriteria = new ContractedScopeCriteria();
        assertThat(contractedScopeCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void contractedScopeCriteriaFluentMethodsCreatesFiltersTest() {
        var contractedScopeCriteria = new ContractedScopeCriteria();

        setAllFilters(contractedScopeCriteria);

        assertThat(contractedScopeCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void contractedScopeCriteriaCopyCreatesNullFilterTest() {
        var contractedScopeCriteria = new ContractedScopeCriteria();
        var copy = contractedScopeCriteria.copy();

        assertThat(contractedScopeCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(contractedScopeCriteria)
        );
    }

    @Test
    void contractedScopeCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var contractedScopeCriteria = new ContractedScopeCriteria();
        setAllFilters(contractedScopeCriteria);

        var copy = contractedScopeCriteria.copy();

        assertThat(contractedScopeCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(contractedScopeCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var contractedScopeCriteria = new ContractedScopeCriteria();

        assertThat(contractedScopeCriteria).hasToString("ContractedScopeCriteria{}");
    }

    private static void setAllFilters(ContractedScopeCriteria contractedScopeCriteria) {
        contractedScopeCriteria.id();
        contractedScopeCriteria.serviceCode();
        contractedScopeCriteria.included();
        contractedScopeCriteria.progress();
        contractedScopeCriteria.updatedAt();
        contractedScopeCriteria.liaisonId();
        contractedScopeCriteria.distinct();
    }

    private static Condition<ContractedScopeCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getServiceCode()) &&
                condition.apply(criteria.getIncluded()) &&
                condition.apply(criteria.getProgress()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getLiaisonId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ContractedScopeCriteria> copyFiltersAre(
        ContractedScopeCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getServiceCode(), copy.getServiceCode()) &&
                condition.apply(criteria.getIncluded(), copy.getIncluded()) &&
                condition.apply(criteria.getProgress(), copy.getProgress()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getLiaisonId(), copy.getLiaisonId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
