package cm.gimac.des.document.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DocumentTypePolicyCriteriaTest {

    @Test
    void newDocumentTypePolicyCriteriaHasAllFiltersNullTest() {
        var documentTypePolicyCriteria = new DocumentTypePolicyCriteria();
        assertThat(documentTypePolicyCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void documentTypePolicyCriteriaFluentMethodsCreatesFiltersTest() {
        var documentTypePolicyCriteria = new DocumentTypePolicyCriteria();

        setAllFilters(documentTypePolicyCriteria);

        assertThat(documentTypePolicyCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void documentTypePolicyCriteriaCopyCreatesNullFilterTest() {
        var documentTypePolicyCriteria = new DocumentTypePolicyCriteria();
        var copy = documentTypePolicyCriteria.copy();

        assertThat(documentTypePolicyCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(documentTypePolicyCriteria)
        );
    }

    @Test
    void documentTypePolicyCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var documentTypePolicyCriteria = new DocumentTypePolicyCriteria();
        setAllFilters(documentTypePolicyCriteria);

        var copy = documentTypePolicyCriteria.copy();

        assertThat(documentTypePolicyCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(documentTypePolicyCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var documentTypePolicyCriteria = new DocumentTypePolicyCriteria();

        assertThat(documentTypePolicyCriteria).hasToString("DocumentTypePolicyCriteria{}");
    }

    private static void setAllFilters(DocumentTypePolicyCriteria documentTypePolicyCriteria) {
        documentTypePolicyCriteria.id();
        documentTypePolicyCriteria.documentType();
        documentTypePolicyCriteria.policyVersion();
        documentTypePolicyCriteria.retentionPeriod();
        documentTypePolicyCriteria.retentionMode();
        documentTypePolicyCriteria.maskingRule();
        documentTypePolicyCriteria.restrictedAccess();
        documentTypePolicyCriteria.pdfaRequired();
        documentTypePolicyCriteria.allowedMediaTypes();
        documentTypePolicyCriteria.maxSizeBytes();
        documentTypePolicyCriteria.allowedCallers();
        documentTypePolicyCriteria.effectiveFrom();
        documentTypePolicyCriteria.active();
        documentTypePolicyCriteria.distinct();
    }

    private static Condition<DocumentTypePolicyCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getDocumentType()) &&
                condition.apply(criteria.getPolicyVersion()) &&
                condition.apply(criteria.getRetentionPeriod()) &&
                condition.apply(criteria.getRetentionMode()) &&
                condition.apply(criteria.getMaskingRule()) &&
                condition.apply(criteria.getRestrictedAccess()) &&
                condition.apply(criteria.getPdfaRequired()) &&
                condition.apply(criteria.getAllowedMediaTypes()) &&
                condition.apply(criteria.getMaxSizeBytes()) &&
                condition.apply(criteria.getAllowedCallers()) &&
                condition.apply(criteria.getEffectiveFrom()) &&
                condition.apply(criteria.getActive()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DocumentTypePolicyCriteria> copyFiltersAre(
        DocumentTypePolicyCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getDocumentType(), copy.getDocumentType()) &&
                condition.apply(criteria.getPolicyVersion(), copy.getPolicyVersion()) &&
                condition.apply(criteria.getRetentionPeriod(), copy.getRetentionPeriod()) &&
                condition.apply(criteria.getRetentionMode(), copy.getRetentionMode()) &&
                condition.apply(criteria.getMaskingRule(), copy.getMaskingRule()) &&
                condition.apply(criteria.getRestrictedAccess(), copy.getRestrictedAccess()) &&
                condition.apply(criteria.getPdfaRequired(), copy.getPdfaRequired()) &&
                condition.apply(criteria.getAllowedMediaTypes(), copy.getAllowedMediaTypes()) &&
                condition.apply(criteria.getMaxSizeBytes(), copy.getMaxSizeBytes()) &&
                condition.apply(criteria.getAllowedCallers(), copy.getAllowedCallers()) &&
                condition.apply(criteria.getEffectiveFrom(), copy.getEffectiveFrom()) &&
                condition.apply(criteria.getActive(), copy.getActive()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
