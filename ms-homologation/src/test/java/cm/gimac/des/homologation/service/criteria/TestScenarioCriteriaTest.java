package cm.gimac.des.homologation.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class TestScenarioCriteriaTest {

    @Test
    void newTestScenarioCriteriaHasAllFiltersNullTest() {
        var testScenarioCriteria = new TestScenarioCriteria();
        assertThat(testScenarioCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void testScenarioCriteriaFluentMethodsCreatesFiltersTest() {
        var testScenarioCriteria = new TestScenarioCriteria();

        setAllFilters(testScenarioCriteria);

        assertThat(testScenarioCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void testScenarioCriteriaCopyCreatesNullFilterTest() {
        var testScenarioCriteria = new TestScenarioCriteria();
        var copy = testScenarioCriteria.copy();

        assertThat(testScenarioCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(testScenarioCriteria)
        );
    }

    @Test
    void testScenarioCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var testScenarioCriteria = new TestScenarioCriteria();
        setAllFilters(testScenarioCriteria);

        var copy = testScenarioCriteria.copy();

        assertThat(testScenarioCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(testScenarioCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var testScenarioCriteria = new TestScenarioCriteria();

        assertThat(testScenarioCriteria).hasToString("TestScenarioCriteria{}");
    }

    private static void setAllFilters(TestScenarioCriteria testScenarioCriteria) {
        testScenarioCriteria.id();
        testScenarioCriteria.simulatorScenarioId();
        testScenarioCriteria.outcome();
        testScenarioCriteria.reason();
        testScenarioCriteria.executedAt();
        testScenarioCriteria.traceRef();
        testScenarioCriteria.replayedAt();
        testScenarioCriteria.match();
        testScenarioCriteria.liaisonId();
        testScenarioCriteria.operationId();
        testScenarioCriteria.distinct();
    }

    private static Condition<TestScenarioCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getSimulatorScenarioId()) &&
                condition.apply(criteria.getOutcome()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getExecutedAt()) &&
                condition.apply(criteria.getTraceRef()) &&
                condition.apply(criteria.getReplayedAt()) &&
                condition.apply(criteria.getMatch()) &&
                condition.apply(criteria.getLiaisonId()) &&
                condition.apply(criteria.getOperationId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<TestScenarioCriteria> copyFiltersAre(
        TestScenarioCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getSimulatorScenarioId(), copy.getSimulatorScenarioId()) &&
                condition.apply(criteria.getOutcome(), copy.getOutcome()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getExecutedAt(), copy.getExecutedAt()) &&
                condition.apply(criteria.getTraceRef(), copy.getTraceRef()) &&
                condition.apply(criteria.getReplayedAt(), copy.getReplayedAt()) &&
                condition.apply(criteria.getMatch(), copy.getMatch()) &&
                condition.apply(criteria.getLiaisonId(), copy.getLiaisonId()) &&
                condition.apply(criteria.getOperationId(), copy.getOperationId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
