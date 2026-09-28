package cm.gimac.des.participant.service.criteria;

import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.participant.domain.StatusTransition} entity. This class is used
 * in {@link cm.gimac.des.participant.web.rest.StatusTransitionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /status-transitions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StatusTransitionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering InstitutionStatus
     */
    public static class InstitutionStatusFilter extends Filter<InstitutionStatus> {

        public InstitutionStatusFilter() {}

        public InstitutionStatusFilter(InstitutionStatusFilter filter) {
            super(filter);
        }

        @Override
        public InstitutionStatusFilter copy() {
            return new InstitutionStatusFilter(this);
        }
    }

    /**
     * Class for filtering TransitionState
     */
    public static class TransitionStateFilter extends Filter<TransitionState> {

        public TransitionStateFilter() {}

        public TransitionStateFilter(TransitionStateFilter filter) {
            super(filter);
        }

        @Override
        public TransitionStateFilter copy() {
            return new TransitionStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private InstitutionStatusFilter fromStatus;

    private InstitutionStatusFilter toStatus;

    private StringFilter reason;

    private UUIDFilter requesterId;

    private InstantFilter requestedAt;

    private BooleanFilter automaticTransition;

    private IntegerFilter requiredApprovals;

    private UUIDFilter approverId;

    private UUIDFilter secondApproverId;

    private InstantFilter secondApprovedAt;

    private InstantFilter decidedAt;

    private TransitionStateFilter state;

    private StringFilter rejectionReason;

    private UUIDFilter evidenceDocumentId;

    private UUIDFilter institutionId;

    private Boolean distinct;

    public StatusTransitionCriteria() {}

    public StatusTransitionCriteria(StatusTransitionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.fromStatus = other.optionalFromStatus().map(InstitutionStatusFilter::copy).orElse(null);
        this.toStatus = other.optionalToStatus().map(InstitutionStatusFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.requesterId = other.optionalRequesterId().map(UUIDFilter::copy).orElse(null);
        this.requestedAt = other.optionalRequestedAt().map(InstantFilter::copy).orElse(null);
        this.automaticTransition = other.optionalAutomaticTransition().map(BooleanFilter::copy).orElse(null);
        this.requiredApprovals = other.optionalRequiredApprovals().map(IntegerFilter::copy).orElse(null);
        this.approverId = other.optionalApproverId().map(UUIDFilter::copy).orElse(null);
        this.secondApproverId = other.optionalSecondApproverId().map(UUIDFilter::copy).orElse(null);
        this.secondApprovedAt = other.optionalSecondApprovedAt().map(InstantFilter::copy).orElse(null);
        this.decidedAt = other.optionalDecidedAt().map(InstantFilter::copy).orElse(null);
        this.state = other.optionalState().map(TransitionStateFilter::copy).orElse(null);
        this.rejectionReason = other.optionalRejectionReason().map(StringFilter::copy).orElse(null);
        this.evidenceDocumentId = other.optionalEvidenceDocumentId().map(UUIDFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public StatusTransitionCriteria copy() {
        return new StatusTransitionCriteria(this);
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

    public InstitutionStatusFilter getFromStatus() {
        return fromStatus;
    }

    public Optional<InstitutionStatusFilter> optionalFromStatus() {
        return Optional.ofNullable(fromStatus);
    }

    public InstitutionStatusFilter fromStatus() {
        if (fromStatus == null) {
            setFromStatus(new InstitutionStatusFilter());
        }
        return fromStatus;
    }

    public void setFromStatus(InstitutionStatusFilter fromStatus) {
        this.fromStatus = fromStatus;
    }

    public InstitutionStatusFilter getToStatus() {
        return toStatus;
    }

    public Optional<InstitutionStatusFilter> optionalToStatus() {
        return Optional.ofNullable(toStatus);
    }

    public InstitutionStatusFilter toStatus() {
        if (toStatus == null) {
            setToStatus(new InstitutionStatusFilter());
        }
        return toStatus;
    }

    public void setToStatus(InstitutionStatusFilter toStatus) {
        this.toStatus = toStatus;
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

    public UUIDFilter getRequesterId() {
        return requesterId;
    }

    public Optional<UUIDFilter> optionalRequesterId() {
        return Optional.ofNullable(requesterId);
    }

    public UUIDFilter requesterId() {
        if (requesterId == null) {
            setRequesterId(new UUIDFilter());
        }
        return requesterId;
    }

    public void setRequesterId(UUIDFilter requesterId) {
        this.requesterId = requesterId;
    }

    public InstantFilter getRequestedAt() {
        return requestedAt;
    }

    public Optional<InstantFilter> optionalRequestedAt() {
        return Optional.ofNullable(requestedAt);
    }

    public InstantFilter requestedAt() {
        if (requestedAt == null) {
            setRequestedAt(new InstantFilter());
        }
        return requestedAt;
    }

    public void setRequestedAt(InstantFilter requestedAt) {
        this.requestedAt = requestedAt;
    }

    public BooleanFilter getAutomaticTransition() {
        return automaticTransition;
    }

    public Optional<BooleanFilter> optionalAutomaticTransition() {
        return Optional.ofNullable(automaticTransition);
    }

    public BooleanFilter automaticTransition() {
        if (automaticTransition == null) {
            setAutomaticTransition(new BooleanFilter());
        }
        return automaticTransition;
    }

    public void setAutomaticTransition(BooleanFilter automaticTransition) {
        this.automaticTransition = automaticTransition;
    }

    public IntegerFilter getRequiredApprovals() {
        return requiredApprovals;
    }

    public Optional<IntegerFilter> optionalRequiredApprovals() {
        return Optional.ofNullable(requiredApprovals);
    }

    public IntegerFilter requiredApprovals() {
        if (requiredApprovals == null) {
            setRequiredApprovals(new IntegerFilter());
        }
        return requiredApprovals;
    }

    public void setRequiredApprovals(IntegerFilter requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    public UUIDFilter getApproverId() {
        return approverId;
    }

    public Optional<UUIDFilter> optionalApproverId() {
        return Optional.ofNullable(approverId);
    }

    public UUIDFilter approverId() {
        if (approverId == null) {
            setApproverId(new UUIDFilter());
        }
        return approverId;
    }

    public void setApproverId(UUIDFilter approverId) {
        this.approverId = approverId;
    }

    public UUIDFilter getSecondApproverId() {
        return secondApproverId;
    }

    public Optional<UUIDFilter> optionalSecondApproverId() {
        return Optional.ofNullable(secondApproverId);
    }

    public UUIDFilter secondApproverId() {
        if (secondApproverId == null) {
            setSecondApproverId(new UUIDFilter());
        }
        return secondApproverId;
    }

    public void setSecondApproverId(UUIDFilter secondApproverId) {
        this.secondApproverId = secondApproverId;
    }

    public InstantFilter getSecondApprovedAt() {
        return secondApprovedAt;
    }

    public Optional<InstantFilter> optionalSecondApprovedAt() {
        return Optional.ofNullable(secondApprovedAt);
    }

    public InstantFilter secondApprovedAt() {
        if (secondApprovedAt == null) {
            setSecondApprovedAt(new InstantFilter());
        }
        return secondApprovedAt;
    }

    public void setSecondApprovedAt(InstantFilter secondApprovedAt) {
        this.secondApprovedAt = secondApprovedAt;
    }

    public InstantFilter getDecidedAt() {
        return decidedAt;
    }

    public Optional<InstantFilter> optionalDecidedAt() {
        return Optional.ofNullable(decidedAt);
    }

    public InstantFilter decidedAt() {
        if (decidedAt == null) {
            setDecidedAt(new InstantFilter());
        }
        return decidedAt;
    }

    public void setDecidedAt(InstantFilter decidedAt) {
        this.decidedAt = decidedAt;
    }

    public TransitionStateFilter getState() {
        return state;
    }

    public Optional<TransitionStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public TransitionStateFilter state() {
        if (state == null) {
            setState(new TransitionStateFilter());
        }
        return state;
    }

    public void setState(TransitionStateFilter state) {
        this.state = state;
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
        final StatusTransitionCriteria that = (StatusTransitionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(fromStatus, that.fromStatus) &&
            Objects.equals(toStatus, that.toStatus) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(requesterId, that.requesterId) &&
            Objects.equals(requestedAt, that.requestedAt) &&
            Objects.equals(automaticTransition, that.automaticTransition) &&
            Objects.equals(requiredApprovals, that.requiredApprovals) &&
            Objects.equals(approverId, that.approverId) &&
            Objects.equals(secondApproverId, that.secondApproverId) &&
            Objects.equals(secondApprovedAt, that.secondApprovedAt) &&
            Objects.equals(decidedAt, that.decidedAt) &&
            Objects.equals(state, that.state) &&
            Objects.equals(rejectionReason, that.rejectionReason) &&
            Objects.equals(evidenceDocumentId, that.evidenceDocumentId) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            fromStatus,
            toStatus,
            reason,
            requesterId,
            requestedAt,
            automaticTransition,
            requiredApprovals,
            approverId,
            secondApproverId,
            secondApprovedAt,
            decidedAt,
            state,
            rejectionReason,
            evidenceDocumentId,
            institutionId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StatusTransitionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalFromStatus().map(f -> "fromStatus=" + f + ", ").orElse("") +
            optionalToStatus().map(f -> "toStatus=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalRequesterId().map(f -> "requesterId=" + f + ", ").orElse("") +
            optionalRequestedAt().map(f -> "requestedAt=" + f + ", ").orElse("") +
            optionalAutomaticTransition().map(f -> "automaticTransition=" + f + ", ").orElse("") +
            optionalRequiredApprovals().map(f -> "requiredApprovals=" + f + ", ").orElse("") +
            optionalApproverId().map(f -> "approverId=" + f + ", ").orElse("") +
            optionalSecondApproverId().map(f -> "secondApproverId=" + f + ", ").orElse("") +
            optionalSecondApprovedAt().map(f -> "secondApprovedAt=" + f + ", ").orElse("") +
            optionalDecidedAt().map(f -> "decidedAt=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalRejectionReason().map(f -> "rejectionReason=" + f + ", ").orElse("") +
            optionalEvidenceDocumentId().map(f -> "evidenceDocumentId=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
