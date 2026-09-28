package cm.gimac.des.document.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DocumentCriteriaTest {

    @Test
    void newDocumentCriteriaHasAllFiltersNullTest() {
        var documentCriteria = new DocumentCriteria();
        assertThat(documentCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void documentCriteriaFluentMethodsCreatesFiltersTest() {
        var documentCriteria = new DocumentCriteria();

        setAllFilters(documentCriteria);

        assertThat(documentCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void documentCriteriaCopyCreatesNullFilterTest() {
        var documentCriteria = new DocumentCriteria();
        var copy = documentCriteria.copy();

        assertThat(documentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(documentCriteria)
        );
    }

    @Test
    void documentCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var documentCriteria = new DocumentCriteria();
        setAllFilters(documentCriteria);

        var copy = documentCriteria.copy();

        assertThat(documentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(documentCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var documentCriteria = new DocumentCriteria();

        assertThat(documentCriteria).hasToString("DocumentCriteria{}");
    }

    private static void setAllFilters(DocumentCriteria documentCriteria) {
        documentCriteria.id();
        documentCriteria.type();
        documentCriteria.title();
        documentCriteria.version();
        documentCriteria.authorId();
        documentCriteria.callerService();
        documentCriteria.institutionId();
        documentCriteria.visibility();
        documentCriteria.ownerUnit();
        documentCriteria.restrictedReaderIds();
        documentCriteria.depositedAt();
        documentCriteria.sha256();
        documentCriteria.sizeBytes();
        documentCriteria.mediaType();
        documentCriteria.objectKey();
        documentCriteria.state();
        documentCriteria.retentionPeriod();
        documentCriteria.retentionMode();
        documentCriteria.legalHold();
        documentCriteria.retainUntil();
        documentCriteria.lockedAt();
        documentCriteria.scanClean();
        documentCriteria.scanSignatureName();
        documentCriteria.scanEngineVersion();
        documentCriteria.scannedAt();
        documentCriteria.maskingCandidates();
        documentCriteria.maskingLuhnValid();
        documentCriteria.maskingMasked();
        documentCriteria.maskingRejected();
        documentCriteria.pdfaCompliant();
        documentCriteria.rejectionReason();
        documentCriteria.sealedAt();
        documentCriteria.purgedAt();
        documentCriteria.lineageId();
        documentCriteria.policyId();
        documentCriteria.documentLineageId();
        documentCriteria.distinct();
    }

    private static Condition<DocumentCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getVersion()) &&
                condition.apply(criteria.getAuthorId()) &&
                condition.apply(criteria.getCallerService()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getVisibility()) &&
                condition.apply(criteria.getOwnerUnit()) &&
                condition.apply(criteria.getRestrictedReaderIds()) &&
                condition.apply(criteria.getDepositedAt()) &&
                condition.apply(criteria.getSha256()) &&
                condition.apply(criteria.getSizeBytes()) &&
                condition.apply(criteria.getMediaType()) &&
                condition.apply(criteria.getObjectKey()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getRetentionPeriod()) &&
                condition.apply(criteria.getRetentionMode()) &&
                condition.apply(criteria.getLegalHold()) &&
                condition.apply(criteria.getRetainUntil()) &&
                condition.apply(criteria.getLockedAt()) &&
                condition.apply(criteria.getScanClean()) &&
                condition.apply(criteria.getScanSignatureName()) &&
                condition.apply(criteria.getScanEngineVersion()) &&
                condition.apply(criteria.getScannedAt()) &&
                condition.apply(criteria.getMaskingCandidates()) &&
                condition.apply(criteria.getMaskingLuhnValid()) &&
                condition.apply(criteria.getMaskingMasked()) &&
                condition.apply(criteria.getMaskingRejected()) &&
                condition.apply(criteria.getPdfaCompliant()) &&
                condition.apply(criteria.getRejectionReason()) &&
                condition.apply(criteria.getSealedAt()) &&
                condition.apply(criteria.getPurgedAt()) &&
                condition.apply(criteria.getLineageId()) &&
                condition.apply(criteria.getPolicyId()) &&
                condition.apply(criteria.getDocumentLineageId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DocumentCriteria> copyFiltersAre(DocumentCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getVersion(), copy.getVersion()) &&
                condition.apply(criteria.getAuthorId(), copy.getAuthorId()) &&
                condition.apply(criteria.getCallerService(), copy.getCallerService()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getVisibility(), copy.getVisibility()) &&
                condition.apply(criteria.getOwnerUnit(), copy.getOwnerUnit()) &&
                condition.apply(criteria.getRestrictedReaderIds(), copy.getRestrictedReaderIds()) &&
                condition.apply(criteria.getDepositedAt(), copy.getDepositedAt()) &&
                condition.apply(criteria.getSha256(), copy.getSha256()) &&
                condition.apply(criteria.getSizeBytes(), copy.getSizeBytes()) &&
                condition.apply(criteria.getMediaType(), copy.getMediaType()) &&
                condition.apply(criteria.getObjectKey(), copy.getObjectKey()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getRetentionPeriod(), copy.getRetentionPeriod()) &&
                condition.apply(criteria.getRetentionMode(), copy.getRetentionMode()) &&
                condition.apply(criteria.getLegalHold(), copy.getLegalHold()) &&
                condition.apply(criteria.getRetainUntil(), copy.getRetainUntil()) &&
                condition.apply(criteria.getLockedAt(), copy.getLockedAt()) &&
                condition.apply(criteria.getScanClean(), copy.getScanClean()) &&
                condition.apply(criteria.getScanSignatureName(), copy.getScanSignatureName()) &&
                condition.apply(criteria.getScanEngineVersion(), copy.getScanEngineVersion()) &&
                condition.apply(criteria.getScannedAt(), copy.getScannedAt()) &&
                condition.apply(criteria.getMaskingCandidates(), copy.getMaskingCandidates()) &&
                condition.apply(criteria.getMaskingLuhnValid(), copy.getMaskingLuhnValid()) &&
                condition.apply(criteria.getMaskingMasked(), copy.getMaskingMasked()) &&
                condition.apply(criteria.getMaskingRejected(), copy.getMaskingRejected()) &&
                condition.apply(criteria.getPdfaCompliant(), copy.getPdfaCompliant()) &&
                condition.apply(criteria.getRejectionReason(), copy.getRejectionReason()) &&
                condition.apply(criteria.getSealedAt(), copy.getSealedAt()) &&
                condition.apply(criteria.getPurgedAt(), copy.getPurgedAt()) &&
                condition.apply(criteria.getLineageId(), copy.getLineageId()) &&
                condition.apply(criteria.getPolicyId(), copy.getPolicyId()) &&
                condition.apply(criteria.getDocumentLineageId(), copy.getDocumentLineageId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
