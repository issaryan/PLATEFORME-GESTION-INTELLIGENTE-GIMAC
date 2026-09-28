package cm.gimac.des.certification.service.criteria;

import cm.gimac.des.certification.domain.enumeration.QuitusDecision;
import cm.gimac.des.certification.domain.enumeration.QuitusStatus;
import cm.gimac.des.certification.domain.enumeration.QuitusType;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.certification.domain.Quitus} entity. This class is used
 * in {@link cm.gimac.des.certification.web.rest.QuitusResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /quituses?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class QuitusCriteria implements Serializable, Criteria {

    /**
     * Class for filtering QuitusType
     */
    public static class QuitusTypeFilter extends Filter<QuitusType> {

        public QuitusTypeFilter() {}

        public QuitusTypeFilter(QuitusTypeFilter filter) {
            super(filter);
        }

        @Override
        public QuitusTypeFilter copy() {
            return new QuitusTypeFilter(this);
        }
    }

    /**
     * Class for filtering QuitusStatus
     */
    public static class QuitusStatusFilter extends Filter<QuitusStatus> {

        public QuitusStatusFilter() {}

        public QuitusStatusFilter(QuitusStatusFilter filter) {
            super(filter);
        }

        @Override
        public QuitusStatusFilter copy() {
            return new QuitusStatusFilter(this);
        }
    }

    /**
     * Class for filtering QuitusDecision
     */
    public static class QuitusDecisionFilter extends Filter<QuitusDecision> {

        public QuitusDecisionFilter() {}

        public QuitusDecisionFilter(QuitusDecisionFilter filter) {
            super(filter);
        }

        @Override
        public QuitusDecisionFilter copy() {
            return new QuitusDecisionFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private QuitusTypeFilter type;

    private QuitusStatusFilter status;

    private QuitusDecisionFilter decision;

    private UUIDFilter signerId;

    private StringFilter reason;

    private StringFilter basisDigest;

    private StringFilter decisionDigest;

    private StringFilter credentialId;

    private LongFilter signCount;

    private InstantFilter signedAt;

    private InstantFilter dueAt;

    private InstantFilter invalidatedAt;

    private UUIDFilter fileId;

    private Boolean distinct;

    public QuitusCriteria() {}

    public QuitusCriteria(QuitusCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.type = other.optionalType().map(QuitusTypeFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(QuitusStatusFilter::copy).orElse(null);
        this.decision = other.optionalDecision().map(QuitusDecisionFilter::copy).orElse(null);
        this.signerId = other.optionalSignerId().map(UUIDFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.basisDigest = other.optionalBasisDigest().map(StringFilter::copy).orElse(null);
        this.decisionDigest = other.optionalDecisionDigest().map(StringFilter::copy).orElse(null);
        this.credentialId = other.optionalCredentialId().map(StringFilter::copy).orElse(null);
        this.signCount = other.optionalSignCount().map(LongFilter::copy).orElse(null);
        this.signedAt = other.optionalSignedAt().map(InstantFilter::copy).orElse(null);
        this.dueAt = other.optionalDueAt().map(InstantFilter::copy).orElse(null);
        this.invalidatedAt = other.optionalInvalidatedAt().map(InstantFilter::copy).orElse(null);
        this.fileId = other.optionalFileId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public QuitusCriteria copy() {
        return new QuitusCriteria(this);
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

    public QuitusTypeFilter getType() {
        return type;
    }

    public Optional<QuitusTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public QuitusTypeFilter type() {
        if (type == null) {
            setType(new QuitusTypeFilter());
        }
        return type;
    }

    public void setType(QuitusTypeFilter type) {
        this.type = type;
    }

    public QuitusStatusFilter getStatus() {
        return status;
    }

    public Optional<QuitusStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public QuitusStatusFilter status() {
        if (status == null) {
            setStatus(new QuitusStatusFilter());
        }
        return status;
    }

    public void setStatus(QuitusStatusFilter status) {
        this.status = status;
    }

    public QuitusDecisionFilter getDecision() {
        return decision;
    }

    public Optional<QuitusDecisionFilter> optionalDecision() {
        return Optional.ofNullable(decision);
    }

    public QuitusDecisionFilter decision() {
        if (decision == null) {
            setDecision(new QuitusDecisionFilter());
        }
        return decision;
    }

    public void setDecision(QuitusDecisionFilter decision) {
        this.decision = decision;
    }

    public UUIDFilter getSignerId() {
        return signerId;
    }

    public Optional<UUIDFilter> optionalSignerId() {
        return Optional.ofNullable(signerId);
    }

    public UUIDFilter signerId() {
        if (signerId == null) {
            setSignerId(new UUIDFilter());
        }
        return signerId;
    }

    public void setSignerId(UUIDFilter signerId) {
        this.signerId = signerId;
    }

    public StringFilter getReason() {
        return reason;
    }

    public Optional<StringFilter> optionalReason() {
        return Optional.ofNullable(reason);
    }

    public StringFilter reason() {
        if (reason == null) {
            setReason(new StringFilter());
        }
        return reason;
    }

    public void setReason(StringFilter reason) {
        this.reason = reason;
    }

    public StringFilter getBasisDigest() {
        return basisDigest;
    }

    public Optional<StringFilter> optionalBasisDigest() {
        return Optional.ofNullable(basisDigest);
    }

    public StringFilter basisDigest() {
        if (basisDigest == null) {
            setBasisDigest(new StringFilter());
        }
        return basisDigest;
    }

    public void setBasisDigest(StringFilter basisDigest) {
        this.basisDigest = basisDigest;
    }

    public StringFilter getDecisionDigest() {
        return decisionDigest;
    }

    public Optional<StringFilter> optionalDecisionDigest() {
        return Optional.ofNullable(decisionDigest);
    }

    public StringFilter decisionDigest() {
        if (decisionDigest == null) {
            setDecisionDigest(new StringFilter());
        }
        return decisionDigest;
    }

    public void setDecisionDigest(StringFilter decisionDigest) {
        this.decisionDigest = decisionDigest;
    }

    public StringFilter getCredentialId() {
        return credentialId;
    }

    public Optional<StringFilter> optionalCredentialId() {
        return Optional.ofNullable(credentialId);
    }

    public StringFilter credentialId() {
        if (credentialId == null) {
            setCredentialId(new StringFilter());
        }
        return credentialId;
    }

    public void setCredentialId(StringFilter credentialId) {
        this.credentialId = credentialId;
    }

    public LongFilter getSignCount() {
        return signCount;
    }

    public Optional<LongFilter> optionalSignCount() {
        return Optional.ofNullable(signCount);
    }

    public LongFilter signCount() {
        if (signCount == null) {
            setSignCount(new LongFilter());
        }
        return signCount;
    }

    public void setSignCount(LongFilter signCount) {
        this.signCount = signCount;
    }

    public InstantFilter getSignedAt() {
        return signedAt;
    }

    public Optional<InstantFilter> optionalSignedAt() {
        return Optional.ofNullable(signedAt);
    }

    public InstantFilter signedAt() {
        if (signedAt == null) {
            setSignedAt(new InstantFilter());
        }
        return signedAt;
    }

    public void setSignedAt(InstantFilter signedAt) {
        this.signedAt = signedAt;
    }

    public InstantFilter getDueAt() {
        return dueAt;
    }

    public Optional<InstantFilter> optionalDueAt() {
        return Optional.ofNullable(dueAt);
    }

    public InstantFilter dueAt() {
        if (dueAt == null) {
            setDueAt(new InstantFilter());
        }
        return dueAt;
    }

    public void setDueAt(InstantFilter dueAt) {
        this.dueAt = dueAt;
    }

    public InstantFilter getInvalidatedAt() {
        return invalidatedAt;
    }

    public Optional<InstantFilter> optionalInvalidatedAt() {
        return Optional.ofNullable(invalidatedAt);
    }

    public InstantFilter invalidatedAt() {
        if (invalidatedAt == null) {
            setInvalidatedAt(new InstantFilter());
        }
        return invalidatedAt;
    }

    public void setInvalidatedAt(InstantFilter invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
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
        final QuitusCriteria that = (QuitusCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(type, that.type) &&
            Objects.equals(status, that.status) &&
            Objects.equals(decision, that.decision) &&
            Objects.equals(signerId, that.signerId) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(basisDigest, that.basisDigest) &&
            Objects.equals(decisionDigest, that.decisionDigest) &&
            Objects.equals(credentialId, that.credentialId) &&
            Objects.equals(signCount, that.signCount) &&
            Objects.equals(signedAt, that.signedAt) &&
            Objects.equals(dueAt, that.dueAt) &&
            Objects.equals(invalidatedAt, that.invalidatedAt) &&
            Objects.equals(fileId, that.fileId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            type,
            status,
            decision,
            signerId,
            reason,
            basisDigest,
            decisionDigest,
            credentialId,
            signCount,
            signedAt,
            dueAt,
            invalidatedAt,
            fileId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "QuitusCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalDecision().map(f -> "decision=" + f + ", ").orElse("") +
            optionalSignerId().map(f -> "signerId=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalBasisDigest().map(f -> "basisDigest=" + f + ", ").orElse("") +
            optionalDecisionDigest().map(f -> "decisionDigest=" + f + ", ").orElse("") +
            optionalCredentialId().map(f -> "credentialId=" + f + ", ").orElse("") +
            optionalSignCount().map(f -> "signCount=" + f + ", ").orElse("") +
            optionalSignedAt().map(f -> "signedAt=" + f + ", ").orElse("") +
            optionalDueAt().map(f -> "dueAt=" + f + ", ").orElse("") +
            optionalInvalidatedAt().map(f -> "invalidatedAt=" + f + ", ").orElse("") +
            optionalFileId().map(f -> "fileId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
