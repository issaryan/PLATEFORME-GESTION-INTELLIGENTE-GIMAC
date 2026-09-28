package cm.gimac.des.reporting.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DataExportCriteriaTest {

    @Test
    void newDataExportCriteriaHasAllFiltersNullTest() {
        var dataExportCriteria = new DataExportCriteria();
        assertThat(dataExportCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void dataExportCriteriaFluentMethodsCreatesFiltersTest() {
        var dataExportCriteria = new DataExportCriteria();

        setAllFilters(dataExportCriteria);

        assertThat(dataExportCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void dataExportCriteriaCopyCreatesNullFilterTest() {
        var dataExportCriteria = new DataExportCriteria();
        var copy = dataExportCriteria.copy();

        assertThat(dataExportCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(dataExportCriteria)
        );
    }

    @Test
    void dataExportCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var dataExportCriteria = new DataExportCriteria();
        setAllFilters(dataExportCriteria);

        var copy = dataExportCriteria.copy();

        assertThat(dataExportCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(dataExportCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var dataExportCriteria = new DataExportCriteria();

        assertThat(dataExportCriteria).hasToString("DataExportCriteria{}");
    }

    private static void setAllFilters(DataExportCriteria dataExportCriteria) {
        dataExportCriteria.id();
        dataExportCriteria.requesterId();
        dataExportCriteria.requestedAt();
        dataExportCriteria.indicatorCodes();
        dataExportCriteria.period();
        dataExportCriteria.rowCount();
        dataExportCriteria.watermark();
        dataExportCriteria.contentDigest();
        dataExportCriteria.documentRef();
        dataExportCriteria.distinct();
    }

    private static Condition<DataExportCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getRequesterId()) &&
                condition.apply(criteria.getRequestedAt()) &&
                condition.apply(criteria.getIndicatorCodes()) &&
                condition.apply(criteria.getPeriod()) &&
                condition.apply(criteria.getRowCount()) &&
                condition.apply(criteria.getWatermark()) &&
                condition.apply(criteria.getContentDigest()) &&
                condition.apply(criteria.getDocumentRef()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DataExportCriteria> copyFiltersAre(DataExportCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getRequesterId(), copy.getRequesterId()) &&
                condition.apply(criteria.getRequestedAt(), copy.getRequestedAt()) &&
                condition.apply(criteria.getIndicatorCodes(), copy.getIndicatorCodes()) &&
                condition.apply(criteria.getPeriod(), copy.getPeriod()) &&
                condition.apply(criteria.getRowCount(), copy.getRowCount()) &&
                condition.apply(criteria.getWatermark(), copy.getWatermark()) &&
                condition.apply(criteria.getContentDigest(), copy.getContentDigest()) &&
                condition.apply(criteria.getDocumentRef(), copy.getDocumentRef()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
