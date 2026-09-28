package cm.gimac.des.monitoring.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ShiftLogEntryCriteriaTest {

    @Test
    void newShiftLogEntryCriteriaHasAllFiltersNullTest() {
        var shiftLogEntryCriteria = new ShiftLogEntryCriteria();
        assertThat(shiftLogEntryCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void shiftLogEntryCriteriaFluentMethodsCreatesFiltersTest() {
        var shiftLogEntryCriteria = new ShiftLogEntryCriteria();

        setAllFilters(shiftLogEntryCriteria);

        assertThat(shiftLogEntryCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void shiftLogEntryCriteriaCopyCreatesNullFilterTest() {
        var shiftLogEntryCriteria = new ShiftLogEntryCriteria();
        var copy = shiftLogEntryCriteria.copy();

        assertThat(shiftLogEntryCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(shiftLogEntryCriteria)
        );
    }

    @Test
    void shiftLogEntryCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var shiftLogEntryCriteria = new ShiftLogEntryCriteria();
        setAllFilters(shiftLogEntryCriteria);

        var copy = shiftLogEntryCriteria.copy();

        assertThat(shiftLogEntryCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(shiftLogEntryCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var shiftLogEntryCriteria = new ShiftLogEntryCriteria();

        assertThat(shiftLogEntryCriteria).hasToString("ShiftLogEntryCriteria{}");
    }

    private static void setAllFilters(ShiftLogEntryCriteria shiftLogEntryCriteria) {
        shiftLogEntryCriteria.id();
        shiftLogEntryCriteria.chainIndex();
        shiftLogEntryCriteria.occurredAt();
        shiftLogEntryCriteria.authorId();
        shiftLogEntryCriteria.nature();
        shiftLogEntryCriteria.text();
        shiftLogEntryCriteria.reference();
        shiftLogEntryCriteria.previousHash();
        shiftLogEntryCriteria.hash();
        shiftLogEntryCriteria.shiftId();
        shiftLogEntryCriteria.correctsId();
        shiftLogEntryCriteria.distinct();
    }

    private static Condition<ShiftLogEntryCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
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
                condition.apply(criteria.getShiftId()) &&
                condition.apply(criteria.getCorrectsId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ShiftLogEntryCriteria> copyFiltersAre(
        ShiftLogEntryCriteria copy,
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
                condition.apply(criteria.getShiftId(), copy.getShiftId()) &&
                condition.apply(criteria.getCorrectsId(), copy.getCorrectsId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
