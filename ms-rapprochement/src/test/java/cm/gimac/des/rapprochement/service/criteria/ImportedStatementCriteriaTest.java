package cm.gimac.des.rapprochement.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ImportedStatementCriteriaTest {

    @Test
    void newImportedStatementCriteriaHasAllFiltersNullTest() {
        var importedStatementCriteria = new ImportedStatementCriteria();
        assertThat(importedStatementCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void importedStatementCriteriaFluentMethodsCreatesFiltersTest() {
        var importedStatementCriteria = new ImportedStatementCriteria();

        setAllFilters(importedStatementCriteria);

        assertThat(importedStatementCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void importedStatementCriteriaCopyCreatesNullFilterTest() {
        var importedStatementCriteria = new ImportedStatementCriteria();
        var copy = importedStatementCriteria.copy();

        assertThat(importedStatementCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(importedStatementCriteria)
        );
    }

    @Test
    void importedStatementCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var importedStatementCriteria = new ImportedStatementCriteria();
        setAllFilters(importedStatementCriteria);

        var copy = importedStatementCriteria.copy();

        assertThat(importedStatementCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(importedStatementCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var importedStatementCriteria = new ImportedStatementCriteria();

        assertThat(importedStatementCriteria).hasToString("ImportedStatementCriteria{}");
    }

    private static void setAllFilters(ImportedStatementCriteria importedStatementCriteria) {
        importedStatementCriteria.id();
        importedStatementCriteria.reference();
        importedStatementCriteria.source();
        importedStatementCriteria.institutionId();
        importedStatementCriteria.businessDate();
        importedStatementCriteria.importedAt();
        importedStatementCriteria.importerId();
        importedStatementCriteria.format();
        importedStatementCriteria.digest();
        importedStatementCriteria.maskingPassed();
        importedStatementCriteria.maskingRejectedLines();
        importedStatementCriteria.maskingCheckedAt();
        importedStatementCriteria.lineCount();
        importedStatementCriteria.documentRef();
        importedStatementCriteria.linesRetainedUntil();
        importedStatementCriteria.distinct();
    }

    private static Condition<ImportedStatementCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getSource()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getBusinessDate()) &&
                condition.apply(criteria.getImportedAt()) &&
                condition.apply(criteria.getImporterId()) &&
                condition.apply(criteria.getFormat()) &&
                condition.apply(criteria.getDigest()) &&
                condition.apply(criteria.getMaskingPassed()) &&
                condition.apply(criteria.getMaskingRejectedLines()) &&
                condition.apply(criteria.getMaskingCheckedAt()) &&
                condition.apply(criteria.getLineCount()) &&
                condition.apply(criteria.getDocumentRef()) &&
                condition.apply(criteria.getLinesRetainedUntil()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ImportedStatementCriteria> copyFiltersAre(
        ImportedStatementCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getSource(), copy.getSource()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getBusinessDate(), copy.getBusinessDate()) &&
                condition.apply(criteria.getImportedAt(), copy.getImportedAt()) &&
                condition.apply(criteria.getImporterId(), copy.getImporterId()) &&
                condition.apply(criteria.getFormat(), copy.getFormat()) &&
                condition.apply(criteria.getDigest(), copy.getDigest()) &&
                condition.apply(criteria.getMaskingPassed(), copy.getMaskingPassed()) &&
                condition.apply(criteria.getMaskingRejectedLines(), copy.getMaskingRejectedLines()) &&
                condition.apply(criteria.getMaskingCheckedAt(), copy.getMaskingCheckedAt()) &&
                condition.apply(criteria.getLineCount(), copy.getLineCount()) &&
                condition.apply(criteria.getDocumentRef(), copy.getDocumentRef()) &&
                condition.apply(criteria.getLinesRetainedUntil(), copy.getLinesRetainedUntil()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
