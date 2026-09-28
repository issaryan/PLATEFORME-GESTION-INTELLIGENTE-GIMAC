package cm.gimac.des.document.service.criteria;

import cm.gimac.des.document.domain.enumeration.DocumentState;
import cm.gimac.des.document.domain.enumeration.DocumentType;
import cm.gimac.des.document.domain.enumeration.DocumentVisibility;
import cm.gimac.des.document.domain.enumeration.RetentionMode;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.document.domain.Document} entity. This class is used
 * in {@link cm.gimac.des.document.web.rest.DocumentResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /documents?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DocumentCriteria implements Serializable, Criteria {

    /**
     * Class for filtering DocumentType
     */
    public static class DocumentTypeFilter extends Filter<DocumentType> {

        public DocumentTypeFilter() {}

        public DocumentTypeFilter(DocumentTypeFilter filter) {
            super(filter);
        }

        @Override
        public DocumentTypeFilter copy() {
            return new DocumentTypeFilter(this);
        }
    }

    /**
     * Class for filtering DocumentVisibility
     */
    public static class DocumentVisibilityFilter extends Filter<DocumentVisibility> {

        public DocumentVisibilityFilter() {}

        public DocumentVisibilityFilter(DocumentVisibilityFilter filter) {
            super(filter);
        }

        @Override
        public DocumentVisibilityFilter copy() {
            return new DocumentVisibilityFilter(this);
        }
    }

    /**
     * Class for filtering DocumentState
     */
    public static class DocumentStateFilter extends Filter<DocumentState> {

        public DocumentStateFilter() {}

        public DocumentStateFilter(DocumentStateFilter filter) {
            super(filter);
        }

        @Override
        public DocumentStateFilter copy() {
            return new DocumentStateFilter(this);
        }
    }

    /**
     * Class for filtering RetentionMode
     */
    public static class RetentionModeFilter extends Filter<RetentionMode> {

        public RetentionModeFilter() {}

        public RetentionModeFilter(RetentionModeFilter filter) {
            super(filter);
        }

        @Override
        public RetentionModeFilter copy() {
            return new RetentionModeFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private DocumentTypeFilter type;

    private StringFilter title;

    private IntegerFilter version;

    private UUIDFilter authorId;

    private StringFilter callerService;

    private UUIDFilter institutionId;

    private DocumentVisibilityFilter visibility;

    private StringFilter ownerUnit;

    private StringFilter restrictedReaderIds;

    private InstantFilter depositedAt;

    private StringFilter sha256;

    private LongFilter sizeBytes;

    private StringFilter mediaType;

    private StringFilter objectKey;

    private DocumentStateFilter state;

    private StringFilter retentionPeriod;

    private RetentionModeFilter retentionMode;

    private BooleanFilter legalHold;

    private InstantFilter retainUntil;

    private InstantFilter lockedAt;

    private BooleanFilter scanClean;

    private StringFilter scanSignatureName;

    private StringFilter scanEngineVersion;

    private InstantFilter scannedAt;

    private IntegerFilter maskingCandidates;

    private IntegerFilter maskingLuhnValid;

    private IntegerFilter maskingMasked;

    private BooleanFilter maskingRejected;

    private BooleanFilter pdfaCompliant;

    private StringFilter rejectionReason;

    private InstantFilter sealedAt;

    private InstantFilter purgedAt;

    private UUIDFilter lineageId;

    private UUIDFilter policyId;

    private UUIDFilter documentLineageId;

    private Boolean distinct;

    public DocumentCriteria() {}

    public DocumentCriteria(DocumentCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.type = other.optionalType().map(DocumentTypeFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.version = other.optionalVersion().map(IntegerFilter::copy).orElse(null);
        this.authorId = other.optionalAuthorId().map(UUIDFilter::copy).orElse(null);
        this.callerService = other.optionalCallerService().map(StringFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.visibility = other.optionalVisibility().map(DocumentVisibilityFilter::copy).orElse(null);
        this.ownerUnit = other.optionalOwnerUnit().map(StringFilter::copy).orElse(null);
        this.restrictedReaderIds = other.optionalRestrictedReaderIds().map(StringFilter::copy).orElse(null);
        this.depositedAt = other.optionalDepositedAt().map(InstantFilter::copy).orElse(null);
        this.sha256 = other.optionalSha256().map(StringFilter::copy).orElse(null);
        this.sizeBytes = other.optionalSizeBytes().map(LongFilter::copy).orElse(null);
        this.mediaType = other.optionalMediaType().map(StringFilter::copy).orElse(null);
        this.objectKey = other.optionalObjectKey().map(StringFilter::copy).orElse(null);
        this.state = other.optionalState().map(DocumentStateFilter::copy).orElse(null);
        this.retentionPeriod = other.optionalRetentionPeriod().map(StringFilter::copy).orElse(null);
        this.retentionMode = other.optionalRetentionMode().map(RetentionModeFilter::copy).orElse(null);
        this.legalHold = other.optionalLegalHold().map(BooleanFilter::copy).orElse(null);
        this.retainUntil = other.optionalRetainUntil().map(InstantFilter::copy).orElse(null);
        this.lockedAt = other.optionalLockedAt().map(InstantFilter::copy).orElse(null);
        this.scanClean = other.optionalScanClean().map(BooleanFilter::copy).orElse(null);
        this.scanSignatureName = other.optionalScanSignatureName().map(StringFilter::copy).orElse(null);
        this.scanEngineVersion = other.optionalScanEngineVersion().map(StringFilter::copy).orElse(null);
        this.scannedAt = other.optionalScannedAt().map(InstantFilter::copy).orElse(null);
        this.maskingCandidates = other.optionalMaskingCandidates().map(IntegerFilter::copy).orElse(null);
        this.maskingLuhnValid = other.optionalMaskingLuhnValid().map(IntegerFilter::copy).orElse(null);
        this.maskingMasked = other.optionalMaskingMasked().map(IntegerFilter::copy).orElse(null);
        this.maskingRejected = other.optionalMaskingRejected().map(BooleanFilter::copy).orElse(null);
        this.pdfaCompliant = other.optionalPdfaCompliant().map(BooleanFilter::copy).orElse(null);
        this.rejectionReason = other.optionalRejectionReason().map(StringFilter::copy).orElse(null);
        this.sealedAt = other.optionalSealedAt().map(InstantFilter::copy).orElse(null);
        this.purgedAt = other.optionalPurgedAt().map(InstantFilter::copy).orElse(null);
        this.lineageId = other.optionalLineageId().map(UUIDFilter::copy).orElse(null);
        this.policyId = other.optionalPolicyId().map(UUIDFilter::copy).orElse(null);
        this.documentLineageId = other.optionalDocumentLineageId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DocumentCriteria copy() {
        return new DocumentCriteria(this);
    }

    public UUIDFilter getId() {
        return id;
    }

    public Optional<UUIDFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public UUIDFilter id() {
        if (id == null) {
            setId(new UUIDFilter());
        }
        return id;
    }

    public void setId(UUIDFilter id) {
        this.id = id;
    }

    public DocumentTypeFilter getType() {
        return type;
    }

    public Optional<DocumentTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public DocumentTypeFilter type() {
        if (type == null) {
            setType(new DocumentTypeFilter());
        }
        return type;
    }

    public void setType(DocumentTypeFilter type) {
        this.type = type;
    }

    public StringFilter getTitle() {
        return title;
    }

    public Optional<StringFilter> optionalTitle() {
        return Optional.ofNullable(title);
    }

    public StringFilter title() {
        if (title == null) {
            setTitle(new StringFilter());
        }
        return title;
    }

    public void setTitle(StringFilter title) {
        this.title = title;
    }

    public IntegerFilter getVersion() {
        return version;
    }

    public Optional<IntegerFilter> optionalVersion() {
        return Optional.ofNullable(version);
    }

    public IntegerFilter version() {
        if (version == null) {
            setVersion(new IntegerFilter());
        }
        return version;
    }

    public void setVersion(IntegerFilter version) {
        this.version = version;
    }

    public UUIDFilter getAuthorId() {
        return authorId;
    }

    public Optional<UUIDFilter> optionalAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public UUIDFilter authorId() {
        if (authorId == null) {
            setAuthorId(new UUIDFilter());
        }
        return authorId;
    }

    public void setAuthorId(UUIDFilter authorId) {
        this.authorId = authorId;
    }

    public StringFilter getCallerService() {
        return callerService;
    }

    public Optional<StringFilter> optionalCallerService() {
        return Optional.ofNullable(callerService);
    }

    public StringFilter callerService() {
        if (callerService == null) {
            setCallerService(new StringFilter());
        }
        return callerService;
    }

    public void setCallerService(StringFilter callerService) {
        this.callerService = callerService;
    }

    public UUIDFilter getInstitutionId() {
        return institutionId;
    }

    public Optional<UUIDFilter> optionalInstitutionId() {
        return Optional.ofNullable(institutionId);
    }

    public UUIDFilter institutionId() {
        if (institutionId == null) {
            setInstitutionId(new UUIDFilter());
        }
        return institutionId;
    }

    public void setInstitutionId(UUIDFilter institutionId) {
        this.institutionId = institutionId;
    }

    public DocumentVisibilityFilter getVisibility() {
        return visibility;
    }

    public Optional<DocumentVisibilityFilter> optionalVisibility() {
        return Optional.ofNullable(visibility);
    }

    public DocumentVisibilityFilter visibility() {
        if (visibility == null) {
            setVisibility(new DocumentVisibilityFilter());
        }
        return visibility;
    }

    public void setVisibility(DocumentVisibilityFilter visibility) {
        this.visibility = visibility;
    }

    public StringFilter getOwnerUnit() {
        return ownerUnit;
    }

    public Optional<StringFilter> optionalOwnerUnit() {
        return Optional.ofNullable(ownerUnit);
    }

    public StringFilter ownerUnit() {
        if (ownerUnit == null) {
            setOwnerUnit(new StringFilter());
        }
        return ownerUnit;
    }

    public void setOwnerUnit(StringFilter ownerUnit) {
        this.ownerUnit = ownerUnit;
    }

    public StringFilter getRestrictedReaderIds() {
        return restrictedReaderIds;
    }

    public Optional<StringFilter> optionalRestrictedReaderIds() {
        return Optional.ofNullable(restrictedReaderIds);
    }

    public StringFilter restrictedReaderIds() {
        if (restrictedReaderIds == null) {
            setRestrictedReaderIds(new StringFilter());
        }
        return restrictedReaderIds;
    }

    public void setRestrictedReaderIds(StringFilter restrictedReaderIds) {
        this.restrictedReaderIds = restrictedReaderIds;
    }

    public InstantFilter getDepositedAt() {
        return depositedAt;
    }

    public Optional<InstantFilter> optionalDepositedAt() {
        return Optional.ofNullable(depositedAt);
    }

    public InstantFilter depositedAt() {
        if (depositedAt == null) {
            setDepositedAt(new InstantFilter());
        }
        return depositedAt;
    }

    public void setDepositedAt(InstantFilter depositedAt) {
        this.depositedAt = depositedAt;
    }

    public StringFilter getSha256() {
        return sha256;
    }

    public Optional<StringFilter> optionalSha256() {
        return Optional.ofNullable(sha256);
    }

    public StringFilter sha256() {
        if (sha256 == null) {
            setSha256(new StringFilter());
        }
        return sha256;
    }

    public void setSha256(StringFilter sha256) {
        this.sha256 = sha256;
    }

    public LongFilter getSizeBytes() {
        return sizeBytes;
    }

    public Optional<LongFilter> optionalSizeBytes() {
        return Optional.ofNullable(sizeBytes);
    }

    public LongFilter sizeBytes() {
        if (sizeBytes == null) {
            setSizeBytes(new LongFilter());
        }
        return sizeBytes;
    }

    public void setSizeBytes(LongFilter sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public StringFilter getMediaType() {
        return mediaType;
    }

    public Optional<StringFilter> optionalMediaType() {
        return Optional.ofNullable(mediaType);
    }

    public StringFilter mediaType() {
        if (mediaType == null) {
            setMediaType(new StringFilter());
        }
        return mediaType;
    }

    public void setMediaType(StringFilter mediaType) {
        this.mediaType = mediaType;
    }

    public StringFilter getObjectKey() {
        return objectKey;
    }

    public Optional<StringFilter> optionalObjectKey() {
        return Optional.ofNullable(objectKey);
    }

    public StringFilter objectKey() {
        if (objectKey == null) {
            setObjectKey(new StringFilter());
        }
        return objectKey;
    }

    public void setObjectKey(StringFilter objectKey) {
        this.objectKey = objectKey;
    }

    public DocumentStateFilter getState() {
        return state;
    }

    public Optional<DocumentStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public DocumentStateFilter state() {
        if (state == null) {
            setState(new DocumentStateFilter());
        }
        return state;
    }

    public void setState(DocumentStateFilter state) {
        this.state = state;
    }

    public StringFilter getRetentionPeriod() {
        return retentionPeriod;
    }

    public Optional<StringFilter> optionalRetentionPeriod() {
        return Optional.ofNullable(retentionPeriod);
    }

    public StringFilter retentionPeriod() {
        if (retentionPeriod == null) {
            setRetentionPeriod(new StringFilter());
        }
        return retentionPeriod;
    }

    public void setRetentionPeriod(StringFilter retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public RetentionModeFilter getRetentionMode() {
        return retentionMode;
    }

    public Optional<RetentionModeFilter> optionalRetentionMode() {
        return Optional.ofNullable(retentionMode);
    }

    public RetentionModeFilter retentionMode() {
        if (retentionMode == null) {
            setRetentionMode(new RetentionModeFilter());
        }
        return retentionMode;
    }

    public void setRetentionMode(RetentionModeFilter retentionMode) {
        this.retentionMode = retentionMode;
    }

    public BooleanFilter getLegalHold() {
        return legalHold;
    }

    public Optional<BooleanFilter> optionalLegalHold() {
        return Optional.ofNullable(legalHold);
    }

    public BooleanFilter legalHold() {
        if (legalHold == null) {
            setLegalHold(new BooleanFilter());
        }
        return legalHold;
    }

    public void setLegalHold(BooleanFilter legalHold) {
        this.legalHold = legalHold;
    }

    public InstantFilter getRetainUntil() {
        return retainUntil;
    }

    public Optional<InstantFilter> optionalRetainUntil() {
        return Optional.ofNullable(retainUntil);
    }

    public InstantFilter retainUntil() {
        if (retainUntil == null) {
            setRetainUntil(new InstantFilter());
        }
        return retainUntil;
    }

    public void setRetainUntil(InstantFilter retainUntil) {
        this.retainUntil = retainUntil;
    }

    public InstantFilter getLockedAt() {
        return lockedAt;
    }

    public Optional<InstantFilter> optionalLockedAt() {
        return Optional.ofNullable(lockedAt);
    }

    public InstantFilter lockedAt() {
        if (lockedAt == null) {
            setLockedAt(new InstantFilter());
        }
        return lockedAt;
    }

    public void setLockedAt(InstantFilter lockedAt) {
        this.lockedAt = lockedAt;
    }

    public BooleanFilter getScanClean() {
        return scanClean;
    }

    public Optional<BooleanFilter> optionalScanClean() {
        return Optional.ofNullable(scanClean);
    }

    public BooleanFilter scanClean() {
        if (scanClean == null) {
            setScanClean(new BooleanFilter());
        }
        return scanClean;
    }

    public void setScanClean(BooleanFilter scanClean) {
        this.scanClean = scanClean;
    }

    public StringFilter getScanSignatureName() {
        return scanSignatureName;
    }

    public Optional<StringFilter> optionalScanSignatureName() {
        return Optional.ofNullable(scanSignatureName);
    }

    public StringFilter scanSignatureName() {
        if (scanSignatureName == null) {
            setScanSignatureName(new StringFilter());
        }
        return scanSignatureName;
    }

    public void setScanSignatureName(StringFilter scanSignatureName) {
        this.scanSignatureName = scanSignatureName;
    }

    public StringFilter getScanEngineVersion() {
        return scanEngineVersion;
    }

    public Optional<StringFilter> optionalScanEngineVersion() {
        return Optional.ofNullable(scanEngineVersion);
    }

    public StringFilter scanEngineVersion() {
        if (scanEngineVersion == null) {
            setScanEngineVersion(new StringFilter());
        }
        return scanEngineVersion;
    }

    public void setScanEngineVersion(StringFilter scanEngineVersion) {
        this.scanEngineVersion = scanEngineVersion;
    }

    public InstantFilter getScannedAt() {
        return scannedAt;
    }

    public Optional<InstantFilter> optionalScannedAt() {
        return Optional.ofNullable(scannedAt);
    }

    public InstantFilter scannedAt() {
        if (scannedAt == null) {
            setScannedAt(new InstantFilter());
        }
        return scannedAt;
    }

    public void setScannedAt(InstantFilter scannedAt) {
        this.scannedAt = scannedAt;
    }

    public IntegerFilter getMaskingCandidates() {
        return maskingCandidates;
    }

    public Optional<IntegerFilter> optionalMaskingCandidates() {
        return Optional.ofNullable(maskingCandidates);
    }

    public IntegerFilter maskingCandidates() {
        if (maskingCandidates == null) {
            setMaskingCandidates(new IntegerFilter());
        }
        return maskingCandidates;
    }

    public void setMaskingCandidates(IntegerFilter maskingCandidates) {
        this.maskingCandidates = maskingCandidates;
    }

    public IntegerFilter getMaskingLuhnValid() {
        return maskingLuhnValid;
    }

    public Optional<IntegerFilter> optionalMaskingLuhnValid() {
        return Optional.ofNullable(maskingLuhnValid);
    }

    public IntegerFilter maskingLuhnValid() {
        if (maskingLuhnValid == null) {
            setMaskingLuhnValid(new IntegerFilter());
        }
        return maskingLuhnValid;
    }

    public void setMaskingLuhnValid(IntegerFilter maskingLuhnValid) {
        this.maskingLuhnValid = maskingLuhnValid;
    }

    public IntegerFilter getMaskingMasked() {
        return maskingMasked;
    }

    public Optional<IntegerFilter> optionalMaskingMasked() {
        return Optional.ofNullable(maskingMasked);
    }

    public IntegerFilter maskingMasked() {
        if (maskingMasked == null) {
            setMaskingMasked(new IntegerFilter());
        }
        return maskingMasked;
    }

    public void setMaskingMasked(IntegerFilter maskingMasked) {
        this.maskingMasked = maskingMasked;
    }

    public BooleanFilter getMaskingRejected() {
        return maskingRejected;
    }

    public Optional<BooleanFilter> optionalMaskingRejected() {
        return Optional.ofNullable(maskingRejected);
    }

    public BooleanFilter maskingRejected() {
        if (maskingRejected == null) {
            setMaskingRejected(new BooleanFilter());
        }
        return maskingRejected;
    }

    public void setMaskingRejected(BooleanFilter maskingRejected) {
        this.maskingRejected = maskingRejected;
    }

    public BooleanFilter getPdfaCompliant() {
        return pdfaCompliant;
    }

    public Optional<BooleanFilter> optionalPdfaCompliant() {
        return Optional.ofNullable(pdfaCompliant);
    }

    public BooleanFilter pdfaCompliant() {
        if (pdfaCompliant == null) {
            setPdfaCompliant(new BooleanFilter());
        }
        return pdfaCompliant;
    }

    public void setPdfaCompliant(BooleanFilter pdfaCompliant) {
        this.pdfaCompliant = pdfaCompliant;
    }

    public StringFilter getRejectionReason() {
        return rejectionReason;
    }

    public Optional<StringFilter> optionalRejectionReason() {
        return Optional.ofNullable(rejectionReason);
    }

    public StringFilter rejectionReason() {
        if (rejectionReason == null) {
            setRejectionReason(new StringFilter());
        }
        return rejectionReason;
    }

    public void setRejectionReason(StringFilter rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public InstantFilter getSealedAt() {
        return sealedAt;
    }

    public Optional<InstantFilter> optionalSealedAt() {
        return Optional.ofNullable(sealedAt);
    }

    public InstantFilter sealedAt() {
        if (sealedAt == null) {
            setSealedAt(new InstantFilter());
        }
        return sealedAt;
    }

    public void setSealedAt(InstantFilter sealedAt) {
        this.sealedAt = sealedAt;
    }

    public InstantFilter getPurgedAt() {
        return purgedAt;
    }

    public Optional<InstantFilter> optionalPurgedAt() {
        return Optional.ofNullable(purgedAt);
    }

    public InstantFilter purgedAt() {
        if (purgedAt == null) {
            setPurgedAt(new InstantFilter());
        }
        return purgedAt;
    }

    public void setPurgedAt(InstantFilter purgedAt) {
        this.purgedAt = purgedAt;
    }

    public UUIDFilter getLineageId() {
        return lineageId;
    }

    public Optional<UUIDFilter> optionalLineageId() {
        return Optional.ofNullable(lineageId);
    }

    public UUIDFilter lineageId() {
        if (lineageId == null) {
            setLineageId(new UUIDFilter());
        }
        return lineageId;
    }

    public void setLineageId(UUIDFilter lineageId) {
        this.lineageId = lineageId;
    }

    public UUIDFilter getPolicyId() {
        return policyId;
    }

    public Optional<UUIDFilter> optionalPolicyId() {
        return Optional.ofNullable(policyId);
    }

    public UUIDFilter policyId() {
        if (policyId == null) {
            setPolicyId(new UUIDFilter());
        }
        return policyId;
    }

    public void setPolicyId(UUIDFilter policyId) {
        this.policyId = policyId;
    }

    public UUIDFilter getDocumentLineageId() {
        return documentLineageId;
    }

    public Optional<UUIDFilter> optionalDocumentLineageId() {
        return Optional.ofNullable(documentLineageId);
    }

    public UUIDFilter documentLineageId() {
        if (documentLineageId == null) {
            setDocumentLineageId(new UUIDFilter());
        }
        return documentLineageId;
    }

    public void setDocumentLineageId(UUIDFilter documentLineageId) {
        this.documentLineageId = documentLineageId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DocumentCriteria that = (DocumentCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(type, that.type) &&
            Objects.equals(title, that.title) &&
            Objects.equals(version, that.version) &&
            Objects.equals(authorId, that.authorId) &&
            Objects.equals(callerService, that.callerService) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(visibility, that.visibility) &&
            Objects.equals(ownerUnit, that.ownerUnit) &&
            Objects.equals(restrictedReaderIds, that.restrictedReaderIds) &&
            Objects.equals(depositedAt, that.depositedAt) &&
            Objects.equals(sha256, that.sha256) &&
            Objects.equals(sizeBytes, that.sizeBytes) &&
            Objects.equals(mediaType, that.mediaType) &&
            Objects.equals(objectKey, that.objectKey) &&
            Objects.equals(state, that.state) &&
            Objects.equals(retentionPeriod, that.retentionPeriod) &&
            Objects.equals(retentionMode, that.retentionMode) &&
            Objects.equals(legalHold, that.legalHold) &&
            Objects.equals(retainUntil, that.retainUntil) &&
            Objects.equals(lockedAt, that.lockedAt) &&
            Objects.equals(scanClean, that.scanClean) &&
            Objects.equals(scanSignatureName, that.scanSignatureName) &&
            Objects.equals(scanEngineVersion, that.scanEngineVersion) &&
            Objects.equals(scannedAt, that.scannedAt) &&
            Objects.equals(maskingCandidates, that.maskingCandidates) &&
            Objects.equals(maskingLuhnValid, that.maskingLuhnValid) &&
            Objects.equals(maskingMasked, that.maskingMasked) &&
            Objects.equals(maskingRejected, that.maskingRejected) &&
            Objects.equals(pdfaCompliant, that.pdfaCompliant) &&
            Objects.equals(rejectionReason, that.rejectionReason) &&
            Objects.equals(sealedAt, that.sealedAt) &&
            Objects.equals(purgedAt, that.purgedAt) &&
            Objects.equals(lineageId, that.lineageId) &&
            Objects.equals(policyId, that.policyId) &&
            Objects.equals(documentLineageId, that.documentLineageId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            type,
            title,
            version,
            authorId,
            callerService,
            institutionId,
            visibility,
            ownerUnit,
            restrictedReaderIds,
            depositedAt,
            sha256,
            sizeBytes,
            mediaType,
            objectKey,
            state,
            retentionPeriod,
            retentionMode,
            legalHold,
            retainUntil,
            lockedAt,
            scanClean,
            scanSignatureName,
            scanEngineVersion,
            scannedAt,
            maskingCandidates,
            maskingLuhnValid,
            maskingMasked,
            maskingRejected,
            pdfaCompliant,
            rejectionReason,
            sealedAt,
            purgedAt,
            lineageId,
            policyId,
            documentLineageId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DocumentCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalVersion().map(f -> "version=" + f + ", ").orElse("") +
            optionalAuthorId().map(f -> "authorId=" + f + ", ").orElse("") +
            optionalCallerService().map(f -> "callerService=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalVisibility().map(f -> "visibility=" + f + ", ").orElse("") +
            optionalOwnerUnit().map(f -> "ownerUnit=" + f + ", ").orElse("") +
            optionalRestrictedReaderIds().map(f -> "restrictedReaderIds=" + f + ", ").orElse("") +
            optionalDepositedAt().map(f -> "depositedAt=" + f + ", ").orElse("") +
            optionalSha256().map(f -> "sha256=" + f + ", ").orElse("") +
            optionalSizeBytes().map(f -> "sizeBytes=" + f + ", ").orElse("") +
            optionalMediaType().map(f -> "mediaType=" + f + ", ").orElse("") +
            optionalObjectKey().map(f -> "objectKey=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalRetentionPeriod().map(f -> "retentionPeriod=" + f + ", ").orElse("") +
            optionalRetentionMode().map(f -> "retentionMode=" + f + ", ").orElse("") +
            optionalLegalHold().map(f -> "legalHold=" + f + ", ").orElse("") +
            optionalRetainUntil().map(f -> "retainUntil=" + f + ", ").orElse("") +
            optionalLockedAt().map(f -> "lockedAt=" + f + ", ").orElse("") +
            optionalScanClean().map(f -> "scanClean=" + f + ", ").orElse("") +
            optionalScanSignatureName().map(f -> "scanSignatureName=" + f + ", ").orElse("") +
            optionalScanEngineVersion().map(f -> "scanEngineVersion=" + f + ", ").orElse("") +
            optionalScannedAt().map(f -> "scannedAt=" + f + ", ").orElse("") +
            optionalMaskingCandidates().map(f -> "maskingCandidates=" + f + ", ").orElse("") +
            optionalMaskingLuhnValid().map(f -> "maskingLuhnValid=" + f + ", ").orElse("") +
            optionalMaskingMasked().map(f -> "maskingMasked=" + f + ", ").orElse("") +
            optionalMaskingRejected().map(f -> "maskingRejected=" + f + ", ").orElse("") +
            optionalPdfaCompliant().map(f -> "pdfaCompliant=" + f + ", ").orElse("") +
            optionalRejectionReason().map(f -> "rejectionReason=" + f + ", ").orElse("") +
            optionalSealedAt().map(f -> "sealedAt=" + f + ", ").orElse("") +
            optionalPurgedAt().map(f -> "purgedAt=" + f + ", ").orElse("") +
            optionalLineageId().map(f -> "lineageId=" + f + ", ").orElse("") +
            optionalPolicyId().map(f -> "policyId=" + f + ", ").orElse("") +
            optionalDocumentLineageId().map(f -> "documentLineageId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
