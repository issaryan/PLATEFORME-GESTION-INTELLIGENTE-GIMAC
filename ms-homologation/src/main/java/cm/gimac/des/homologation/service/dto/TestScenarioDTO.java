package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.ScenarioMatch;
import cm.gimac.des.homologation.domain.enumeration.ScenarioOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.TestScenario} entity.
 */
@Schema(
    description = "Résultat de scénario repris du simulateur (algorithme 6.32), jamais recalculé\nni saisi à la main (RG-29). Idempotence par (simulatorScenarioId, executedAt),\nclé unique posée par migration Liquibase."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TestScenarioDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 50)
    private String simulatorScenarioId;

    @NotNull
    private ScenarioOutcome outcome;

    @Size(max = 1000)
    @Schema(description = "Motif restitué par le simulateur.")
    private String reason;

    @NotNull
    private Instant executedAt;

    @NotNull
    @Size(max = 100)
    @Schema(
        description = "Référence de trace dans le simulateur, sans copie de la trace (RG-30).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String traceRef;

    @NotNull
    private Instant replayedAt;

    @NotNull
    private ScenarioMatch match;

    @NotNull
    private LiaisonDTO liaison;

    @Schema(description = "Vide tant que le scénario est en attente de rapprochement (RG-31).")
    private OperationDTO operation;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSimulatorScenarioId() {
        return simulatorScenarioId;
    }

    public void setSimulatorScenarioId(String simulatorScenarioId) {
        this.simulatorScenarioId = simulatorScenarioId;
    }

    public ScenarioOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(ScenarioOutcome outcome) {
        this.outcome = outcome;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public String getTraceRef() {
        return traceRef;
    }

    public void setTraceRef(String traceRef) {
        this.traceRef = traceRef;
    }

    public Instant getReplayedAt() {
        return replayedAt;
    }

    public void setReplayedAt(Instant replayedAt) {
        this.replayedAt = replayedAt;
    }

    public ScenarioMatch getMatch() {
        return match;
    }

    public void setMatch(ScenarioMatch match) {
        this.match = match;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    public OperationDTO getOperation() {
        return operation;
    }

    public void setOperation(OperationDTO operation) {
        this.operation = operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TestScenarioDTO)) {
            return false;
        }

        TestScenarioDTO testScenarioDTO = (TestScenarioDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, testScenarioDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TestScenarioDTO{" +
            "id='" + getId() + "'" +
            ", simulatorScenarioId='" + getSimulatorScenarioId() + "'" +
            ", outcome='" + getOutcome() + "'" +
            ", reason='" + getReason() + "'" +
            ", executedAt='" + getExecutedAt() + "'" +
            ", traceRef='" + getTraceRef() + "'" +
            ", replayedAt='" + getReplayedAt() + "'" +
            ", match='" + getMatch() + "'" +
            ", liaison=" + getLiaison() +
            ", operation=" + getOperation() +
            "}";
    }
}
