package cm.gimac.des.monitoring.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ShiftReportCriteriaTest {

    @Test
    void newShiftReportCriteriaHasAllFiltersNullTest() {
        var shiftReportCriteria = new ShiftReportCriteria();
        assertThat(shiftReportCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void shiftReportCriteriaFluentMethodsCreatesFiltersTest() {
        var shiftReportCriteria = new ShiftReportCriteria();

        setAllFilters(shiftReportCriteria);

        assertThat(shiftReportCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void shiftReportCriteriaCopyCreatesNullFilterTest() {
        var shiftReportCriteria = new ShiftReportCriteria();
        var copy = shiftReportCriteria.copy();

        assertThat(shiftReportCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(shiftReportCriteria)
        );
    }

    @Test
    void shiftReportCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var shiftReportCriteria = new ShiftReportCriteria();
        setAllFilters(shiftReportCriteria);

        var copy = shiftReportCriteria.copy();

        assertThat(shiftReportCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(shiftReportCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var shiftReportCriteria = new ShiftReportCriteria();

        assertThat(shiftReportCriteria).hasToString("ShiftReportCriteria{}");
    }

    private static void setAllFilters(ShiftReportCriteria shiftReportCriteria) {
        shiftReportCriteria.id();
        shiftReportCriteria.reference();
        shiftReportCriteria.state();
        shiftReportCriteria.logHeadHash();
        shiftReportCriteria.contentHash();
        shiftReportCriteria.digest();
        shiftReportCriteria.documentDigest();
        shiftReportCriteria.timestampSerialNumber();
        shiftReportCriteria.documentRef();
        shiftReportCriteria.responsibilityTransferredAt();
        shiftReportCriteria.sealedAt();
        shiftReportCriteria.shiftId();
        shiftReportCriteria.supersedesId();
        shiftReportCriteria.shiftReportId();
        shiftReportCriteria.distinct();
    }

    private static Condition<ShiftReportCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getLogHeadHash()) &&
                condition.apply(criteria.getContentHash()) &&
                condition.apply(criteria.getDigest()) &&
                condition.apply(criteria.getDocumentDigest()) &&
                condition.apply(criteria.getTimestampSerialNumber()) &&
                condition.apply(criteria.getDocumentRef()) &&
                condition.apply(criteria.getResponsibilityTransferredAt()) &&
                condition.apply(criteria.getSealedAt()) &&
                condition.apply(criteria.getShiftId()) &&
                condition.apply(criteria.getSupersedesId()) &&
                condition.apply(criteria.getShiftReportId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ShiftReportCriteria> copyFiltersAre(ShiftReportCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getLogHeadHash(), copy.getLogHeadHash()) &&
                condition.apply(criteria.getContentHash(), copy.getContentHash()) &&
                condition.apply(criteria.getDigest(), copy.getDigest()) &&
                condition.apply(criteria.getDocumentDigest(), copy.getDocumentDigest()) &&
                condition.apply(criteria.getTimestampSerialNumber(), copy.getTimestampSerialNumber()) &&
                condition.apply(criteria.getDocumentRef(), copy.getDocumentRef()) &&
                condition.apply(criteria.getResponsibilityTransferredAt(), copy.getResponsibilityTransferredAt()) &&
                condition.apply(criteria.getSealedAt(), copy.getSealedAt()) &&
                condition.apply(criteria.getShiftId(), copy.getShiftId()) &&
                condition.apply(criteria.getSupersedesId(), copy.getSupersedesId()) &&
                condition.apply(criteria.getShiftReportId(), copy.getShiftReportId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
