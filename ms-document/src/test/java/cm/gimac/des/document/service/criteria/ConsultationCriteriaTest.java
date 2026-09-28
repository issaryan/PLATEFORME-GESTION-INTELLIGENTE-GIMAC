package cm.gimac.des.document.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ConsultationCriteriaTest {

    @Test
    void newConsultationCriteriaHasAllFiltersNullTest() {
        var consultationCriteria = new ConsultationCriteria();
        assertThat(consultationCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void consultationCriteriaFluentMethodsCreatesFiltersTest() {
        var consultationCriteria = new ConsultationCriteria();

        setAllFilters(consultationCriteria);

        assertThat(consultationCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void consultationCriteriaCopyCreatesNullFilterTest() {
        var consultationCriteria = new ConsultationCriteria();
        var copy = consultationCriteria.copy();

        assertThat(consultationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(consultationCriteria)
        );
    }

    @Test
    void consultationCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var consultationCriteria = new ConsultationCriteria();
        setAllFilters(consultationCriteria);

        var copy = consultationCriteria.copy();

        assertThat(consultationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(consultationCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var consultationCriteria = new ConsultationCriteria();

        assertThat(consultationCriteria).hasToString("ConsultationCriteria{}");
    }

    private static void setAllFilters(ConsultationCriteria consultationCriteria) {
        consultationCriteria.id();
        consultationCriteria.userId();
        consultationCriteria.institutionId();
        consultationCriteria.action();
        consultationCriteria.channel();
        consultationCriteria.consultedAt();
        consultationCriteria.correlationId();
        consultationCriteria.granted();
        consultationCriteria.refusalReason();
        consultationCriteria.documentId();
        consultationCriteria.distinct();
    }

    private static Condition<ConsultationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getAction()) &&
                condition.apply(criteria.getChannel()) &&
                condition.apply(criteria.getConsultedAt()) &&
                condition.apply(criteria.getCorrelationId()) &&
                condition.apply(criteria.getGranted()) &&
                condition.apply(criteria.getRefusalReason()) &&
                condition.apply(criteria.getDocumentId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ConsultationCriteria> copyFiltersAre(
        ConsultationCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getAction(), copy.getAction()) &&
                condition.apply(criteria.getChannel(), copy.getChannel()) &&
                condition.apply(criteria.getConsultedAt(), copy.getConsultedAt()) &&
                condition.apply(criteria.getCorrelationId(), copy.getCorrelationId()) &&
                condition.apply(criteria.getGranted(), copy.getGranted()) &&
                condition.apply(criteria.getRefusalReason(), copy.getRefusalReason()) &&
                condition.apply(criteria.getDocumentId(), copy.getDocumentId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
