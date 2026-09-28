package cm.gimac.des.reporting.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DimensionCriteriaTest {

    @Test
    void newDimensionCriteriaHasAllFiltersNullTest() {
        var dimensionCriteria = new DimensionCriteria();
        assertThat(dimensionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void dimensionCriteriaFluentMethodsCreatesFiltersTest() {
        var dimensionCriteria = new DimensionCriteria();

        setAllFilters(dimensionCriteria);

        assertThat(dimensionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void dimensionCriteriaCopyCreatesNullFilterTest() {
        var dimensionCriteria = new DimensionCriteria();
        var copy = dimensionCriteria.copy();

        assertThat(dimensionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(dimensionCriteria)
        );
    }

    @Test
    void dimensionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var dimensionCriteria = new DimensionCriteria();
        setAllFilters(dimensionCriteria);

        var copy = dimensionCriteria.copy();

        assertThat(dimensionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(dimensionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var dimensionCriteria = new DimensionCriteria();

        assertThat(dimensionCriteria).hasToString("DimensionCriteria{}");
    }

    private static void setAllFilters(DimensionCriteria dimensionCriteria) {
        dimensionCriteria.id();
        dimensionCriteria.kind();
        dimensionCriteria.key();
        dimensionCriteria.label();
        dimensionCriteria.parentKey();
        dimensionCriteria.updatedAt();
        dimensionCriteria.distinct();
    }

    private static Condition<DimensionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getKind()) &&
                condition.apply(criteria.getKey()) &&
                condition.apply(criteria.getLabel()) &&
                condition.apply(criteria.getParentKey()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DimensionCriteria> copyFiltersAre(DimensionCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getKind(), copy.getKind()) &&
                condition.apply(criteria.getKey(), copy.getKey()) &&
                condition.apply(criteria.getLabel(), copy.getLabel()) &&
                condition.apply(criteria.getParentKey(), copy.getParentKey()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
