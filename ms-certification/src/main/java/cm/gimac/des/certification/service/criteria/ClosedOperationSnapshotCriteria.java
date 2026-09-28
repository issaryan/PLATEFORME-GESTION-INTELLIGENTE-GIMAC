package cm.gimac.des.certification.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.certification.domain.ClosedOperationSnapshot} entity. This class is used
 * in {@link cm.gimac.des.certification.web.rest.ClosedOperationSnapshotResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /closed-operation-snapshots?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ClosedOperationSnapshotCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter liaisonId;

    private UUIDFilter operationId;

    private StringFilter operationCode;

    private StringFilter referenceCode;

    private StringFilter serviceCode;

    private UUIDFilter ownerId;

    private UUIDFilter validatorId;

    private UUIDFilter evidenceDocumentId;

    private StringFilter evidenceDigest;

    private InstantFilter closedAt;

    private UUIDFilter sourceEventId;

    private Boolean distinct;

    public ClosedOperationSnapshotCriteria() {}

    public ClosedOperationSnapshotCriteria(ClosedOperationSnapshotCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.liaisonId = other.optionalLiaisonId().map(UUIDFilter::copy).orElse(null);
        this.operationId = other.optionalOperationId().map(UUIDFilter::copy).orElse(null);
        this.operationCode = other.optionalOperationCode().map(StringFilter::copy).orElse(null);
        this.referenceCode = other.optionalReferenceCode().map(StringFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(UUIDFilter::copy).orElse(null);
        this.validatorId = other.optionalValidatorId().map(UUIDFilter::copy).orElse(null);
        this.evidenceDocumentId = other.optionalEvidenceDocumentId().map(UUIDFilter::copy).orElse(null);
        this.evidenceDigest = other.optionalEvidenceDigest().map(StringFilter::copy).orElse(null);
        this.closedAt = other.optionalClosedAt().map(InstantFilter::copy).orElse(null);
        this.sourceEventId = other.optionalSourceEventId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ClosedOperationSnapshotCriteria copy() {
        return new ClosedOperationSnapshotCriteria(this);
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

    public UUIDFilter getLiaisonId() {
        return liaisonId;
    }

    public Optional<UUIDFilter> optionalLiaisonId() {
        return Optional.ofNullable(liaisonId);
    }

    public UUIDFilter liaisonId() {
        if (liaisonId == null) {
            setLiaisonId(new UUIDFilter());
        }
        return liaisonId;
    }

    public void setLiaisonId(UUIDFilter liaisonId) {
        this.liaisonId = liaisonId;
    }

    public UUIDFilter getOperationId() {
        return operationId;
    }

    public Optional<UUIDFilter> optionalOperationId() {
        return Optional.ofNullable(operationId);
    }

    public UUIDFilter operationId() {
        if (operationId == null) {
            setOperationId(new UUIDFilter());
        }
        return operationId;
    }

    public void setOperationId(UUIDFilter operationId) {
        this.operationId = operationId;
    }

    public StringFilter getOperationCode() {
        return operationCode;
    }

    public Optional<StringFilter> optionalOperationCode() {
        return Optional.ofNullable(operationCode);
    }

    public StringFilter operationCode() {
        if (operationCode == null) {
            setOperationCode(new StringFilter());
        }
        return operationCode;
    }

    public void setOperationCode(StringFilter operationCode) {
        this.operationCode = operationCode;
    }

    public StringFilter getReferenceCode() {
        return referenceCode;
    }

    public Optional<StringFilter> optionalReferenceCode() {
        return Optional.ofNullable(referenceCode);
    }

    public StringFilter referenceCode() {
        if (referenceCode == null) {
            setReferenceCode(new StringFilter());
        }
        return referenceCode;
    }

    public void setReferenceCode(StringFilter referenceCode) {
        this.referenceCode = referenceCode;
    }

    public StringFilter getServiceCode() {
        return serviceCode;
    }

    public Optional<StringFilter> optionalServiceCode() {
        return Optional.ofNullable(serviceCode);
    }

    public StringFilter serviceCode() {
        if (serviceCode == null) {
            setServiceCode(new StringFilter());
        }
        return serviceCode;
    }

    public void setServiceCode(StringFilter serviceCode) {
        this.serviceCode = serviceCode;
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

    public UUIDFilter getValidatorId() {
        return validatorId;
    }

    public Optional<UUIDFilter> optionalValidatorId() {
        return Optional.ofNullable(validatorId);
    }

    public UUIDFilter validatorId() {
        if (validatorId == null) {
            setValidatorId(new UUIDFilter());
        }
        return validatorId;
    }

    public void setValidatorId(UUIDFilter validatorId) {
        this.validatorId = validatorId;
    }

    public UUIDFilter getEvidenceDocumentId() {
        return evidenceDocumentId;
    }

    public Optional<UUIDFilter> optionalEvidenceDocumentId() {
        return Optional.ofNullable(evidenceDocumentId);
    }

    public UUIDFilter evidenceDocumentId() {
        if (evidenceDocumentId == null) {
            setEvidenceDocumentId(new UUIDFilter());
        }
        return evidenceDocumentId;
    }

    public void setEvidenceDocumentId(UUIDFilter evidenceDocumentId) {
        this.evidenceDocumentId = evidenceDocumentId;
    }

    public StringFilter getEvidenceDigest() {
        return evidenceDigest;
    }

    public Optional<StringFilter> optionalEvidenceDigest() {
        return Optional.ofNullable(evidenceDigest);
    }

    public StringFilter evidenceDigest() {
        if (evidenceDigest == null) {
            setEvidenceDigest(new StringFilter());
        }
        return evidenceDigest;
    }

    public void setEvidenceDigest(StringFilter evidenceDigest) {
        this.evidenceDigest = evidenceDigest;
    }

    public InstantFilter getClosedAt() {
        return closedAt;
    }

    public Optional<InstantFilter> optionalClosedAt() {
        return Optional.ofNullable(closedAt);
    }

    public InstantFilter closedAt() {
        if (closedAt == null) {
            setClosedAt(new InstantFilter());
        }
        return closedAt;
    }

    public void setClosedAt(InstantFilter closedAt) {
        this.closedAt = closedAt;
    }

    public UUIDFilter getSourceEventId() {
        return sourceEventId;
    }

    public Optional<UUIDFilter> optionalSourceEventId() {
        return Optional.ofNullable(sourceEventId);
    }

    public UUIDFilter sourceEventId() {
        if (sourceEventId == null) {
            setSourceEventId(new UUIDFilter());
        }
        return sourceEventId;
    }

    public void setSourceEventId(UUIDFilter sourceEventId) {
        this.sourceEventId = sourceEventId;
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
        final ClosedOperationSnapshotCriteria that = (ClosedOperationSnapshotCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(liaisonId, that.liaisonId) &&
            Objects.equals(operationId, that.operationId) &&
            Objects.equals(operationCode, that.operationCode) &&
            Objects.equals(referenceCode, that.referenceCode) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(validatorId, that.validatorId) &&
            Objects.equals(evidenceDocumentId, that.evidenceDocumentId) &&
            Objects.equals(evidenceDigest, that.evidenceDigest) &&
            Objects.equals(closedAt, that.closedAt) &&
            Objects.equals(sourceEventId, that.sourceEventId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            liaisonId,
            operationId,
            operationCode,
            referenceCode,
            serviceCode,
            ownerId,
            validatorId,
            evidenceDocumentId,
            evidenceDigest,
            closedAt,
            sourceEventId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ClosedOperationSnapshotCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalLiaisonId().map(f -> "liaisonId=" + f + ", ").orElse("") +
            optionalOperationId().map(f -> "operationId=" + f + ", ").orElse("") +
            optionalOperationCode().map(f -> "operationCode=" + f + ", ").orElse("") +
            optionalReferenceCode().map(f -> "referenceCode=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalValidatorId().map(f -> "validatorId=" + f + ", ").orElse("") +
            optionalEvidenceDocumentId().map(f -> "evidenceDocumentId=" + f + ", ").orElse("") +
            optionalEvidenceDigest().map(f -> "evidenceDigest=" + f + ", ").orElse("") +
            optionalClosedAt().map(f -> "closedAt=" + f + ", ").orElse("") +
            optionalSourceEventId().map(f -> "sourceEventId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
