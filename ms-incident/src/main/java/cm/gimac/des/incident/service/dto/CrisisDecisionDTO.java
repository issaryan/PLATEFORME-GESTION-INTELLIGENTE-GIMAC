package cm.gimac.des.incident.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.CrisisDecision} entity.
 */
@Schema(description = "Décision de la cellule ; une décision portant une action devient une tâche.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CrisisDecisionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 2000)
    private String statement;

    @NotNull
    private UUID decidedBy;

    @NotNull
    private Instant decidedAt;

    @Size(max = 50)
    @Schema(description = "Référence de tâche rendue par MS-PILOTAGE.")
    private String taskRef;

    @NotNull
    private CrisisCellDTO cell;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getTaskRef() {
        return taskRef;
    }

    public void setTaskRef(String taskRef) {
        this.taskRef = taskRef;
    }

    public CrisisCellDTO getCell() {
        return cell;
    }

    public void setCell(CrisisCellDTO cell) {
        this.cell = cell;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrisisDecisionDTO)) {
            return false;
        }

        CrisisDecisionDTO crisisDecisionDTO = (CrisisDecisionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, crisisDecisionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CrisisDecisionDTO{" +
            "id='" + getId() + "'" +
            ", statement='" + getStatement() + "'" +
            ", decidedBy='" + getDecidedBy() + "'" +
            ", decidedAt='" + getDecidedAt() + "'" +
            ", taskRef='" + getTaskRef() + "'" +
            ", cell=" + getCell() +
            "}";
    }
}
