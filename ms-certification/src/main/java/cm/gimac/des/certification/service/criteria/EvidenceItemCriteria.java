package cm.gimac.des.certification.service.criteria;

import cm.gimac.des.certification.domain.enumeration.EvidenceKind;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.certification.domain.EvidenceItem} entity. This class is used
 * in {@link cm.gimac.des.certification.web.rest.EvidenceItemResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /evidence-items?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvidenceItemCriteria implements Serializable, Criteria {

    /**
     * Class for filtering EvidenceKind
     */
    public static class EvidenceKindFilter extends Filter<EvidenceKind> {

        public EvidenceKindFilter() {}

        public EvidenceKindFilter(EvidenceKindFilter filter) {
            super(filter);
        }

        @Override
        public EvidenceKindFilter copy() {
            return new EvidenceKindFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter documentId;

    private EvidenceKindFilter kind;

    private StringFilter digest;

    private InstantFilter sealedAt;

    private BooleanFilter sealConfirmed;

    private InstantFilter lastVerifiedAt;

    private UUIDFilter attachedBy;

    private InstantFilter attachedAt;

    private UUIDFilter fileId;

    private UUIDFilter commissioningFileId;

    private Boolean distinct;

    public EvidenceItemCriteria() {}

    public EvidenceItemCriteria(EvidenceItemCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.documentId = other.optionalDocumentId().map(UUIDFilter::copy).orElse(null);
        this.kind = other.optionalKind().map(EvidenceKindFilter::copy).orElse(null);
        this.digest = other.optionalDigest().map(StringFilter::copy).orElse(null);
        this.sealedAt = other.optionalSealedAt().map(InstantFilter::copy).orElse(null);
        this.sealConfirmed = other.optionalSealConfirmed().map(BooleanFilter::copy).orElse(null);
        this.lastVerifiedAt = other.optionalLastVerifiedAt().map(InstantFilter::copy).orElse(null);
        this.attachedBy = other.optionalAttachedBy().map(UUIDFilter::copy).orElse(null);
        this.attachedAt = other.optionalAttachedAt().map(InstantFilter::copy).orElse(null);
        this.fileId = other.optionalFileId().map(UUIDFilter::copy).orElse(null);
        this.commissioningFileId = other.optionalCommissioningFileId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public EvidenceItemCriteria copy() {
        return new EvidenceItemCriteria(this);
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

    public EvidenceKindFilter getKind() {
        return kind;
    }

    public Optional<EvidenceKindFilter> optionalKind() {
        return Optional.ofNullable(kind);
    }

    public EvidenceKindFilter kind() {
        if (kind == null) {
            setKind(new EvidenceKindFilter());
        }
        return kind;
    }

    public void setKind(EvidenceKindFilter kind) {
        this.kind = kind;
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

    public BooleanFilter getSealConfirmed() {
        return sealConfirmed;
    }

    public Optional<BooleanFilter> optionalSealConfirmed() {
        return Optional.ofNullable(sealConfirmed);
    }

    public BooleanFilter sealConfirmed() {
        if (sealConfirmed == null) {
            setSealConfirmed(new BooleanFilter());
        }
        return sealConfirmed;
    }

    public void setSealConfirmed(BooleanFilter sealConfirmed) {
        this.sealConfirmed = sealConfirmed;
    }

    public InstantFilter getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public Optional<InstantFilter> optionalLastVerifiedAt() {
        return Optional.ofNullable(lastVerifiedAt);
    }

    public InstantFilter lastVerifiedAt() {
        if (lastVerifiedAt == null) {
            setLastVerifiedAt(new InstantFilter());
        }
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(InstantFilter lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public UUIDFilter getAttachedBy() {
        return attachedBy;
    }

    public Optional<UUIDFilter> optionalAttachedBy() {
        return Optional.ofNullable(attachedBy);
    }

    public UUIDFilter attachedBy() {
        if (attachedBy == null) {
            setAttachedBy(new UUIDFilter());
        }
        return attachedBy;
    }

    public void setAttachedBy(UUIDFilter attachedBy) {
        this.attachedBy = attachedBy;
    }

    public InstantFilter getAttachedAt() {
        return attachedAt;
    }

    public Optional<InstantFilter> optionalAttachedAt() {
        return Optional.ofNullable(attachedAt);
    }

    public InstantFilter attachedAt() {
        if (attachedAt == null) {
            setAttachedAt(new InstantFilter());
        }
        return attachedAt;
    }

    public void setAttachedAt(InstantFilter attachedAt) {
        this.attachedAt = attachedAt;
    }

    public UUIDFilter getFileId() {
        return fileId;
    }

    public Optional<UUIDFilter> optionalFileId() {
        return Optional.ofNullable(fileId);
    }

    public UUIDFilter fileId() {
        if (fileId == null) {
            setFileId(new UUIDFilter());
        }
        return fileId;
    }

    public void setFileId(UUIDFilter fileId) {
        this.fileId = fileId;
    }

    public UUIDFilter getCommissioningFileId() {
        return commissioningFileId;
    }

    public Optional<UUIDFilter> optionalCommissioningFileId() {
        return Optional.ofNullable(commissioningFileId);
    }

    public UUIDFilter commissioningFileId() {
        if (commissioningFileId == null) {
            setCommissioningFileId(new UUIDFilter());
        }
        return commissioningFileId;
    }

    public void setCommissioningFileId(UUIDFilter commissioningFileId) {
        this.commissioningFileId = commissioningFileId;
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
        final EvidenceItemCriteria that = (EvidenceItemCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(documentId, that.documentId) &&
            Objects.equals(kind, that.kind) &&
            Objects.equals(digest, that.digest) &&
            Objects.equals(sealedAt, that.sealedAt) &&
            Objects.equals(sealConfirmed, that.sealConfirmed) &&
            Objects.equals(lastVerifiedAt, that.lastVerifiedAt) &&
            Objects.equals(attachedBy, that.attachedBy) &&
            Objects.equals(attachedAt, that.attachedAt) &&
            Objects.equals(fileId, that.fileId) &&
            Objects.equals(commissioningFileId, that.commissioningFileId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            documentId,
            kind,
            digest,
            sealedAt,
            sealConfirmed,
            lastVerifiedAt,
            attachedBy,
            attachedAt,
            fileId,
            commissioningFileId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvidenceItemCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalDocumentId().map(f -> "documentId=" + f + ", ").orElse("") +
            optionalKind().map(f -> "kind=" + f + ", ").orElse("") +
            optionalDigest().map(f -> "digest=" + f + ", ").orElse("") +
            optionalSealedAt().map(f -> "sealedAt=" + f + ", ").orElse("") +
            optionalSealConfirmed().map(f -> "sealConfirmed=" + f + ", ").orElse("") +
            optionalLastVerifiedAt().map(f -> "lastVerifiedAt=" + f + ", ").orElse("") +
            optionalAttachedBy().map(f -> "attachedBy=" + f + ", ").orElse("") +
            optionalAttachedAt().map(f -> "attachedAt=" + f + ", ").orElse("") +
            optionalFileId().map(f -> "fileId=" + f + ", ").orElse("") +
            optionalCommissioningFileId().map(f -> "commissioningFileId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
