package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ActionSheetCriteriaTest {

    @Test
    void newActionSheetCriteriaHasAllFiltersNullTest() {
        var actionSheetCriteria = new ActionSheetCriteria();
        assertThat(actionSheetCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void actionSheetCriteriaFluentMethodsCreatesFiltersTest() {
        var actionSheetCriteria = new ActionSheetCriteria();

        setAllFilters(actionSheetCriteria);

        assertThat(actionSheetCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void actionSheetCriteriaCopyCreatesNullFilterTest() {
        var actionSheetCriteria = new ActionSheetCriteria();
        var copy = actionSheetCriteria.copy();

        assertThat(actionSheetCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(actionSheetCriteria)
        );
    }

    @Test
    void actionSheetCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var actionSheetCriteria = new ActionSheetCriteria();
        setAllFilters(actionSheetCriteria);

        var copy = actionSheetCriteria.copy();

        assertThat(actionSheetCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(actionSheetCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var actionSheetCriteria = new ActionSheetCriteria();

        assertThat(actionSheetCriteria).hasToString("ActionSheetCriteria{}");
    }

    private static void setAllFilters(ActionSheetCriteria actionSheetCriteria) {
        actionSheetCriteria.id();
        actionSheetCriteria.originKind();
        actionSheetCriteria.originReference();
        actionSheetCriteria.originService();
        actionSheetCriteria.phase();
        actionSheetCriteria.objective();
        actionSheetCriteria.causeAnalysis();
        actionSheetCriteria.targetScore();
        actionSheetCriteria.ownerId();
        actionSheetCriteria.dueDate();
        actionSheetCriteria.observationEndDate();
        actionSheetCriteria.result();
        actionSheetCriteria.cycle();
        actionSheetCriteria.procedureDocumentRef();
        actionSheetCriteria.closedAt();
        actionSheetCriteria.riskId();
        actionSheetCriteria.distinct();
    }

    private static Condition<ActionSheetCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getOriginKind()) &&
                condition.apply(criteria.getOriginReference()) &&
                condition.apply(criteria.getOriginService()) &&
                condition.apply(criteria.getPhase()) &&
                condition.apply(criteria.getObjective()) &&
                condition.apply(criteria.getCauseAnalysis()) &&
                condition.apply(criteria.getTargetScore()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getDueDate()) &&
                condition.apply(criteria.getObservationEndDate()) &&
                condition.apply(criteria.getResult()) &&
                condition.apply(criteria.getCycle()) &&
                condition.apply(criteria.getProcedureDocumentRef()) &&
                condition.apply(criteria.getClosedAt()) &&
                condition.apply(criteria.getRiskId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ActionSheetCriteria> copyFiltersAre(ActionSheetCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getOriginKind(), copy.getOriginKind()) &&
                condition.apply(criteria.getOriginReference(), copy.getOriginReference()) &&
                condition.apply(criteria.getOriginService(), copy.getOriginService()) &&
                condition.apply(criteria.getPhase(), copy.getPhase()) &&
                condition.apply(criteria.getObjective(), copy.getObjective()) &&
                condition.apply(criteria.getCauseAnalysis(), copy.getCauseAnalysis()) &&
                condition.apply(criteria.getTargetScore(), copy.getTargetScore()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getDueDate(), copy.getDueDate()) &&
                condition.apply(criteria.getObservationEndDate(), copy.getObservationEndDate()) &&
                condition.apply(criteria.getResult(), copy.getResult()) &&
                condition.apply(criteria.getCycle(), copy.getCycle()) &&
                condition.apply(criteria.getProcedureDocumentRef(), copy.getProcedureDocumentRef()) &&
                condition.apply(criteria.getClosedAt(), copy.getClosedAt()) &&
                condition.apply(criteria.getRiskId(), copy.getRiskId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
