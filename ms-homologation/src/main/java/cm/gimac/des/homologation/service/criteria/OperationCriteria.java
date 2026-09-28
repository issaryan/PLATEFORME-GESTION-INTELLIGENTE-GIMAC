package cm.gimac.des.homologation.service.criteria;

import cm.gimac.des.homologation.domain.enumeration.OperationState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.homologation.domain.Operation} entity. This class is used
 * in {@link cm.gimac.des.homologation.web.rest.OperationResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /operations?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OperationCriteria implements Serializable, Criteria {

    /**
     * Class for filtering OperationState
     */
    public static class OperationStateFilter extends Filter<OperationState> {

        public OperationStateFilter() {}

        public OperationStateFilter(OperationStateFilter filter) {
            super(filter);
        }

        @Override
        public OperationStateFilter copy() {
            return new OperationStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter operationCode;

    private OperationStateFilter state;

    private UUIDFilter ownerId;

    private UUIDFilter evidenceDocumentId;

    private StringFilter evidenceDigest;

    private InstantFilter evidenceSealedAt;

    private DurationFilter evidenceRetention;

    private BooleanFilter evidenceSealConfirmed;

    private StringFilter evidenceTraceRef;

    private StringFilter timestampImprint;

    private InstantFilter timestampGenTime;

    private UUIDFilter validatorId;

    private InstantFilter validatedAt;

    private InstantFilter closedAt;

    private StringFilter stateReason;

    private UUIDFilter liaisonId;

    private UUIDFilter referenceId;

    private Boolean distinct;

    public OperationCriteria() {}

    public OperationCriteria(OperationCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.operationCode = other.optionalOperationCode().map(StringFilter::copy).orElse(null);
        this.state = other.optionalState().map(OperationStateFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(UUIDFilter::copy).orElse(null);
        this.evidenceDocumentId = other.optionalEvidenceDocumentId().map(UUIDFilter::copy).orElse(null);
        this.evidenceDigest = other.optionalEvidenceDigest().map(StringFilter::copy).orElse(null);
        this.evidenceSealedAt = other.optionalEvidenceSealedAt().map(InstantFilter::copy).orElse(null);
        this.evidenceRetention = other.optionalEvidenceRetention().map(DurationFilter::copy).orElse(null);
        this.evidenceSealConfirmed = other.optionalEvidenceSealConfirmed().map(BooleanFilter::copy).orElse(null);
        this.evidenceTraceRef = other.optionalEvidenceTraceRef().map(StringFilter::copy).orElse(null);
        this.timestampImprint = other.optionalTimestampImprint().map(StringFilter::copy).orElse(null);
        this.timestampGenTime = other.optionalTimestampGenTime().map(InstantFilter::copy).orElse(null);
        this.validatorId = other.optionalValidatorId().map(UUIDFilter::copy).orElse(null);
        this.validatedAt = other.optionalValidatedAt().map(InstantFilter::copy).orElse(null);
        this.closedAt = other.optionalClosedAt().map(InstantFilter::copy).orElse(null);
        this.stateReason = other.optionalStateReason().map(StringFilter::copy).orElse(null);
        this.liaisonId = other.optionalLiaisonId().map(UUIDFilter::copy).orElse(null);
        this.referenceId = other.optionalReferenceId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public OperationCriteria copy() {
        return new OperationCriteria(this);
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

    public OperationStateFilter getState() {
        return state;
    }

    public Optional<OperationStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public OperationStateFilter state() {
        if (state == null) {
            setState(new OperationStateFilter());
        }
        return state;
    }

    public void setState(OperationStateFilter state) {
        this.state = state;
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

    public InstantFilter getEvidenceSealedAt() {
        return evidenceSealedAt;
    }

    public Optional<InstantFilter> optionalEvidenceSealedAt() {
        return Optional.ofNullable(evidenceSealedAt);
    }

    public InstantFilter evidenceSealedAt() {
        if (evidenceSealedAt == null) {
            setEvidenceSealedAt(new InstantFilter());
        }
        return evidenceSealedAt;
    }

    public void setEvidenceSealedAt(InstantFilter evidenceSealedAt) {
        this.evidenceSealedAt = evidenceSealedAt;
    }

    public DurationFilter getEvidenceRetention() {
        return evidenceRetention;
    }

    public Optional<DurationFilter> optionalEvidenceRetention() {
        return Optional.ofNullable(evidenceRetention);
    }

    public DurationFilter evidenceRetention() {
        if (evidenceRetention == null) {
            setEvidenceRetention(new DurationFilter());
        }
        return evidenceRetention;
    }

    public void setEvidenceRetention(DurationFilter evidenceRetention) {
        this.evidenceRetention = evidenceRetention;
    }

    public BooleanFilter getEvidenceSealConfirmed() {
        return evidenceSealConfirmed;
    }

    public Optional<BooleanFilter> optionalEvidenceSealConfirmed() {
        return Optional.ofNullable(evidenceSealConfirmed);
    }

    public BooleanFilter evidenceSealConfirmed() {
        if (evidenceSealConfirmed == null) {
            setEvidenceSealConfirmed(new BooleanFilter());
        }
        return evidenceSealConfirmed;
    }

    public void setEvidenceSealConfirmed(BooleanFilter evidenceSealConfirmed) {
        this.evidenceSealConfirmed = evidenceSealConfirmed;
    }

    public StringFilter getEvidenceTraceRef() {
        return evidenceTraceRef;
    }

    public Optional<StringFilter> optionalEvidenceTraceRef() {
        return Optional.ofNullable(evidenceTraceRef);
    }

    public StringFilter evidenceTraceRef() {
        if (evidenceTraceRef == null) {
            setEvidenceTraceRef(new StringFilter());
        }
        return evidenceTraceRef;
    }

    public void setEvidenceTraceRef(StringFilter evidenceTraceRef) {
        this.evidenceTraceRef = evidenceTraceRef;
    }

    public StringFilter getTimestampImprint() {
        return timestampImprint;
    }

    public Optional<StringFilter> optionalTimestampImprint() {
        return Optional.ofNullable(timestampImprint);
    }

    public StringFilter timestampImprint() {
        if (timestampImprint == null) {
            setTimestampImprint(new StringFilter());
        }
        return timestampImprint;
    }

    public void setTimestampImprint(StringFilter timestampImprint) {
        this.timestampImprint = timestampImprint;
    }

    public InstantFilter getTimestampGenTime() {
        return timestampGenTime;
    }

    public Optional<InstantFilter> optionalTimestampGenTime() {
        return Optional.ofNullable(timestampGenTime);
    }

    public InstantFilter timestampGenTime() {
        if (timestampGenTime == null) {
            setTimestampGenTime(new InstantFilter());
        }
        return timestampGenTime;
    }

    public void setTimestampGenTime(InstantFilter timestampGenTime) {
        this.timestampGenTime = timestampGenTime;
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

    public InstantFilter getValidatedAt() {
        return validatedAt;
    }

    public Optional<InstantFilter> optionalValidatedAt() {
        return Optional.ofNullable(validatedAt);
    }

    public InstantFilter validatedAt() {
        if (validatedAt == null) {
            setValidatedAt(new InstantFilter());
        }
        return validatedAt;
    }

    public void setValidatedAt(InstantFilter validatedAt) {
        this.validatedAt = validatedAt;
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

    public StringFilter getStateReason() {
        return stateReason;
    }

    public Optional<StringFilter> optionalStateReason() {
        return Optional.ofNullable(stateReason);
    }

    public StringFilter stateReason() {
        if (stateReason == null) {
            setStateReason(new StringFilter());
        }
        return stateReason;
    }

    public void setStateReason(StringFilter stateReason) {
        this.stateReason = stateReason;
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

    public UUIDFilter getReferenceId() {
        return referenceId;
    }

    public Optional<UUIDFilter> optionalReferenceId() {
        return Optional.ofNullable(referenceId);
    }

    public UUIDFilter referenceId() {
        if (referenceId == null) {
            setReferenceId(new UUIDFilter());
        }
        return referenceId;
    }

    public void setReferenceId(UUIDFilter referenceId) {
        this.referenceId = referenceId;
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
        final OperationCriteria that = (OperationCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(operationCode, that.operationCode) &&
            Objects.equals(state, that.state) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(evidenceDocumentId, that.evidenceDocumentId) &&
            Objects.equals(evidenceDigest, that.evidenceDigest) &&
            Objects.equals(evidenceSealedAt, that.evidenceSealedAt) &&
            Objects.equals(evidenceRetention, that.evidenceRetention) &&
            Objects.equals(evidenceSealConfirmed, that.evidenceSealConfirmed) &&
            Objects.equals(evidenceTraceRef, that.evidenceTraceRef) &&
            Objects.equals(timestampImprint, that.timestampImprint) &&
            Objects.equals(timestampGenTime, that.timestampGenTime) &&
            Objects.equals(validatorId, that.validatorId) &&
            Objects.equals(validatedAt, that.validatedAt) &&
            Objects.equals(closedAt, that.closedAt) &&
            Objects.equals(stateReason, that.stateReason) &&
            Objects.equals(liaisonId, that.liaisonId) &&
            Objects.equals(referenceId, that.referenceId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            operationCode,
            state,
            ownerId,
            evidenceDocumentId,
            evidenceDigest,
            evidenceSealedAt,
            evidenceRetention,
            evidenceSealConfirmed,
            evidenceTraceRef,
            timestampImprint,
            timestampGenTime,
            validatorId,
            validatedAt,
            closedAt,
            stateReason,
            liaisonId,
            referenceId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OperationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalOperationCode().map(f -> "operationCode=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalEvidenceDocumentId().map(f -> "evidenceDocumentId=" + f + ", ").orElse("") +
            optionalEvidenceDigest().map(f -> "evidenceDigest=" + f + ", ").orElse("") +
            optionalEvidenceSealedAt().map(f -> "evidenceSealedAt=" + f + ", ").orElse("") +
            optionalEvidenceRetention().map(f -> "evidenceRetention=" + f + ", ").orElse("") +
            optionalEvidenceSealConfirmed().map(f -> "evidenceSealConfirmed=" + f + ", ").orElse("") +
            optionalEvidenceTraceRef().map(f -> "evidenceTraceRef=" + f + ", ").orElse("") +
            optionalTimestampImprint().map(f -> "timestampImprint=" + f + ", ").orElse("") +
            optionalTimestampGenTime().map(f -> "timestampGenTime=" + f + ", ").orElse("") +
            optionalValidatorId().map(f -> "validatorId=" + f + ", ").orElse("") +
            optionalValidatedAt().map(f -> "validatedAt=" + f + ", ").orElse("") +
            optionalClosedAt().map(f -> "closedAt=" + f + ", ").orElse("") +
            optionalStateReason().map(f -> "stateReason=" + f + ", ").orElse("") +
            optionalLiaisonId().map(f -> "liaisonId=" + f + ", ").orElse("") +
            optionalReferenceId().map(f -> "referenceId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
