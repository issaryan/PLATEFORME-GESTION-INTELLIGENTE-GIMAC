package cm.gimac.des.participant.service.criteria;

import cm.gimac.des.participant.domain.enumeration.ConventionType;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.participant.domain.Convention} entity. This class is used
 * in {@link cm.gimac.des.participant.web.rest.ConventionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /conventions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConventionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ConventionType
     */
    public static class ConventionTypeFilter extends Filter<ConventionType> {

        public ConventionTypeFilter() {}

        public ConventionTypeFilter(ConventionTypeFilter filter) {
            super(filter);
        }

        @Override
        public ConventionTypeFilter copy() {
            return new ConventionTypeFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private ConventionTypeFilter type;

    private StringFilter reference;

    private LocalDateFilter signedOn;

    private LocalDateFilter expiresOn;

    private UUIDFilter documentId;

    private StringFilter documentSha256;

    private UUIDFilter institutionId;

    private Boolean distinct;

    public ConventionCriteria() {}

    public ConventionCriteria(ConventionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.type = other.optionalType().map(ConventionTypeFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.signedOn = other.optionalSignedOn().map(LocalDateFilter::copy).orElse(null);
        this.expiresOn = other.optionalExpiresOn().map(LocalDateFilter::copy).orElse(null);
        this.documentId = other.optionalDocumentId().map(UUIDFilter::copy).orElse(null);
        this.documentSha256 = other.optionalDocumentSha256().map(StringFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ConventionCriteria copy() {
        return new ConventionCriteria(this);
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

    public ConventionTypeFilter getType() {
        return type;
    }

    public Optional<ConventionTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public ConventionTypeFilter type() {
        if (type == null) {
            setType(new ConventionTypeFilter());
        }
        return type;
    }

    public void setType(ConventionTypeFilter type) {
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

    public LocalDateFilter getSignedOn() {
        return signedOn;
    }

    public Optional<LocalDateFilter> optionalSignedOn() {
        return Optional.ofNullable(signedOn);
    }

    public LocalDateFilter signedOn() {
        if (signedOn == null) {
            setSignedOn(new LocalDateFilter());
        }
        return signedOn;
    }

    public void setSignedOn(LocalDateFilter signedOn) {
        this.signedOn = signedOn;
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

    public StringFilter getDocumentSha256() {
        return documentSha256;
    }

    public Optional<StringFilter> optionalDocumentSha256() {
        return Optional.ofNullable(documentSha256);
    }

    public StringFilter documentSha256() {
        if (documentSha256 == null) {
            setDocumentSha256(new StringFilter());
        }
        return documentSha256;
    }

    public void setDocumentSha256(StringFilter documentSha256) {
        this.documentSha256 = documentSha256;
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
        final ConventionCriteria that = (ConventionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(type, that.type) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(signedOn, that.signedOn) &&
            Objects.equals(expiresOn, that.expiresOn) &&
            Objects.equals(documentId, that.documentId) &&
            Objects.equals(documentSha256, that.documentSha256) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, reference, signedOn, expiresOn, documentId, documentSha256, institutionId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConventionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalSignedOn().map(f -> "signedOn=" + f + ", ").orElse("") +
            optionalExpiresOn().map(f -> "expiresOn=" + f + ", ").orElse("") +
            optionalDocumentId().map(f -> "documentId=" + f + ", ").orElse("") +
            optionalDocumentSha256().map(f -> "documentSha256=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
