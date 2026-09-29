package cm.gimac.des.participant.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class EscalationContactCriteriaTest {

    @Test
    void newEscalationContactCriteriaHasAllFiltersNullTest() {
        var escalationContactCriteria = new EscalationContactCriteria();
        assertThat(escalationContactCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void escalationContactCriteriaFluentMethodsCreatesFiltersTest() {
        var escalationContactCriteria = new EscalationContactCriteria();

        setAllFilters(escalationContactCriteria);

        assertThat(escalationContactCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void escalationContactCriteriaCopyCreatesNullFilterTest() {
        var escalationContactCriteria = new EscalationContactCriteria();
        var copy = escalationContactCriteria.copy();

        assertThat(escalationContactCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(escalationContactCriteria)
        );
    }

    @Test
    void escalationContactCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var escalationContactCriteria = new EscalationContactCriteria();
        setAllFilters(escalationContactCriteria);

        var copy = escalationContactCriteria.copy();

        assertThat(escalationContactCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(escalationContactCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var escalationContactCriteria = new EscalationContactCriteria();

        assertThat(escalationContactCriteria).hasToString("EscalationContactCriteria{}");
    }

    private static void setAllFilters(EscalationContactCriteria escalationContactCriteria) {
        escalationContactCriteria.id();
        escalationContactCriteria.severity();
        escalationContactCriteria.escalationRank();
        escalationContactCriteria.ackDelay();
        escalationContactCriteria.active();
        escalationContactCriteria.institutionId();
        escalationContactCriteria.correspondentId();
        escalationContactCriteria.deputyId();
        escalationContactCriteria.distinct();
    }

    private static Condition<EscalationContactCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getSeverity()) &&
                condition.apply(criteria.getEscalationRank()) &&
                condition.apply(criteria.getAckDelay()) &&
                condition.apply(criteria.getActive()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getCorrespondentId()) &&
                condition.apply(criteria.getDeputyId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<EscalationContactCriteria> copyFiltersAre(
        EscalationContactCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getSeverity(), copy.getSeverity()) &&
                condition.apply(criteria.getEscalationRank(), copy.getEscalationRank()) &&
                condition.apply(criteria.getAckDelay(), copy.getAckDelay()) &&
                condition.apply(criteria.getActive(), copy.getActive()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getCorrespondentId(), copy.getCorrespondentId()) &&
                condition.apply(criteria.getDeputyId(), copy.getDeputyId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
