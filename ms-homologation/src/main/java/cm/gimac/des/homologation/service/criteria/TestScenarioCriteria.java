package cm.gimac.des.homologation.service.criteria;

import cm.gimac.des.homologation.domain.enumeration.ScenarioMatch;
import cm.gimac.des.homologation.domain.enumeration.ScenarioOutcome;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.homologation.domain.TestScenario} entity. This class is used
 * in {@link cm.gimac.des.homologation.web.rest.TestScenarioResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /test-scenarios?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TestScenarioCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ScenarioOutcome
     */
    public static class ScenarioOutcomeFilter extends Filter<ScenarioOutcome> {

        public ScenarioOutcomeFilter() {}

        public ScenarioOutcomeFilter(ScenarioOutcomeFilter filter) {
            super(filter);
        }

        @Override
        public ScenarioOutcomeFilter copy() {
            return new ScenarioOutcomeFilter(this);
        }
    }

    /**
     * Class for filtering ScenarioMatch
     */
    public static class ScenarioMatchFilter extends Filter<ScenarioMatch> {

        public ScenarioMatchFilter() {}

        public ScenarioMatchFilter(ScenarioMatchFilter filter) {
            super(filter);
        }

        @Override
        public ScenarioMatchFilter copy() {
            return new ScenarioMatchFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter simulatorScenarioId;

    private ScenarioOutcomeFilter outcome;

    private StringFilter reason;

    private InstantFilter executedAt;

    private StringFilter traceRef;

    private InstantFilter replayedAt;

    private ScenarioMatchFilter match;

    private UUIDFilter liaisonId;

    private UUIDFilter operationId;

    private Boolean distinct;

    public TestScenarioCriteria() {}

    public TestScenarioCriteria(TestScenarioCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.simulatorScenarioId = other.optionalSimulatorScenarioId().map(StringFilter::copy).orElse(null);
        this.outcome = other.optionalOutcome().map(ScenarioOutcomeFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.executedAt = other.optionalExecutedAt().map(InstantFilter::copy).orElse(null);
        this.traceRef = other.optionalTraceRef().map(StringFilter::copy).orElse(null);
        this.replayedAt = other.optionalReplayedAt().map(InstantFilter::copy).orElse(null);
        this.match = other.optionalMatch().map(ScenarioMatchFilter::copy).orElse(null);
        this.liaisonId = other.optionalLiaisonId().map(UUIDFilter::copy).orElse(null);
        this.operationId = other.optionalOperationId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public TestScenarioCriteria copy() {
        return new TestScenarioCriteria(this);
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

    public StringFilter getSimulatorScenarioId() {
        return simulatorScenarioId;
    }

    public Optional<StringFilter> optionalSimulatorScenarioId() {
        return Optional.ofNullable(simulatorScenarioId);
    }

    public StringFilter simulatorScenarioId() {
        if (simulatorScenarioId == null) {
            setSimulatorScenarioId(new StringFilter());
        }
        return simulatorScenarioId;
    }

    public void setSimulatorScenarioId(StringFilter simulatorScenarioId) {
        this.simulatorScenarioId = simulatorScenarioId;
    }

    public ScenarioOutcomeFilter getOutcome() {
        return outcome;
    }

    public Optional<ScenarioOutcomeFilter> optionalOutcome() {
        return Optional.ofNullable(outcome);
    }

    public ScenarioOutcomeFilter outcome() {
        if (outcome == null) {
            setOutcome(new ScenarioOutcomeFilter());
        }
        return outcome;
    }

    public void setOutcome(ScenarioOutcomeFilter outcome) {
        this.outcome = outcome;
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

    public StringFilter getTraceRef() {
        return traceRef;
    }

    public Optional<StringFilter> optionalTraceRef() {
        return Optional.ofNullable(traceRef);
    }

    public StringFilter traceRef() {
        if (traceRef == null) {
            setTraceRef(new StringFilter());
        }
        return traceRef;
    }

    public void setTraceRef(StringFilter traceRef) {
        this.traceRef = traceRef;
    }

    public InstantFilter getReplayedAt() {
        return replayedAt;
    }

    public Optional<InstantFilter> optionalReplayedAt() {
        return Optional.ofNullable(replayedAt);
    }

    public InstantFilter replayedAt() {
        if (replayedAt == null) {
            setReplayedAt(new InstantFilter());
        }
        return replayedAt;
    }

    public void setReplayedAt(InstantFilter replayedAt) {
        this.replayedAt = replayedAt;
    }

    public ScenarioMatchFilter getMatch() {
        return match;
    }

    public Optional<ScenarioMatchFilter> optionalMatch() {
        return Optional.ofNullable(match);
    }

    public ScenarioMatchFilter match() {
        if (match == null) {
            setMatch(new ScenarioMatchFilter());
        }
        return match;
    }

    public void setMatch(ScenarioMatchFilter match) {
        this.match = match;
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
        final TestScenarioCriteria that = (TestScenarioCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(simulatorScenarioId, that.simulatorScenarioId) &&
            Objects.equals(outcome, that.outcome) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(executedAt, that.executedAt) &&
            Objects.equals(traceRef, that.traceRef) &&
            Objects.equals(replayedAt, that.replayedAt) &&
            Objects.equals(match, that.match) &&
            Objects.equals(liaisonId, that.liaisonId) &&
            Objects.equals(operationId, that.operationId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            simulatorScenarioId,
            outcome,
            reason,
            executedAt,
            traceRef,
            replayedAt,
            match,
            liaisonId,
            operationId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TestScenarioCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalSimulatorScenarioId().map(f -> "simulatorScenarioId=" + f + ", ").orElse("") +
            optionalOutcome().map(f -> "outcome=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalExecutedAt().map(f -> "executedAt=" + f + ", ").orElse("") +
            optionalTraceRef().map(f -> "traceRef=" + f + ", ").orElse("") +
            optionalReplayedAt().map(f -> "replayedAt=" + f + ", ").orElse("") +
            optionalMatch().map(f -> "match=" + f + ", ").orElse("") +
            optionalLiaisonId().map(f -> "liaisonId=" + f + ", ").orElse("") +
            optionalOperationId().map(f -> "operationId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
