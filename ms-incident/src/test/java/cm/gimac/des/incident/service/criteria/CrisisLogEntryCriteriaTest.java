package cm.gimac.des.incident.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CrisisLogEntryCriteriaTest {

    @Test
    void newCrisisLogEntryCriteriaHasAllFiltersNullTest() {
        var crisisLogEntryCriteria = new CrisisLogEntryCriteria();
        assertThat(crisisLogEntryCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void crisisLogEntryCriteriaFluentMethodsCreatesFiltersTest() {
        var crisisLogEntryCriteria = new CrisisLogEntryCriteria();

        setAllFilters(crisisLogEntryCriteria);

        assertThat(crisisLogEntryCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void crisisLogEntryCriteriaCopyCreatesNullFilterTest() {
        var crisisLogEntryCriteria = new CrisisLogEntryCriteria();
        var copy = crisisLogEntryCriteria.copy();

        assertThat(crisisLogEntryCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(crisisLogEntryCriteria)
        );
    }

    @Test
    void crisisLogEntryCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var crisisLogEntryCriteria = new CrisisLogEntryCriteria();
        setAllFilters(crisisLogEntryCriteria);

        var copy = crisisLogEntryCriteria.copy();

        assertThat(crisisLogEntryCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(crisisLogEntryCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var crisisLogEntryCriteria = new CrisisLogEntryCriteria();

        assertThat(crisisLogEntryCriteria).hasToString("CrisisLogEntryCriteria{}");
    }

    private static void setAllFilters(CrisisLogEntryCriteria crisisLogEntryCriteria) {
        crisisLogEntryCriteria.id();
        crisisLogEntryCriteria.chainIndex();
        crisisLogEntryCriteria.occurredAt();
        crisisLogEntryCriteria.authorId();
        crisisLogEntryCriteria.nature();
        crisisLogEntryCriteria.text();
        crisisLogEntryCriteria.reference();
        crisisLogEntryCriteria.previousHash();
        crisisLogEntryCriteria.hash();
        crisisLogEntryCriteria.cellId();
        crisisLogEntryCriteria.distinct();
    }

    private static Condition<CrisisLogEntryCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getChainIndex()) &&
                condition.apply(criteria.getOccurredAt()) &&
                condition.apply(criteria.getAuthorId()) &&
                condition.apply(criteria.getNature()) &&
                condition.apply(criteria.getText()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getPreviousHash()) &&
                condition.apply(criteria.getHash()) &&
                condition.apply(criteria.getCellId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CrisisLogEntryCriteria> copyFiltersAre(
        CrisisLogEntryCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getChainIndex(), copy.getChainIndex()) &&
                condition.apply(criteria.getOccurredAt(), copy.getOccurredAt()) &&
                condition.apply(criteria.getAuthorId(), copy.getAuthorId()) &&
                condition.apply(criteria.getNature(), copy.getNature()) &&
                condition.apply(criteria.getText(), copy.getText()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getPreviousHash(), copy.getPreviousHash()) &&
                condition.apply(criteria.getHash(), copy.getHash()) &&
                condition.apply(criteria.getCellId(), copy.getCellId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
