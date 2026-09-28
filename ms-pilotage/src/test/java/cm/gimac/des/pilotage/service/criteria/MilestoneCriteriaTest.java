package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class MilestoneCriteriaTest {

    @Test
    void newMilestoneCriteriaHasAllFiltersNullTest() {
        var milestoneCriteria = new MilestoneCriteria();
        assertThat(milestoneCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void milestoneCriteriaFluentMethodsCreatesFiltersTest() {
        var milestoneCriteria = new MilestoneCriteria();

        setAllFilters(milestoneCriteria);

        assertThat(milestoneCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void milestoneCriteriaCopyCreatesNullFilterTest() {
        var milestoneCriteria = new MilestoneCriteria();
        var copy = milestoneCriteria.copy();

        assertThat(milestoneCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(milestoneCriteria)
        );
    }

    @Test
    void milestoneCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var milestoneCriteria = new MilestoneCriteria();
        setAllFilters(milestoneCriteria);

        var copy = milestoneCriteria.copy();

        assertThat(milestoneCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(milestoneCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var milestoneCriteria = new MilestoneCriteria();

        assertThat(milestoneCriteria).hasToString("MilestoneCriteria{}");
    }

    private static void setAllFilters(MilestoneCriteria milestoneCriteria) {
        milestoneCriteria.id();
        milestoneCriteria.label();
        milestoneCriteria.optimistic();
        milestoneCriteria.mostLikely();
        milestoneCriteria.pessimistic();
        milestoneCriteria.expectedDuration();
        milestoneCriteria.variance();
        milestoneCriteria.earliestStart();
        milestoneCriteria.latestStart();
        milestoneCriteria.totalFloat();
        milestoneCriteria.freeFloat();
        milestoneCriteria.critical();
        milestoneCriteria.state();
        milestoneCriteria.actualDuration();
        milestoneCriteria.reachedAt();
        milestoneCriteria.projectId();
        milestoneCriteria.predecessorsId();
        milestoneCriteria.successorsId();
        milestoneCriteria.distinct();
    }

    private static Condition<MilestoneCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getLabel()) &&
                condition.apply(criteria.getOptimistic()) &&
                condition.apply(criteria.getMostLikely()) &&
                condition.apply(criteria.getPessimistic()) &&
                condition.apply(criteria.getExpectedDuration()) &&
                condition.apply(criteria.getVariance()) &&
                condition.apply(criteria.getEarliestStart()) &&
                condition.apply(criteria.getLatestStart()) &&
                condition.apply(criteria.getTotalFloat()) &&
                condition.apply(criteria.getFreeFloat()) &&
                condition.apply(criteria.getCritical()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getActualDuration()) &&
                condition.apply(criteria.getReachedAt()) &&
                condition.apply(criteria.getProjectId()) &&
                condition.apply(criteria.getPredecessorsId()) &&
                condition.apply(criteria.getSuccessorsId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<MilestoneCriteria> copyFiltersAre(MilestoneCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getLabel(), copy.getLabel()) &&
                condition.apply(criteria.getOptimistic(), copy.getOptimistic()) &&
                condition.apply(criteria.getMostLikely(), copy.getMostLikely()) &&
                condition.apply(criteria.getPessimistic(), copy.getPessimistic()) &&
                condition.apply(criteria.getExpectedDuration(), copy.getExpectedDuration()) &&
                condition.apply(criteria.getVariance(), copy.getVariance()) &&
                condition.apply(criteria.getEarliestStart(), copy.getEarliestStart()) &&
                condition.apply(criteria.getLatestStart(), copy.getLatestStart()) &&
                condition.apply(criteria.getTotalFloat(), copy.getTotalFloat()) &&
                condition.apply(criteria.getFreeFloat(), copy.getFreeFloat()) &&
                condition.apply(criteria.getCritical(), copy.getCritical()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getActualDuration(), copy.getActualDuration()) &&
                condition.apply(criteria.getReachedAt(), copy.getReachedAt()) &&
                condition.apply(criteria.getProjectId(), copy.getProjectId()) &&
                condition.apply(criteria.getPredecessorsId(), copy.getPredecessorsId()) &&
                condition.apply(criteria.getSuccessorsId(), copy.getSuccessorsId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
