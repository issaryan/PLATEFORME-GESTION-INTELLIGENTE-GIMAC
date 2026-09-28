package cm.gimac.des.pilotage.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.AcceptanceCriterion} entity.
 */
@Schema(description = "Critère d'acceptation d'une tâche ; au moins un par tâche (CC §6.1.12.1).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AcceptanceCriterionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 500)
    private String label;

    @NotNull
    private Boolean satisfied;

    @NotNull
    @Schema(
        description = "Une pièce justificative en GED est exigée pour satisfaire ce critère.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean evidenceRequired;

    @Schema(description = "Pièce justificative : identifiant du document dans MS-DOCUMENT.")
    private UUID evidenceRef;

    private Instant satisfiedAt;

    @NotNull
    private TaskDTO task;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getSatisfied() {
        return satisfied;
    }

    public void setSatisfied(Boolean satisfied) {
        this.satisfied = satisfied;
    }

    public Boolean getEvidenceRequired() {
        return evidenceRequired;
    }

    public void setEvidenceRequired(Boolean evidenceRequired) {
        this.evidenceRequired = evidenceRequired;
    }

    public UUID getEvidenceRef() {
        return evidenceRef;
    }

    public void setEvidenceRef(UUID evidenceRef) {
        this.evidenceRef = evidenceRef;
    }

    public Instant getSatisfiedAt() {
        return satisfiedAt;
    }

    public void setSatisfiedAt(Instant satisfiedAt) {
        this.satisfiedAt = satisfiedAt;
    }

    public TaskDTO getTask() {
        return task;
    }

    public void setTask(TaskDTO task) {
        this.task = task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AcceptanceCriterionDTO)) {
            return false;
        }

        AcceptanceCriterionDTO acceptanceCriterionDTO = (AcceptanceCriterionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, acceptanceCriterionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AcceptanceCriterionDTO{" +
            "id='" + getId() + "'" +
            ", label='" + getLabel() + "'" +
            ", satisfied='" + getSatisfied() + "'" +
            ", evidenceRequired='" + getEvidenceRequired() + "'" +
            ", evidenceRef='" + getEvidenceRef() + "'" +
            ", satisfiedAt='" + getSatisfiedAt() + "'" +
            ", task=" + getTask() +
            "}";
    }
}
