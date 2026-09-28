package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class WipOverrideCriteriaTest {

    @Test
    void newWipOverrideCriteriaHasAllFiltersNullTest() {
        var wipOverrideCriteria = new WipOverrideCriteria();
        assertThat(wipOverrideCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void wipOverrideCriteriaFluentMethodsCreatesFiltersTest() {
        var wipOverrideCriteria = new WipOverrideCriteria();

        setAllFilters(wipOverrideCriteria);

        assertThat(wipOverrideCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void wipOverrideCriteriaCopyCreatesNullFilterTest() {
        var wipOverrideCriteria = new WipOverrideCriteria();
        var copy = wipOverrideCriteria.copy();

        assertThat(wipOverrideCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(wipOverrideCriteria)
        );
    }

    @Test
    void wipOverrideCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var wipOverrideCriteria = new WipOverrideCriteria();
        setAllFilters(wipOverrideCriteria);

        var copy = wipOverrideCriteria.copy();

        assertThat(wipOverrideCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(wipOverrideCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var wipOverrideCriteria = new WipOverrideCriteria();

        assertThat(wipOverrideCriteria).hasToString("WipOverrideCriteria{}");
    }

    private static void setAllFilters(WipOverrideCriteria wipOverrideCriteria) {
        wipOverrideCriteria.id();
        wipOverrideCriteria.requesterId();
        wipOverrideCriteria.approverId();
        wipOverrideCriteria.reason();
        wipOverrideCriteria.grantedAt();
        wipOverrideCriteria.expiresAt();
        wipOverrideCriteria.expiryNotifiedAt();
        wipOverrideCriteria.boardColumnId();
        wipOverrideCriteria.taskId();
        wipOverrideCriteria.distinct();
    }

    private static Condition<WipOverrideCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getRequesterId()) &&
                condition.apply(criteria.getApproverId()) &&
                condition.apply(criteria.getReason()) &&
                condition.apply(criteria.getGrantedAt()) &&
                condition.apply(criteria.getExpiresAt()) &&
                condition.apply(criteria.getExpiryNotifiedAt()) &&
                condition.apply(criteria.getBoardColumnId()) &&
                condition.apply(criteria.getTaskId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<WipOverrideCriteria> copyFiltersAre(WipOverrideCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getRequesterId(), copy.getRequesterId()) &&
                condition.apply(criteria.getApproverId(), copy.getApproverId()) &&
                condition.apply(criteria.getReason(), copy.getReason()) &&
                condition.apply(criteria.getGrantedAt(), copy.getGrantedAt()) &&
                condition.apply(criteria.getExpiresAt(), copy.getExpiresAt()) &&
                condition.apply(criteria.getExpiryNotifiedAt(), copy.getExpiryNotifiedAt()) &&
                condition.apply(criteria.getBoardColumnId(), copy.getBoardColumnId()) &&
                condition.apply(criteria.getTaskId(), copy.getTaskId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
