package cm.gimac.des.pilotage.service.criteria;

import cm.gimac.des.pilotage.domain.enumeration.OriginKind;
import cm.gimac.des.pilotage.domain.enumeration.PdcaPhase;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.ActionSheet} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.ActionSheetResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /action-sheets?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ActionSheetCriteria implements Serializable, Criteria {

    /**
     * Class for filtering OriginKind
     */
    public static class OriginKindFilter extends Filter<OriginKind> {

        public OriginKindFilter() {}

        public OriginKindFilter(OriginKindFilter filter) {
            super(filter);
        }

        @Override
        public OriginKindFilter copy() {
            return new OriginKindFilter(this);
        }
    }

    /**
     * Class for filtering PdcaPhase
     */
    public static class PdcaPhaseFilter extends Filter<PdcaPhase> {

        public PdcaPhaseFilter() {}

        public PdcaPhaseFilter(PdcaPhaseFilter filter) {
            super(filter);
        }

        @Override
        public PdcaPhaseFilter copy() {
            return new PdcaPhaseFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private OriginKindFilter originKind;

    private StringFilter originReference;

    private StringFilter originService;

    private PdcaPhaseFilter phase;

    private StringFilter objective;

    private StringFilter causeAnalysis;

    private BigDecimalFilter targetScore;

    private UUIDFilter ownerId;

    private LocalDateFilter dueDate;

    private LocalDateFilter observationEndDate;

    private StringFilter result;

    private IntegerFilter cycle;

    private UUIDFilter procedureDocumentRef;

    private InstantFilter closedAt;

    private UUIDFilter riskId;

    private Boolean distinct;

    public ActionSheetCriteria() {}

    public ActionSheetCriteria(ActionSheetCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.originKind = other.optionalOriginKind().map(OriginKindFilter::copy).orElse(null);
        this.originReference = other.optionalOriginReference().map(StringFilter::copy).orElse(null);
        this.originService = other.optionalOriginService().map(StringFilter::copy).orElse(null);
        this.phase = other.optionalPhase().map(PdcaPhaseFilter::copy).orElse(null);
        this.objective = other.optionalObjective().map(StringFilter::copy).orElse(null);
        this.causeAnalysis = other.optionalCauseAnalysis().map(StringFilter::copy).orElse(null);
        this.targetScore = other.optionalTargetScore().map(BigDecimalFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(UUIDFilter::copy).orElse(null);
        this.dueDate = other.optionalDueDate().map(LocalDateFilter::copy).orElse(null);
        this.observationEndDate = other.optionalObservationEndDate().map(LocalDateFilter::copy).orElse(null);
        this.result = other.optionalResult().map(StringFilter::copy).orElse(null);
        this.cycle = other.optionalCycle().map(IntegerFilter::copy).orElse(null);
        this.procedureDocumentRef = other.optionalProcedureDocumentRef().map(UUIDFilter::copy).orElse(null);
        this.closedAt = other.optionalClosedAt().map(InstantFilter::copy).orElse(null);
        this.riskId = other.optionalRiskId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ActionSheetCriteria copy() {
        return new ActionSheetCriteria(this);
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

    public OriginKindFilter getOriginKind() {
        return originKind;
    }

    public Optional<OriginKindFilter> optionalOriginKind() {
        return Optional.ofNullable(originKind);
    }

    public OriginKindFilter originKind() {
        if (originKind == null) {
            setOriginKind(new OriginKindFilter());
        }
        return originKind;
    }

    public void setOriginKind(OriginKindFilter originKind) {
        this.originKind = originKind;
    }

    public StringFilter getOriginReference() {
        return originReference;
    }

    public Optional<StringFilter> optionalOriginReference() {
        return Optional.ofNullable(originReference);
    }

    public StringFilter originReference() {
        if (originReference == null) {
            setOriginReference(new StringFilter());
        }
        return originReference;
    }

    public void setOriginReference(StringFilter originReference) {
        this.originReference = originReference;
    }

    public StringFilter getOriginService() {
        return originService;
    }

    public Optional<StringFilter> optionalOriginService() {
        return Optional.ofNullable(originService);
    }

    public StringFilter originService() {
        if (originService == null) {
            setOriginService(new StringFilter());
        }
        return originService;
    }

    public void setOriginService(StringFilter originService) {
        this.originService = originService;
    }

    public PdcaPhaseFilter getPhase() {
        return phase;
    }

    public Optional<PdcaPhaseFilter> optionalPhase() {
        return Optional.ofNullable(phase);
    }

    public PdcaPhaseFilter phase() {
        if (phase == null) {
            setPhase(new PdcaPhaseFilter());
        }
        return phase;
    }

    public void setPhase(PdcaPhaseFilter phase) {
        this.phase = phase;
    }

    public StringFilter getObjective() {
        return objective;
    }

    public Optional<StringFilter> optionalObjective() {
        return Optional.ofNullable(objective);
    }

    public StringFilter objective() {
        if (objective == null) {
            setObjective(new StringFilter());
        }
        return objective;
    }

    public void setObjective(StringFilter objective) {
        this.objective = objective;
    }

    public StringFilter getCauseAnalysis() {
        return causeAnalysis;
    }

    public Optional<StringFilter> optionalCauseAnalysis() {
        return Optional.ofNullable(causeAnalysis);
    }

    public StringFilter causeAnalysis() {
        if (causeAnalysis == null) {
            setCauseAnalysis(new StringFilter());
        }
        return causeAnalysis;
    }

    public void setCauseAnalysis(StringFilter causeAnalysis) {
        this.causeAnalysis = causeAnalysis;
    }

    public BigDecimalFilter getTargetScore() {
        return targetScore;
    }

    public Optional<BigDecimalFilter> optionalTargetScore() {
        return Optional.ofNullable(targetScore);
    }

    public BigDecimalFilter targetScore() {
        if (targetScore == null) {
            setTargetScore(new BigDecimalFilter());
        }
        return targetScore;
    }

    public void setTargetScore(BigDecimalFilter targetScore) {
        this.targetScore = targetScore;
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

    public LocalDateFilter getObservationEndDate() {
        return observationEndDate;
    }

    public Optional<LocalDateFilter> optionalObservationEndDate() {
        return Optional.ofNullable(observationEndDate);
    }

    public LocalDateFilter observationEndDate() {
        if (observationEndDate == null) {
            setObservationEndDate(new LocalDateFilter());
        }
        return observationEndDate;
    }

    public void setObservationEndDate(LocalDateFilter observationEndDate) {
        this.observationEndDate = observationEndDate;
    }

    public StringFilter getResult() {
        return result;
    }

    public Optional<StringFilter> optionalResult() {
        return Optional.ofNullable(result);
    }

    public StringFilter result() {
        if (result == null) {
            setResult(new StringFilter());
        }
        return result;
    }

    public void setResult(StringFilter result) {
        this.result = result;
    }

    public IntegerFilter getCycle() {
        return cycle;
    }

    public Optional<IntegerFilter> optionalCycle() {
        return Optional.ofNullable(cycle);
    }

    public IntegerFilter cycle() {
        if (cycle == null) {
            setCycle(new IntegerFilter());
        }
        return cycle;
    }

    public void setCycle(IntegerFilter cycle) {
        this.cycle = cycle;
    }

    public UUIDFilter getProcedureDocumentRef() {
        return procedureDocumentRef;
    }

    public Optional<UUIDFilter> optionalProcedureDocumentRef() {
        return Optional.ofNullable(procedureDocumentRef);
    }

    public UUIDFilter procedureDocumentRef() {
        if (procedureDocumentRef == null) {
            setProcedureDocumentRef(new UUIDFilter());
        }
        return procedureDocumentRef;
    }

    public void setProcedureDocumentRef(UUIDFilter procedureDocumentRef) {
        this.procedureDocumentRef = procedureDocumentRef;
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

    public UUIDFilter getRiskId() {
        return riskId;
    }

    public Optional<UUIDFilter> optionalRiskId() {
        return Optional.ofNullable(riskId);
    }

    public UUIDFilter riskId() {
        if (riskId == null) {
            setRiskId(new UUIDFilter());
        }
        return riskId;
    }

    public void setRiskId(UUIDFilter riskId) {
        this.riskId = riskId;
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
        final ActionSheetCriteria that = (ActionSheetCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(originKind, that.originKind) &&
            Objects.equals(originReference, that.originReference) &&
            Objects.equals(originService, that.originService) &&
            Objects.equals(phase, that.phase) &&
            Objects.equals(objective, that.objective) &&
            Objects.equals(causeAnalysis, that.causeAnalysis) &&
            Objects.equals(targetScore, that.targetScore) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(dueDate, that.dueDate) &&
            Objects.equals(observationEndDate, that.observationEndDate) &&
            Objects.equals(result, that.result) &&
            Objects.equals(cycle, that.cycle) &&
            Objects.equals(procedureDocumentRef, that.procedureDocumentRef) &&
            Objects.equals(closedAt, that.closedAt) &&
            Objects.equals(riskId, that.riskId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            originKind,
            originReference,
            originService,
            phase,
            objective,
            causeAnalysis,
            targetScore,
            ownerId,
            dueDate,
            observationEndDate,
            result,
            cycle,
            procedureDocumentRef,
            closedAt,
            riskId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ActionSheetCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalOriginKind().map(f -> "originKind=" + f + ", ").orElse("") +
            optionalOriginReference().map(f -> "originReference=" + f + ", ").orElse("") +
            optionalOriginService().map(f -> "originService=" + f + ", ").orElse("") +
            optionalPhase().map(f -> "phase=" + f + ", ").orElse("") +
            optionalObjective().map(f -> "objective=" + f + ", ").orElse("") +
            optionalCauseAnalysis().map(f -> "causeAnalysis=" + f + ", ").orElse("") +
            optionalTargetScore().map(f -> "targetScore=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalDueDate().map(f -> "dueDate=" + f + ", ").orElse("") +
            optionalObservationEndDate().map(f -> "observationEndDate=" + f + ", ").orElse("") +
            optionalResult().map(f -> "result=" + f + ", ").orElse("") +
            optionalCycle().map(f -> "cycle=" + f + ", ").orElse("") +
            optionalProcedureDocumentRef().map(f -> "procedureDocumentRef=" + f + ", ").orElse("") +
            optionalClosedAt().map(f -> "closedAt=" + f + ", ").orElse("") +
            optionalRiskId().map(f -> "riskId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
