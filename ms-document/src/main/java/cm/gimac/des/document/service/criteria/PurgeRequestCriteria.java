package cm.gimac.des.document.service.criteria;

import cm.gimac.des.document.domain.enumeration.PurgeState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.document.domain.PurgeRequest} entity. This class is used
 * in {@link cm.gimac.des.document.web.rest.PurgeRequestResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /purge-requests?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PurgeRequestCriteria implements Serializable, Criteria {

    /**
     * Class for filtering PurgeState
     */
    public static class PurgeStateFilter extends Filter<PurgeState> {

        public PurgeStateFilter() {}

        public PurgeStateFilter(PurgeStateFilter filter) {
            super(filter);
        }

        @Override
        public PurgeStateFilter copy() {
            return new PurgeStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter requesterId;

    private StringFilter reason;

    private InstantFilter requestedAt;

    private UUIDFilter approverId;

    private InstantFilter approvedAt;

    private PurgeStateFilter state;

    private StringFilter rejectionReason;

    private InstantFilter executedAt;

    private UUIDFilter documentId;

    private Boolean distinct;

    public PurgeRequestCriteria() {}

    public PurgeRequestCriteria(PurgeRequestCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.requesterId = other.optionalRequesterId().map(UUIDFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.requestedAt = other.optionalRequestedAt().map(InstantFilter::copy).orElse(null);
        this.approverId = other.optionalApproverId().map(UUIDFilter::copy).orElse(null);
        this.approvedAt = other.optionalApprovedAt().map(InstantFilter::copy).orElse(null);
        this.state = other.optionalState().map(PurgeStateFilter::copy).orElse(null);
        this.rejectionReason = other.optionalRejectionReason().map(StringFilter::copy).orElse(null);
        this.executedAt = other.optionalExecutedAt().map(InstantFilter::copy).orElse(null);
        this.documentId = other.optionalDocumentId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public PurgeRequestCriteria copy() {
        return new PurgeRequestCriteria(this);
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

    public InstantFilter getApprovedAt() {
        return approvedAt;
    }

    public Optional<InstantFilter> optionalApprovedAt() {
        return Optional.ofNullable(approvedAt);
    }

    public InstantFilter approvedAt() {
        if (approvedAt == null) {
            setApprovedAt(new InstantFilter());
        }
        return approvedAt;
    }

    public void setApprovedAt(InstantFilter approvedAt) {
        this.approvedAt = approvedAt;
    }

    public PurgeStateFilter getState() {
        return state;
    }

    public Optional<PurgeStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public PurgeStateFilter state() {
        if (state == null) {
            setState(new PurgeStateFilter());
        }
        return state;
    }

    public void setState(PurgeStateFilter state) {
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

    public InstantFilter getExecutedAt() {
        return executedAt;
    }

    public Optional<InstantFilter> optionalExecutedAt() {
        return Optional.ofNullable(executedAt);
    }

    public InstantFilter executedAt() {
        if (executedAt == null) {
            setExecutedAt(new InstantFilter());
        }
        return executedAt;
    }

    public void setExecutedAt(InstantFilter executedAt) {
        this.executedAt = executedAt;
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
        final PurgeRequestCriteria that = (PurgeRequestCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(requesterId, that.requesterId) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(requestedAt, that.requestedAt) &&
            Objects.equals(approverId, that.approverId) &&
            Objects.equals(approvedAt, that.approvedAt) &&
            Objects.equals(state, that.state) &&
            Objects.equals(rejectionReason, that.rejectionReason) &&
            Objects.equals(executedAt, that.executedAt) &&
            Objects.equals(documentId, that.documentId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            requesterId,
            reason,
            requestedAt,
            approverId,
            approvedAt,
            state,
            rejectionReason,
            executedAt,
            documentId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PurgeRequestCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalRequesterId().map(f -> "requesterId=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalRequestedAt().map(f -> "requestedAt=" + f + ", ").orElse("") +
            optionalApproverId().map(f -> "approverId=" + f + ", ").orElse("") +
            optionalApprovedAt().map(f -> "approvedAt=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalRejectionReason().map(f -> "rejectionReason=" + f + ", ").orElse("") +
            optionalExecutedAt().map(f -> "executedAt=" + f + ", ").orElse("") +
            optionalDocumentId().map(f -> "documentId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
