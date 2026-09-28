package cm.gimac.des.rapprochement.service.criteria;

import cm.gimac.des.rapprochement.domain.enumeration.DiscrepancyType;
import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.rapprochement.domain.Suspens} entity. This class is used
 * in {@link cm.gimac.des.rapprochement.web.rest.SuspensResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /suspens?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SuspensCriteria implements Serializable, Criteria {

    /**
     * Class for filtering DiscrepancyType
     */
    public static class DiscrepancyTypeFilter extends Filter<DiscrepancyType> {

        public DiscrepancyTypeFilter() {}

        public DiscrepancyTypeFilter(DiscrepancyTypeFilter filter) {
            super(filter);
        }

        @Override
        public DiscrepancyTypeFilter copy() {
            return new DiscrepancyTypeFilter(this);
        }
    }

    /**
     * Class for filtering SuspensState
     */
    public static class SuspensStateFilter extends Filter<SuspensState> {

        public SuspensStateFilter() {}

        public SuspensStateFilter(SuspensStateFilter filter) {
            super(filter);
        }

        @Override
        public SuspensStateFilter copy() {
            return new SuspensStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter reference;

    private UUIDFilter institutionId;

    private DiscrepancyTypeFilter type;

    private StringFilter pivotTransactionRef;

    private StringFilter pivotAuthorizationRef;

    private InstantFilter pivotOperationAt;

    private BigDecimalFilter pivotAmount;

    private StringFilter pivotParticipantCode;

    private BigDecimalFilter amountSwitch;

    private BigDecimalFilter amountParticipant;

    private SuspensStateFilter state;

    private UUIDFilter assigneeId;

    private LocalDateFilter dueDate;

    private InstantFilter openedAt;

    private UUIDFilter disputeTicketId;

    private InstantFilter closedAt;

    private UUIDFilter reconciliationId;

    private UUIDFilter regularizationAttestationId;

    private Boolean distinct;

    public SuspensCriteria() {}

    public SuspensCriteria(SuspensCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.type = other.optionalType().map(DiscrepancyTypeFilter::copy).orElse(null);
        this.pivotTransactionRef = other.optionalPivotTransactionRef().map(StringFilter::copy).orElse(null);
        this.pivotAuthorizationRef = other.optionalPivotAuthorizationRef().map(StringFilter::copy).orElse(null);
        this.pivotOperationAt = other.optionalPivotOperationAt().map(InstantFilter::copy).orElse(null);
        this.pivotAmount = other.optionalPivotAmount().map(BigDecimalFilter::copy).orElse(null);
        this.pivotParticipantCode = other.optionalPivotParticipantCode().map(StringFilter::copy).orElse(null);
        this.amountSwitch = other.optionalAmountSwitch().map(BigDecimalFilter::copy).orElse(null);
        this.amountParticipant = other.optionalAmountParticipant().map(BigDecimalFilter::copy).orElse(null);
        this.state = other.optionalState().map(SuspensStateFilter::copy).orElse(null);
        this.assigneeId = other.optionalAssigneeId().map(UUIDFilter::copy).orElse(null);
        this.dueDate = other.optionalDueDate().map(LocalDateFilter::copy).orElse(null);
        this.openedAt = other.optionalOpenedAt().map(InstantFilter::copy).orElse(null);
        this.disputeTicketId = other.optionalDisputeTicketId().map(UUIDFilter::copy).orElse(null);
        this.closedAt = other.optionalClosedAt().map(InstantFilter::copy).orElse(null);
        this.reconciliationId = other.optionalReconciliationId().map(UUIDFilter::copy).orElse(null);
        this.regularizationAttestationId = other.optionalRegularizationAttestationId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public SuspensCriteria copy() {
        return new SuspensCriteria(this);
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

    public DiscrepancyTypeFilter getType() {
        return type;
    }

    public Optional<DiscrepancyTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public DiscrepancyTypeFilter type() {
        if (type == null) {
            setType(new DiscrepancyTypeFilter());
        }
        return type;
    }

    public void setType(DiscrepancyTypeFilter type) {
        this.type = type;
    }

    public StringFilter getPivotTransactionRef() {
        return pivotTransactionRef;
    }

    public Optional<StringFilter> optionalPivotTransactionRef() {
        return Optional.ofNullable(pivotTransactionRef);
    }

    public StringFilter pivotTransactionRef() {
        if (pivotTransactionRef == null) {
            setPivotTransactionRef(new StringFilter());
        }
        return pivotTransactionRef;
    }

    public void setPivotTransactionRef(StringFilter pivotTransactionRef) {
        this.pivotTransactionRef = pivotTransactionRef;
    }

    public StringFilter getPivotAuthorizationRef() {
        return pivotAuthorizationRef;
    }

    public Optional<StringFilter> optionalPivotAuthorizationRef() {
        return Optional.ofNullable(pivotAuthorizationRef);
    }

    public StringFilter pivotAuthorizationRef() {
        if (pivotAuthorizationRef == null) {
            setPivotAuthorizationRef(new StringFilter());
        }
        return pivotAuthorizationRef;
    }

    public void setPivotAuthorizationRef(StringFilter pivotAuthorizationRef) {
        this.pivotAuthorizationRef = pivotAuthorizationRef;
    }

    public InstantFilter getPivotOperationAt() {
        return pivotOperationAt;
    }

    public Optional<InstantFilter> optionalPivotOperationAt() {
        return Optional.ofNullable(pivotOperationAt);
    }

    public InstantFilter pivotOperationAt() {
        if (pivotOperationAt == null) {
            setPivotOperationAt(new InstantFilter());
        }
        return pivotOperationAt;
    }

    public void setPivotOperationAt(InstantFilter pivotOperationAt) {
        this.pivotOperationAt = pivotOperationAt;
    }

    public BigDecimalFilter getPivotAmount() {
        return pivotAmount;
    }

    public Optional<BigDecimalFilter> optionalPivotAmount() {
        return Optional.ofNullable(pivotAmount);
    }

    public BigDecimalFilter pivotAmount() {
        if (pivotAmount == null) {
            setPivotAmount(new BigDecimalFilter());
        }
        return pivotAmount;
    }

    public void setPivotAmount(BigDecimalFilter pivotAmount) {
        this.pivotAmount = pivotAmount;
    }

    public StringFilter getPivotParticipantCode() {
        return pivotParticipantCode;
    }

    public Optional<StringFilter> optionalPivotParticipantCode() {
        return Optional.ofNullable(pivotParticipantCode);
    }

    public StringFilter pivotParticipantCode() {
        if (pivotParticipantCode == null) {
            setPivotParticipantCode(new StringFilter());
        }
        return pivotParticipantCode;
    }

    public void setPivotParticipantCode(StringFilter pivotParticipantCode) {
        this.pivotParticipantCode = pivotParticipantCode;
    }

    public BigDecimalFilter getAmountSwitch() {
        return amountSwitch;
    }

    public Optional<BigDecimalFilter> optionalAmountSwitch() {
        return Optional.ofNullable(amountSwitch);
    }

    public BigDecimalFilter amountSwitch() {
        if (amountSwitch == null) {
            setAmountSwitch(new BigDecimalFilter());
        }
        return amountSwitch;
    }

    public void setAmountSwitch(BigDecimalFilter amountSwitch) {
        this.amountSwitch = amountSwitch;
    }

    public BigDecimalFilter getAmountParticipant() {
        return amountParticipant;
    }

    public Optional<BigDecimalFilter> optionalAmountParticipant() {
        return Optional.ofNullable(amountParticipant);
    }

    public BigDecimalFilter amountParticipant() {
        if (amountParticipant == null) {
            setAmountParticipant(new BigDecimalFilter());
        }
        return amountParticipant;
    }

    public void setAmountParticipant(BigDecimalFilter amountParticipant) {
        this.amountParticipant = amountParticipant;
    }

    public SuspensStateFilter getState() {
        return state;
    }

    public Optional<SuspensStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public SuspensStateFilter state() {
        if (state == null) {
            setState(new SuspensStateFilter());
        }
        return state;
    }

    public void setState(SuspensStateFilter state) {
        this.state = state;
    }

    public UUIDFilter getAssigneeId() {
        return assigneeId;
    }

    public Optional<UUIDFilter> optionalAssigneeId() {
        return Optional.ofNullable(assigneeId);
    }

    public UUIDFilter assigneeId() {
        if (assigneeId == null) {
            setAssigneeId(new UUIDFilter());
        }
        return assigneeId;
    }

    public void setAssigneeId(UUIDFilter assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDateFilter getDueDate() {
        return dueDate;
    }

    public Optional<LocalDateFilter> optionalDueDate() {
        return Optional.ofNullable(dueDate);
    }

    public LocalDateFilter dueDate() {
        if (dueDate == null) {
            setDueDate(new LocalDateFilter());
        }
        return dueDate;
    }

    public void setDueDate(LocalDateFilter dueDate) {
        this.dueDate = dueDate;
    }

    public InstantFilter getOpenedAt() {
        return openedAt;
    }

    public Optional<InstantFilter> optionalOpenedAt() {
        return Optional.ofNullable(openedAt);
    }

    public InstantFilter openedAt() {
        if (openedAt == null) {
            setOpenedAt(new InstantFilter());
        }
        return openedAt;
    }

    public void setOpenedAt(InstantFilter openedAt) {
        this.openedAt = openedAt;
    }

    public UUIDFilter getDisputeTicketId() {
        return disputeTicketId;
    }

    public Optional<UUIDFilter> optionalDisputeTicketId() {
        return Optional.ofNullable(disputeTicketId);
    }

    public UUIDFilter disputeTicketId() {
        if (disputeTicketId == null) {
            setDisputeTicketId(new UUIDFilter());
        }
        return disputeTicketId;
    }

    public void setDisputeTicketId(UUIDFilter disputeTicketId) {
        this.disputeTicketId = disputeTicketId;
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

    public UUIDFilter getReconciliationId() {
        return reconciliationId;
    }

    public Optional<UUIDFilter> optionalReconciliationId() {
        return Optional.ofNullable(reconciliationId);
    }

    public UUIDFilter reconciliationId() {
        if (reconciliationId == null) {
            setReconciliationId(new UUIDFilter());
        }
        return reconciliationId;
    }

    public void setReconciliationId(UUIDFilter reconciliationId) {
        this.reconciliationId = reconciliationId;
    }

    public UUIDFilter getRegularizationAttestationId() {
        return regularizationAttestationId;
    }

    public Optional<UUIDFilter> optionalRegularizationAttestationId() {
        return Optional.ofNullable(regularizationAttestationId);
    }

    public UUIDFilter regularizationAttestationId() {
        if (regularizationAttestationId == null) {
            setRegularizationAttestationId(new UUIDFilter());
        }
        return regularizationAttestationId;
    }

    public void setRegularizationAttestationId(UUIDFilter regularizationAttestationId) {
        this.regularizationAttestationId = regularizationAttestationId;
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
        final SuspensCriteria that = (SuspensCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(type, that.type) &&
            Objects.equals(pivotTransactionRef, that.pivotTransactionRef) &&
            Objects.equals(pivotAuthorizationRef, that.pivotAuthorizationRef) &&
            Objects.equals(pivotOperationAt, that.pivotOperationAt) &&
            Objects.equals(pivotAmount, that.pivotAmount) &&
            Objects.equals(pivotParticipantCode, that.pivotParticipantCode) &&
            Objects.equals(amountSwitch, that.amountSwitch) &&
            Objects.equals(amountParticipant, that.amountParticipant) &&
            Objects.equals(state, that.state) &&
            Objects.equals(assigneeId, that.assigneeId) &&
            Objects.equals(dueDate, that.dueDate) &&
            Objects.equals(openedAt, that.openedAt) &&
            Objects.equals(disputeTicketId, that.disputeTicketId) &&
            Objects.equals(closedAt, that.closedAt) &&
            Objects.equals(reconciliationId, that.reconciliationId) &&
            Objects.equals(regularizationAttestationId, that.regularizationAttestationId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            reference,
            institutionId,
            type,
            pivotTransactionRef,
            pivotAuthorizationRef,
            pivotOperationAt,
            pivotAmount,
            pivotParticipantCode,
            amountSwitch,
            amountParticipant,
            state,
            assigneeId,
            dueDate,
            openedAt,
            disputeTicketId,
            closedAt,
            reconciliationId,
            regularizationAttestationId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SuspensCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalPivotTransactionRef().map(f -> "pivotTransactionRef=" + f + ", ").orElse("") +
            optionalPivotAuthorizationRef().map(f -> "pivotAuthorizationRef=" + f + ", ").orElse("") +
            optionalPivotOperationAt().map(f -> "pivotOperationAt=" + f + ", ").orElse("") +
            optionalPivotAmount().map(f -> "pivotAmount=" + f + ", ").orElse("") +
            optionalPivotParticipantCode().map(f -> "pivotParticipantCode=" + f + ", ").orElse("") +
            optionalAmountSwitch().map(f -> "amountSwitch=" + f + ", ").orElse("") +
            optionalAmountParticipant().map(f -> "amountParticipant=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalAssigneeId().map(f -> "assigneeId=" + f + ", ").orElse("") +
            optionalDueDate().map(f -> "dueDate=" + f + ", ").orElse("") +
            optionalOpenedAt().map(f -> "openedAt=" + f + ", ").orElse("") +
            optionalDisputeTicketId().map(f -> "disputeTicketId=" + f + ", ").orElse("") +
            optionalClosedAt().map(f -> "closedAt=" + f + ", ").orElse("") +
            optionalReconciliationId().map(f -> "reconciliationId=" + f + ", ").orElse("") +
            optionalRegularizationAttestationId().map(f -> "regularizationAttestationId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
