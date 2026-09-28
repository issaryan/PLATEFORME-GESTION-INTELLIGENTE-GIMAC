package cm.gimac.des.rapprochement.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class StatementLineCriteriaTest {

    @Test
    void newStatementLineCriteriaHasAllFiltersNullTest() {
        var statementLineCriteria = new StatementLineCriteria();
        assertThat(statementLineCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void statementLineCriteriaFluentMethodsCreatesFiltersTest() {
        var statementLineCriteria = new StatementLineCriteria();

        setAllFilters(statementLineCriteria);

        assertThat(statementLineCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void statementLineCriteriaCopyCreatesNullFilterTest() {
        var statementLineCriteria = new StatementLineCriteria();
        var copy = statementLineCriteria.copy();

        assertThat(statementLineCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(statementLineCriteria)
        );
    }

    @Test
    void statementLineCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var statementLineCriteria = new StatementLineCriteria();
        setAllFilters(statementLineCriteria);

        var copy = statementLineCriteria.copy();

        assertThat(statementLineCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(statementLineCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var statementLineCriteria = new StatementLineCriteria();

        assertThat(statementLineCriteria).hasToString("StatementLineCriteria{}");
    }

    private static void setAllFilters(StatementLineCriteria statementLineCriteria) {
        statementLineCriteria.id();
        statementLineCriteria.lineNumber();
        statementLineCriteria.transactionRef();
        statementLineCriteria.authorizationRef();
        statementLineCriteria.operationAt();
        statementLineCriteria.amount();
        statementLineCriteria.participantCode();
        statementLineCriteria.lineStatus();
        statementLineCriteria.currency();
        statementLineCriteria.statementId();
        statementLineCriteria.distinct();
    }

    private static Condition<StatementLineCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getLineNumber()) &&
                condition.apply(criteria.getTransactionRef()) &&
                condition.apply(criteria.getAuthorizationRef()) &&
                condition.apply(criteria.getOperationAt()) &&
                condition.apply(criteria.getAmount()) &&
                condition.apply(criteria.getParticipantCode()) &&
                condition.apply(criteria.getLineStatus()) &&
                condition.apply(criteria.getCurrency()) &&
                condition.apply(criteria.getStatementId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<StatementLineCriteria> copyFiltersAre(
        StatementLineCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getLineNumber(), copy.getLineNumber()) &&
                condition.apply(criteria.getTransactionRef(), copy.getTransactionRef()) &&
                condition.apply(criteria.getAuthorizationRef(), copy.getAuthorizationRef()) &&
                condition.apply(criteria.getOperationAt(), copy.getOperationAt()) &&
                condition.apply(criteria.getAmount(), copy.getAmount()) &&
                condition.apply(criteria.getParticipantCode(), copy.getParticipantCode()) &&
                condition.apply(criteria.getLineStatus(), copy.getLineStatus()) &&
                condition.apply(criteria.getCurrency(), copy.getCurrency()) &&
                condition.apply(criteria.getStatementId(), copy.getStatementId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
