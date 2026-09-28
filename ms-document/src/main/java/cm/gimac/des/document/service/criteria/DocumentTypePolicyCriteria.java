package cm.gimac.des.document.service.criteria;

import cm.gimac.des.document.domain.enumeration.DocumentType;
import cm.gimac.des.document.domain.enumeration.MaskingRule;
import cm.gimac.des.document.domain.enumeration.RetentionMode;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.document.domain.DocumentTypePolicy} entity. This class is used
 * in {@link cm.gimac.des.document.web.rest.DocumentTypePolicyResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /document-type-policies?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DocumentTypePolicyCriteria implements Serializable, Criteria {

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

    /**
     * Class for filtering MaskingRule
     */
    public static class MaskingRuleFilter extends Filter<MaskingRule> {

        public MaskingRuleFilter() {}

        public MaskingRuleFilter(MaskingRuleFilter filter) {
            super(filter);
        }

        @Override
        public MaskingRuleFilter copy() {
            return new MaskingRuleFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private DocumentTypeFilter documentType;

    private IntegerFilter policyVersion;

    private StringFilter retentionPeriod;

    private RetentionModeFilter retentionMode;

    private MaskingRuleFilter maskingRule;

    private BooleanFilter restrictedAccess;

    private BooleanFilter pdfaRequired;

    private StringFilter allowedMediaTypes;

    private LongFilter maxSizeBytes;

    private StringFilter allowedCallers;

    private InstantFilter effectiveFrom;

    private BooleanFilter active;

    private Boolean distinct;

    public DocumentTypePolicyCriteria() {}

    public DocumentTypePolicyCriteria(DocumentTypePolicyCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.documentType = other.optionalDocumentType().map(DocumentTypeFilter::copy).orElse(null);
        this.policyVersion = other.optionalPolicyVersion().map(IntegerFilter::copy).orElse(null);
        this.retentionPeriod = other.optionalRetentionPeriod().map(StringFilter::copy).orElse(null);
        this.retentionMode = other.optionalRetentionMode().map(RetentionModeFilter::copy).orElse(null);
        this.maskingRule = other.optionalMaskingRule().map(MaskingRuleFilter::copy).orElse(null);
        this.restrictedAccess = other.optionalRestrictedAccess().map(BooleanFilter::copy).orElse(null);
        this.pdfaRequired = other.optionalPdfaRequired().map(BooleanFilter::copy).orElse(null);
        this.allowedMediaTypes = other.optionalAllowedMediaTypes().map(StringFilter::copy).orElse(null);
        this.maxSizeBytes = other.optionalMaxSizeBytes().map(LongFilter::copy).orElse(null);
        this.allowedCallers = other.optionalAllowedCallers().map(StringFilter::copy).orElse(null);
        this.effectiveFrom = other.optionalEffectiveFrom().map(InstantFilter::copy).orElse(null);
        this.active = other.optionalActive().map(BooleanFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DocumentTypePolicyCriteria copy() {
        return new DocumentTypePolicyCriteria(this);
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

    public DocumentTypeFilter getDocumentType() {
        return documentType;
    }

    public Optional<DocumentTypeFilter> optionalDocumentType() {
        return Optional.ofNullable(documentType);
    }

    public DocumentTypeFilter documentType() {
        if (documentType == null) {
            setDocumentType(new DocumentTypeFilter());
        }
        return documentType;
    }

    public void setDocumentType(DocumentTypeFilter documentType) {
        this.documentType = documentType;
    }

    public IntegerFilter getPolicyVersion() {
        return policyVersion;
    }

    public Optional<IntegerFilter> optionalPolicyVersion() {
        return Optional.ofNullable(policyVersion);
    }

    public IntegerFilter policyVersion() {
        if (policyVersion == null) {
            setPolicyVersion(new IntegerFilter());
        }
        return policyVersion;
    }

    public void setPolicyVersion(IntegerFilter policyVersion) {
        this.policyVersion = policyVersion;
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

    public MaskingRuleFilter getMaskingRule() {
        return maskingRule;
    }

    public Optional<MaskingRuleFilter> optionalMaskingRule() {
        return Optional.ofNullable(maskingRule);
    }

    public MaskingRuleFilter maskingRule() {
        if (maskingRule == null) {
            setMaskingRule(new MaskingRuleFilter());
        }
        return maskingRule;
    }

    public void setMaskingRule(MaskingRuleFilter maskingRule) {
        this.maskingRule = maskingRule;
    }

    public BooleanFilter getRestrictedAccess() {
        return restrictedAccess;
    }

    public Optional<BooleanFilter> optionalRestrictedAccess() {
        return Optional.ofNullable(restrictedAccess);
    }

    public BooleanFilter restrictedAccess() {
        if (restrictedAccess == null) {
            setRestrictedAccess(new BooleanFilter());
        }
        return restrictedAccess;
    }

    public void setRestrictedAccess(BooleanFilter restrictedAccess) {
        this.restrictedAccess = restrictedAccess;
    }

    public BooleanFilter getPdfaRequired() {
        return pdfaRequired;
    }

    public Optional<BooleanFilter> optionalPdfaRequired() {
        return Optional.ofNullable(pdfaRequired);
    }

    public BooleanFilter pdfaRequired() {
        if (pdfaRequired == null) {
            setPdfaRequired(new BooleanFilter());
        }
        return pdfaRequired;
    }

    public void setPdfaRequired(BooleanFilter pdfaRequired) {
        this.pdfaRequired = pdfaRequired;
    }

    public StringFilter getAllowedMediaTypes() {
        return allowedMediaTypes;
    }

    public Optional<StringFilter> optionalAllowedMediaTypes() {
        return Optional.ofNullable(allowedMediaTypes);
    }

    public StringFilter allowedMediaTypes() {
        if (allowedMediaTypes == null) {
            setAllowedMediaTypes(new StringFilter());
        }
        return allowedMediaTypes;
    }

    public void setAllowedMediaTypes(StringFilter allowedMediaTypes) {
        this.allowedMediaTypes = allowedMediaTypes;
    }

    public LongFilter getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public Optional<LongFilter> optionalMaxSizeBytes() {
        return Optional.ofNullable(maxSizeBytes);
    }

    public LongFilter maxSizeBytes() {
        if (maxSizeBytes == null) {
            setMaxSizeBytes(new LongFilter());
        }
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(LongFilter maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public StringFilter getAllowedCallers() {
        return allowedCallers;
    }

    public Optional<StringFilter> optionalAllowedCallers() {
        return Optional.ofNullable(allowedCallers);
    }

    public StringFilter allowedCallers() {
        if (allowedCallers == null) {
            setAllowedCallers(new StringFilter());
        }
        return allowedCallers;
    }

    public void setAllowedCallers(StringFilter allowedCallers) {
        this.allowedCallers = allowedCallers;
    }

    public InstantFilter getEffectiveFrom() {
        return effectiveFrom;
    }

    public Optional<InstantFilter> optionalEffectiveFrom() {
        return Optional.ofNullable(effectiveFrom);
    }

    public InstantFilter effectiveFrom() {
        if (effectiveFrom == null) {
            setEffectiveFrom(new InstantFilter());
        }
        return effectiveFrom;
    }

    public void setEffectiveFrom(InstantFilter effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public BooleanFilter getActive() {
        return active;
    }

    public Optional<BooleanFilter> optionalActive() {
        return Optional.ofNullable(active);
    }

    public BooleanFilter active() {
        if (active == null) {
            setActive(new BooleanFilter());
        }
        return active;
    }

    public void setActive(BooleanFilter active) {
        this.active = active;
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
        final DocumentTypePolicyCriteria that = (DocumentTypePolicyCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(documentType, that.documentType) &&
            Objects.equals(policyVersion, that.policyVersion) &&
            Objects.equals(retentionPeriod, that.retentionPeriod) &&
            Objects.equals(retentionMode, that.retentionMode) &&
            Objects.equals(maskingRule, that.maskingRule) &&
            Objects.equals(restrictedAccess, that.restrictedAccess) &&
            Objects.equals(pdfaRequired, that.pdfaRequired) &&
            Objects.equals(allowedMediaTypes, that.allowedMediaTypes) &&
            Objects.equals(maxSizeBytes, that.maxSizeBytes) &&
            Objects.equals(allowedCallers, that.allowedCallers) &&
            Objects.equals(effectiveFrom, that.effectiveFrom) &&
            Objects.equals(active, that.active) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            documentType,
            policyVersion,
            retentionPeriod,
            retentionMode,
            maskingRule,
            restrictedAccess,
            pdfaRequired,
            allowedMediaTypes,
            maxSizeBytes,
            allowedCallers,
            effectiveFrom,
            active,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DocumentTypePolicyCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalDocumentType().map(f -> "documentType=" + f + ", ").orElse("") +
            optionalPolicyVersion().map(f -> "policyVersion=" + f + ", ").orElse("") +
            optionalRetentionPeriod().map(f -> "retentionPeriod=" + f + ", ").orElse("") +
            optionalRetentionMode().map(f -> "retentionMode=" + f + ", ").orElse("") +
            optionalMaskingRule().map(f -> "maskingRule=" + f + ", ").orElse("") +
            optionalRestrictedAccess().map(f -> "restrictedAccess=" + f + ", ").orElse("") +
            optionalPdfaRequired().map(f -> "pdfaRequired=" + f + ", ").orElse("") +
            optionalAllowedMediaTypes().map(f -> "allowedMediaTypes=" + f + ", ").orElse("") +
            optionalMaxSizeBytes().map(f -> "maxSizeBytes=" + f + ", ").orElse("") +
            optionalAllowedCallers().map(f -> "allowedCallers=" + f + ", ").orElse("") +
            optionalEffectiveFrom().map(f -> "effectiveFrom=" + f + ", ").orElse("") +
            optionalActive().map(f -> "active=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
