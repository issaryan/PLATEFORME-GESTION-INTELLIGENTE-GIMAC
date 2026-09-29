package cm.gimac.des.participant.service.criteria;

import cm.gimac.des.participant.domain.enumeration.LegalDocumentState;
import cm.gimac.des.participant.domain.enumeration.LegalDocumentType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.participant.domain.LegalDocument} entity. This class is used
 * in {@link cm.gimac.des.participant.web.rest.LegalDocumentResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /legal-documents?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LegalDocumentCriteria implements Serializable, Criteria {

    /**
     * Class for filtering LegalDocumentType
     */
    public static class LegalDocumentTypeFilter extends Filter<LegalDocumentType> {

        public LegalDocumentTypeFilter() {}

        public LegalDocumentTypeFilter(LegalDocumentTypeFilter filter) {
            super(filter);
        }

        @Override
        public LegalDocumentTypeFilter copy() {
            return new LegalDocumentTypeFilter(this);
        }
    }

    /**
     * Class for filtering LegalDocumentState
     */
    public static class LegalDocumentStateFilter extends Filter<LegalDocumentState> {

        public LegalDocumentStateFilter() {}

        public LegalDocumentStateFilter(LegalDocumentStateFilter filter) {
            super(filter);
        }

        @Override
        public LegalDocumentStateFilter copy() {
            return new LegalDocumentStateFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private LegalDocumentTypeFilter type;

    private StringFilter reference;

    private StringFilter issuingAuthority;

    private LocalDateFilter issuedOn;

    private LocalDateFilter expiresOn;

    private UUIDFilter documentId;

    private IntegerFilter documentVersion;

    private StringFilter digest;

    private BooleanFilter sealed;

    private LegalDocumentStateFilter state;

    private UUIDFilter previousDocumentId;

    private UUIDFilter institutionId;

    private UUIDFilter legalDocumentId;

    private Boolean distinct;

    public LegalDocumentCriteria() {}

    public LegalDocumentCriteria(LegalDocumentCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.type = other.optionalType().map(LegalDocumentTypeFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.issuingAuthority = other.optionalIssuingAuthority().map(StringFilter::copy).orElse(null);
        this.issuedOn = other.optionalIssuedOn().map(LocalDateFilter::copy).orElse(null);
        this.expiresOn = other.optionalExpiresOn().map(LocalDateFilter::copy).orElse(null);
        this.documentId = other.optionalDocumentId().map(UUIDFilter::copy).orElse(null);
        this.documentVersion = other.optionalDocumentVersion().map(IntegerFilter::copy).orElse(null);
        this.digest = other.optionalDigest().map(StringFilter::copy).orElse(null);
        this.sealed = other.optionalSealed().map(BooleanFilter::copy).orElse(null);
        this.state = other.optionalState().map(LegalDocumentStateFilter::copy).orElse(null);
        this.previousDocumentId = other.optionalPreviousDocumentId().map(UUIDFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.legalDocumentId = other.optionalLegalDocumentId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public LegalDocumentCriteria copy() {
        return new LegalDocumentCriteria(this);
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

    public LegalDocumentTypeFilter getType() {
        return type;
    }

    public Optional<LegalDocumentTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public LegalDocumentTypeFilter type() {
        if (type == null) {
            setType(new LegalDocumentTypeFilter());
        }
        return type;
    }

    public void setType(LegalDocumentTypeFilter type) {
        this.type = type;
    }

    public StringFilter getReference() {
        return reference;
    }

    public Optional<StringFilter> optionalReference() {
        return Optional.ofNullable(reference);
    }

    public StringFilter reference() {
        if (reference == null) {
            setReference(new StringFilter());
        }
        return reference;
    }

    public void setReference(StringFilter reference) {
        this.reference = reference;
    }

    public StringFilter getIssuingAuthority() {
        return issuingAuthority;
    }

    public Optional<StringFilter> optionalIssuingAuthority() {
        return Optional.ofNullable(issuingAuthority);
    }

    public StringFilter issuingAuthority() {
        if (issuingAuthority == null) {
            setIssuingAuthority(new StringFilter());
        }
        return issuingAuthority;
    }

    public void setIssuingAuthority(StringFilter issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public LocalDateFilter getIssuedOn() {
        return issuedOn;
    }

    public Optional<LocalDateFilter> optionalIssuedOn() {
        return Optional.ofNullable(issuedOn);
    }

    public LocalDateFilter issuedOn() {
        if (issuedOn == null) {
            setIssuedOn(new LocalDateFilter());
        }
        return issuedOn;
    }

    public void setIssuedOn(LocalDateFilter issuedOn) {
        this.issuedOn = issuedOn;
    }

    public LocalDateFilter getExpiresOn() {
        return expiresOn;
    }

    public Optional<LocalDateFilter> optionalExpiresOn() {
        return Optional.ofNullable(expiresOn);
    }

    public LocalDateFilter expiresOn() {
        if (expiresOn == null) {
            setExpiresOn(new LocalDateFilter());
        }
        return expiresOn;
    }

    public void setExpiresOn(LocalDateFilter expiresOn) {
        this.expiresOn = expiresOn;
    }

    public UUIDFilter getDocumentId() {
        return documentId;
    }

    public Optional<UUIDFilter> optionalDocumentId() {
        return Optional.ofNullable(documentId);
    }

    public UUIDFilter documentId() {
        if (documentId == null) {
            setDocumentId(new UUIDFilter());
        }
        return documentId;
    }

    public void setDocumentId(UUIDFilter documentId) {
        this.documentId = documentId;
    }

    public IntegerFilter getDocumentVersion() {
        return documentVersion;
    }

    public Optional<IntegerFilter> optionalDocumentVersion() {
        return Optional.ofNullable(documentVersion);
    }

    public IntegerFilter documentVersion() {
        if (documentVersion == null) {
            setDocumentVersion(new IntegerFilter());
        }
        return documentVersion;
    }

    public void setDocumentVersion(IntegerFilter documentVersion) {
        this.documentVersion = documentVersion;
    }

    public StringFilter getDigest() {
        return digest;
    }

    public Optional<StringFilter> optionalDigest() {
        return Optional.ofNullable(digest);
    }

    public StringFilter digest() {
        if (digest == null) {
            setDigest(new StringFilter());
        }
        return digest;
    }

    public void setDigest(StringFilter digest) {
        this.digest = digest;
    }

    public BooleanFilter getSealed() {
        return sealed;
    }

    public Optional<BooleanFilter> optionalSealed() {
        return Optional.ofNullable(sealed);
    }

    public BooleanFilter sealed() {
        if (sealed == null) {
            setSealed(new BooleanFilter());
        }
        return sealed;
    }

    public void setSealed(BooleanFilter sealed) {
        this.sealed = sealed;
    }

    public LegalDocumentStateFilter getState() {
        return state;
    }

    public Optional<LegalDocumentStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public LegalDocumentStateFilter state() {
        if (state == null) {
            setState(new LegalDocumentStateFilter());
        }
        return state;
    }

    public void setState(LegalDocumentStateFilter state) {
        this.state = state;
    }

    public UUIDFilter getPreviousDocumentId() {
        return previousDocumentId;
    }

    public Optional<UUIDFilter> optionalPreviousDocumentId() {
        return Optional.ofNullable(previousDocumentId);
    }

    public UUIDFilter previousDocumentId() {
        if (previousDocumentId == null) {
            setPreviousDocumentId(new UUIDFilter());
        }
        return previousDocumentId;
    }

    public void setPreviousDocumentId(UUIDFilter previousDocumentId) {
        this.previousDocumentId = previousDocumentId;
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

    public UUIDFilter getLegalDocumentId() {
        return legalDocumentId;
    }

    public Optional<UUIDFilter> optionalLegalDocumentId() {
        return Optional.ofNullable(legalDocumentId);
    }

    public UUIDFilter legalDocumentId() {
        if (legalDocumentId == null) {
            setLegalDocumentId(new UUIDFilter());
        }
        return legalDocumentId;
    }

    public void setLegalDocumentId(UUIDFilter legalDocumentId) {
        this.legalDocumentId = legalDocumentId;
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
        final LegalDocumentCriteria that = (LegalDocumentCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(type, that.type) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(issuingAuthority, that.issuingAuthority) &&
            Objects.equals(issuedOn, that.issuedOn) &&
            Objects.equals(expiresOn, that.expiresOn) &&
            Objects.equals(documentId, that.documentId) &&
            Objects.equals(documentVersion, that.documentVersion) &&
            Objects.equals(digest, that.digest) &&
            Objects.equals(sealed, that.sealed) &&
            Objects.equals(state, that.state) &&
            Objects.equals(previousDocumentId, that.previousDocumentId) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(legalDocumentId, that.legalDocumentId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            type,
            reference,
            issuingAuthority,
            issuedOn,
            expiresOn,
            documentId,
            documentVersion,
            digest,
            sealed,
            state,
            previousDocumentId,
            institutionId,
            legalDocumentId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LegalDocumentCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalIssuingAuthority().map(f -> "issuingAuthority=" + f + ", ").orElse("") +
            optionalIssuedOn().map(f -> "issuedOn=" + f + ", ").orElse("") +
            optionalExpiresOn().map(f -> "expiresOn=" + f + ", ").orElse("") +
            optionalDocumentId().map(f -> "documentId=" + f + ", ").orElse("") +
            optionalDocumentVersion().map(f -> "documentVersion=" + f + ", ").orElse("") +
            optionalDigest().map(f -> "digest=" + f + ", ").orElse("") +
            optionalSealed().map(f -> "sealed=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalPreviousDocumentId().map(f -> "previousDocumentId=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalLegalDocumentId().map(f -> "legalDocumentId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
