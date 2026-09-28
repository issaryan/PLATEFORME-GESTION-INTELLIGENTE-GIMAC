package cm.gimac.des.certification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ChangeRecordCriteriaTest {

    @Test
    void newChangeRecordCriteriaHasAllFiltersNullTest() {
        var changeRecordCriteria = new ChangeRecordCriteria();
        assertThat(changeRecordCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void changeRecordCriteriaFluentMethodsCreatesFiltersTest() {
        var changeRecordCriteria = new ChangeRecordCriteria();

        setAllFilters(changeRecordCriteria);

        assertThat(changeRecordCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void changeRecordCriteriaCopyCreatesNullFilterTest() {
        var changeRecordCriteria = new ChangeRecordCriteria();
        var copy = changeRecordCriteria.copy();

        assertThat(changeRecordCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(changeRecordCriteria)
        );
    }

    @Test
    void changeRecordCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var changeRecordCriteria = new ChangeRecordCriteria();
        setAllFilters(changeRecordCriteria);

        var copy = changeRecordCriteria.copy();

        assertThat(changeRecordCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(changeRecordCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var changeRecordCriteria = new ChangeRecordCriteria();

        assertThat(changeRecordCriteria).hasToString("ChangeRecordCriteria{}");
    }

    private static void setAllFilters(ChangeRecordCriteria changeRecordCriteria) {
        changeRecordCriteria.id();
        changeRecordCriteria.externalRef();
        changeRecordCriteria.approval();
        changeRecordCriteria.approvedAt();
        changeRecordCriteria.windowStart();
        changeRecordCriteria.windowEnd();
        changeRecordCriteria.readAt();
        changeRecordCriteria.contentDigest();
        changeRecordCriteria.referencedBy();
        changeRecordCriteria.commissioningFileId();
        changeRecordCriteria.distinct();
    }

    private static Condition<ChangeRecordCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getExternalRef()) &&
                condition.apply(criteria.getApproval()) &&
                condition.apply(criteria.getApprovedAt()) &&
                condition.apply(criteria.getWindowStart()) &&
                condition.apply(criteria.getWindowEnd()) &&
                condition.apply(criteria.getReadAt()) &&
                condition.apply(criteria.getContentDigest()) &&
                condition.apply(criteria.getReferencedBy()) &&
                condition.apply(criteria.getCommissioningFileId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ChangeRecordCriteria> copyFiltersAre(
        ChangeRecordCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getExternalRef(), copy.getExternalRef()) &&
                condition.apply(criteria.getApproval(), copy.getApproval()) &&
                condition.apply(criteria.getApprovedAt(), copy.getApprovedAt()) &&
                condition.apply(criteria.getWindowStart(), copy.getWindowStart()) &&
                condition.apply(criteria.getWindowEnd(), copy.getWindowEnd()) &&
                condition.apply(criteria.getReadAt(), copy.getReadAt()) &&
                condition.apply(criteria.getContentDigest(), copy.getContentDigest()) &&
                condition.apply(criteria.getReferencedBy(), copy.getReferencedBy()) &&
                condition.apply(criteria.getCommissioningFileId(), copy.getCommissioningFileId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
