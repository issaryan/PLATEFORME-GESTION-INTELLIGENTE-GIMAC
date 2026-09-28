package cm.gimac.des.document.service.criteria;

import cm.gimac.des.document.domain.enumeration.DocumentType;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.document.domain.DocumentLineage} entity. This class is used
 * in {@link cm.gimac.des.document.web.rest.DocumentLineageResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /document-lineages?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DocumentLineageCriteria implements Serializable, Criteria {

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

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private DocumentTypeFilter type;

    private StringFilter ownerService;

    private UUIDFilter ownerId;

    private UUIDFilter institutionId;

    private IntegerFilter currentVersion;

    private InstantFilter createdAt;

    private UUIDFilter currentDocumentId;

    private Boolean distinct;

    public DocumentLineageCriteria() {}

    public DocumentLineageCriteria(DocumentLineageCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.type = other.optionalType().map(DocumentTypeFilter::copy).orElse(null);
        this.ownerService = other.optionalOwnerService().map(StringFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(UUIDFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.currentVersion = other.optionalCurrentVersion().map(IntegerFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.currentDocumentId = other.optionalCurrentDocumentId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DocumentLineageCriteria copy() {
        return new DocumentLineageCriteria(this);
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

    public StringFilter getOwnerService() {
        return ownerService;
    }

    public Optional<StringFilter> optionalOwnerService() {
        return Optional.ofNullable(ownerService);
    }

    public StringFilter ownerService() {
        if (ownerService == null) {
            setOwnerService(new StringFilter());
        }
        return ownerService;
    }

    public void setOwnerService(StringFilter ownerService) {
        this.ownerService = ownerService;
    }

    public UUIDFilter getOwnerId() {
        return ownerId;
    }

    public Optional<UUIDFilter> optionalOwnerId() {
        return Optional.ofNullable(ownerId);
    }

    public UUIDFilter ownerId() {
        if (ownerId == null) {
            setOwnerId(new UUIDFilter());
        }
        return ownerId;
    }

    public void setOwnerId(UUIDFilter ownerId) {
        this.ownerId = ownerId;
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

    public IntegerFilter getCurrentVersion() {
        return currentVersion;
    }

    public Optional<IntegerFilter> optionalCurrentVersion() {
        return Optional.ofNullable(currentVersion);
    }

    public IntegerFilter currentVersion() {
        if (currentVersion == null) {
            setCurrentVersion(new IntegerFilter());
        }
        return currentVersion;
    }

    public void setCurrentVersion(IntegerFilter currentVersion) {
        this.currentVersion = currentVersion;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public UUIDFilter getCurrentDocumentId() {
        return currentDocumentId;
    }

    public Optional<UUIDFilter> optionalCurrentDocumentId() {
        return Optional.ofNullable(currentDocumentId);
    }

    public UUIDFilter currentDocumentId() {
        if (currentDocumentId == null) {
            setCurrentDocumentId(new UUIDFilter());
        }
        return currentDocumentId;
    }

    public void setCurrentDocumentId(UUIDFilter currentDocumentId) {
        this.currentDocumentId = currentDocumentId;
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
        final DocumentLineageCriteria that = (DocumentLineageCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(type, that.type) &&
            Objects.equals(ownerService, that.ownerService) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(currentVersion, that.currentVersion) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(currentDocumentId, that.currentDocumentId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, ownerService, ownerId, institutionId, currentVersion, createdAt, currentDocumentId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DocumentLineageCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalOwnerService().map(f -> "ownerService=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalCurrentVersion().map(f -> "currentVersion=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalCurrentDocumentId().map(f -> "currentDocumentId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
