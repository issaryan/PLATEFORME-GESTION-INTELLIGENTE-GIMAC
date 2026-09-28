package cm.gimac.des.rapprochement.service.criteria;

import cm.gimac.des.rapprochement.domain.enumeration.ReconciliationState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.rapprochement.domain.Reconciliation} entity. This class is used
 * in {@link cm.gimac.des.rapprochement.web.rest.ReconciliationResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /reconciliations?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReconciliationCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ReconciliationState
     */
    public static class ReconciliationStateFilter extends Filter<ReconciliationState> {

        public ReconciliationStateFilter() {}

        public ReconciliationStateFilter(ReconciliationStateFilter filter) {
            super(filter);
        }

        @Override
        public ReconciliationStateFilter copy() {
            return new ReconciliationStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter reference;

    private LocalDateFilter businessDate;

    private UUIDFilter institutionId;

    private ReconciliationStateFilter state;

    private DurationFilter timestampTolerance;

    private IntegerFilter matchedCount;

    private IntegerFilter discrepancyCount;

    private InstantFilter startedAt;

    private InstantFilter completedAt;

    private UUIDFilter closedBy;

    private InstantFilter closedAt;

    private UUIDFilter switchStatementId;

    private UUIDFilter participantStatementId;

    private UUIDFilter settlementStatementId;

    private Boolean distinct;

    public ReconciliationCriteria() {}

    public ReconciliationCriteria(ReconciliationCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.businessDate = other.optionalBusinessDate().map(LocalDateFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.state = other.optionalState().map(ReconciliationStateFilter::copy).orElse(null);
        this.timestampTolerance = other.optionalTimestampTolerance().map(DurationFilter::copy).orElse(null);
        this.matchedCount = other.optionalMatchedCount().map(IntegerFilter::copy).orElse(null);
        this.discrepancyCount = other.optionalDiscrepancyCount().map(IntegerFilter::copy).orElse(null);
        this.startedAt = other.optionalStartedAt().map(InstantFilter::copy).orElse(null);
        this.completedAt = other.optionalCompletedAt().map(InstantFilter::copy).orElse(null);
        this.closedBy = other.optionalClosedBy().map(UUIDFilter::copy).orElse(null);
        this.closedAt = other.optionalClosedAt().map(InstantFilter::copy).orElse(null);
        this.switchStatementId = other.optionalSwitchStatementId().map(UUIDFilter::copy).orElse(null);
        this.participantStatementId = other.optionalParticipantStatementId().map(UUIDFilter::copy).orElse(null);
        this.settlementStatementId = other.optionalSettlementStatementId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ReconciliationCriteria copy() {
        return new ReconciliationCriteria(this);
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

    public LocalDateFilter getBusinessDate() {
        return businessDate;
    }

    public Optional<LocalDateFilter> optionalBusinessDate() {
        return Optional.ofNullable(businessDate);
    }

    public LocalDateFilter businessDate() {
        if (businessDate == null) {
            setBusinessDate(new LocalDateFilter());
        }
        return businessDate;
    }

    public void setBusinessDate(LocalDateFilter businessDate) {
        this.businessDate = businessDate;
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

    public ReconciliationStateFilter getState() {
        return state;
    }

    public Optional<ReconciliationStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public ReconciliationStateFilter state() {
        if (state == null) {
            setState(new ReconciliationStateFilter());
        }
        return state;
    }

    public void setState(ReconciliationStateFilter state) {
        this.state = state;
    }

    public DurationFilter getTimestampTolerance() {
        return timestampTolerance;
    }

    public Optional<DurationFilter> optionalTimestampTolerance() {
        return Optional.ofNullable(timestampTolerance);
    }

    public DurationFilter timestampTolerance() {
        if (timestampTolerance == null) {
            setTimestampTolerance(new DurationFilter());
        }
        return timestampTolerance;
    }

    public void setTimestampTolerance(DurationFilter timestampTolerance) {
        this.timestampTolerance = timestampTolerance;
    }

    public IntegerFilter getMatchedCount() {
        return matchedCount;
    }

    public Optional<IntegerFilter> optionalMatchedCount() {
        return Optional.ofNullable(matchedCount);
    }

    public IntegerFilter matchedCount() {
        if (matchedCount == null) {
            setMatchedCount(new IntegerFilter());
        }
        return matchedCount;
    }

    public void setMatchedCount(IntegerFilter matchedCount) {
        this.matchedCount = matchedCount;
    }

    public IntegerFilter getDiscrepancyCount() {
        return discrepancyCount;
    }

    public Optional<IntegerFilter> optionalDiscrepancyCount() {
        return Optional.ofNullable(discrepancyCount);
    }

    public IntegerFilter discrepancyCount() {
        if (discrepancyCount == null) {
            setDiscrepancyCount(new IntegerFilter());
        }
        return discrepancyCount;
    }

    public void setDiscrepancyCount(IntegerFilter discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public InstantFilter getStartedAt() {
        return startedAt;
    }

    public Optional<InstantFilter> optionalStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public InstantFilter startedAt() {
        if (startedAt == null) {
            setStartedAt(new InstantFilter());
        }
        return startedAt;
    }

    public void setStartedAt(InstantFilter startedAt) {
        this.startedAt = startedAt;
    }

    public InstantFilter getCompletedAt() {
        return completedAt;
    }

    public Optional<InstantFilter> optionalCompletedAt() {
        return Optional.ofNullable(completedAt);
    }

    public InstantFilter completedAt() {
        if (completedAt == null) {
            setCompletedAt(new InstantFilter());
        }
        return completedAt;
    }

    public void setCompletedAt(InstantFilter completedAt) {
        this.completedAt = completedAt;
    }

    public UUIDFilter getClosedBy() {
        return closedBy;
    }

    public Optional<UUIDFilter> optionalClosedBy() {
        return Optional.ofNullable(closedBy);
    }

    public UUIDFilter closedBy() {
        if (closedBy == null) {
            setClosedBy(new UUIDFilter());
        }
        return closedBy;
    }

    public void setClosedBy(UUIDFilter closedBy) {
        this.closedBy = closedBy;
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

    public UUIDFilter getSwitchStatementId() {
        return switchStatementId;
    }

    public Optional<UUIDFilter> optionalSwitchStatementId() {
        return Optional.ofNullable(switchStatementId);
    }

    public UUIDFilter switchStatementId() {
        if (switchStatementId == null) {
            setSwitchStatementId(new UUIDFilter());
        }
        return switchStatementId;
    }

    public void setSwitchStatementId(UUIDFilter switchStatementId) {
        this.switchStatementId = switchStatementId;
    }

    public UUIDFilter getParticipantStatementId() {
        return participantStatementId;
    }

    public Optional<UUIDFilter> optionalParticipantStatementId() {
        return Optional.ofNullable(participantStatementId);
    }

    public UUIDFilter participantStatementId() {
        if (participantStatementId == null) {
            setParticipantStatementId(new UUIDFilter());
        }
        return participantStatementId;
    }

    public void setParticipantStatementId(UUIDFilter participantStatementId) {
        this.participantStatementId = participantStatementId;
    }

    public UUIDFilter getSettlementStatementId() {
        return settlementStatementId;
    }

    public Optional<UUIDFilter> optionalSettlementStatementId() {
        return Optional.ofNullable(settlementStatementId);
    }

    public UUIDFilter settlementStatementId() {
        if (settlementStatementId == null) {
            setSettlementStatementId(new UUIDFilter());
        }
        return settlementStatementId;
    }

    public void setSettlementStatementId(UUIDFilter settlementStatementId) {
        this.settlementStatementId = settlementStatementId;
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
        final ReconciliationCriteria that = (ReconciliationCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(businessDate, that.businessDate) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(state, that.state) &&
            Objects.equals(timestampTolerance, that.timestampTolerance) &&
            Objects.equals(matchedCount, that.matchedCount) &&
            Objects.equals(discrepancyCount, that.discrepancyCount) &&
            Objects.equals(startedAt, that.startedAt) &&
            Objects.equals(completedAt, that.completedAt) &&
            Objects.equals(closedBy, that.closedBy) &&
            Objects.equals(closedAt, that.closedAt) &&
            Objects.equals(switchStatementId, that.switchStatementId) &&
            Objects.equals(participantStatementId, that.participantStatementId) &&
            Objects.equals(settlementStatementId, that.settlementStatementId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            reference,
            businessDate,
            institutionId,
            state,
            timestampTolerance,
            matchedCount,
            discrepancyCount,
            startedAt,
            completedAt,
            closedBy,
            closedAt,
            switchStatementId,
            participantStatementId,
            settlementStatementId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReconciliationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalBusinessDate().map(f -> "businessDate=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalTimestampTolerance().map(f -> "timestampTolerance=" + f + ", ").orElse("") +
            optionalMatchedCount().map(f -> "matchedCount=" + f + ", ").orElse("") +
            optionalDiscrepancyCount().map(f -> "discrepancyCount=" + f + ", ").orElse("") +
            optionalStartedAt().map(f -> "startedAt=" + f + ", ").orElse("") +
            optionalCompletedAt().map(f -> "completedAt=" + f + ", ").orElse("") +
            optionalClosedBy().map(f -> "closedBy=" + f + ", ").orElse("") +
            optionalClosedAt().map(f -> "closedAt=" + f + ", ").orElse("") +
            optionalSwitchStatementId().map(f -> "switchStatementId=" + f + ", ").orElse("") +
            optionalParticipantStatementId().map(f -> "participantStatementId=" + f + ", ").orElse("") +
            optionalSettlementStatementId().map(f -> "settlementStatementId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
