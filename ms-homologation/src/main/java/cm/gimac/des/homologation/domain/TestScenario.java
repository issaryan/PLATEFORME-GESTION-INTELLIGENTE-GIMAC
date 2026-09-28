package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.ScenarioMatch;
import cm.gimac.des.homologation.domain.enumeration.ScenarioOutcome;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Résultat de scénario repris du simulateur (algorithme 6.32), jamais recalculé
 * ni saisi à la main (RG-29). Idempotence par (simulatorScenarioId, executedAt),
 * clé unique posée par migration Liquibase.
 */
@Entity
@Table(name = "test_scenario")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TestScenario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 50)
    @Column(name = "simulator_scenario_id", length = 50, nullable = false)
    private String simulatorScenarioId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private ScenarioOutcome outcome;

    /**
     * Motif restitué par le simulateur.
     */
    @Size(max = 1000)
    @Column(name = "reason", length = 1000)
    private String reason;

    @NotNull
    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    /**
     * Référence de trace dans le simulateur, sans copie de la trace (RG-30).
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "trace_ref", length = 100, nullable = false)
    private String traceRef;

    @NotNull
    @Column(name = "replayed_at", nullable = false)
    private Instant replayedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "match", nullable = false)
    private ScenarioMatch match;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "catalog", "campaignSync" }, allowSetters = true)
    private Liaison liaison;

    /**
     * Vide tant que le scénario est en attente de rapprochement (RG-31).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "liaison", "reference" }, allowSetters = true)
    private Operation operation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public TestScenario id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSimulatorScenarioId() {
        return this.simulatorScenarioId;
    }

    public TestScenario simulatorScenarioId(String simulatorScenarioId) {
        this.setSimulatorScenarioId(simulatorScenarioId);
        return this;
    }

    public void setSimulatorScenarioId(String simulatorScenarioId) {
        this.simulatorScenarioId = simulatorScenarioId;
    }

    public ScenarioOutcome getOutcome() {
        return this.outcome;
    }

    public TestScenario outcome(ScenarioOutcome outcome) {
        this.setOutcome(outcome);
        return this;
    }

    public void setOutcome(ScenarioOutcome outcome) {
        this.outcome = outcome;
    }

    public String getReason() {
        return this.reason;
    }

    public TestScenario reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getExecutedAt() {
        return this.executedAt;
    }

    public TestScenario executedAt(Instant executedAt) {
        this.setExecutedAt(executedAt);
        return this;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public String getTraceRef() {
        return this.traceRef;
    }

    public TestScenario traceRef(String traceRef) {
        this.setTraceRef(traceRef);
        return this;
    }

    public void setTraceRef(String traceRef) {
        this.traceRef = traceRef;
    }

    public Instant getReplayedAt() {
        return this.replayedAt;
    }

    public TestScenario replayedAt(Instant replayedAt) {
        this.setReplayedAt(replayedAt);
        return this;
    }

    public void setReplayedAt(Instant replayedAt) {
        this.replayedAt = replayedAt;
    }

    public ScenarioMatch getMatch() {
        return this.match;
    }

    public TestScenario match(ScenarioMatch match) {
        this.setMatch(match);
        return this;
    }

    public void setMatch(ScenarioMatch match) {
        this.match = match;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public TestScenario liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public TestScenario operation(Operation operation) {
        this.setOperation(operation);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TestScenario)) {
            return false;
        }
        return getId() != null && getId().equals(((TestScenario) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TestScenario{" +
            "id=" + getId() +
            ", simulatorScenarioId='" + getSimulatorScenarioId() + "'" +
            ", outcome='" + getOutcome() + "'" +
            ", reason='" + getReason() + "'" +
            ", executedAt='" + getExecutedAt() + "'" +
            ", traceRef='" + getTraceRef() + "'" +
            ", replayedAt='" + getReplayedAt() + "'" +
            ", match='" + getMatch() + "'" +
            "}";
    }
}
