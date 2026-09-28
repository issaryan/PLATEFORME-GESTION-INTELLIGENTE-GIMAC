package cm.gimac.des.incident.service.dto;

import cm.gimac.des.incident.domain.enumeration.RemediationKind;
import cm.gimac.des.incident.domain.enumeration.RemediationState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.RemediationAction} entity.
 */
@Schema(
    description = "Action de remédiation demandée à MS-PILOTAGE (incident.remediation-requested.v1),\ndont l'état suit les événements de tâche (écart CC complété : entité exigée\npar le critère « remédiation » de l'algorithme 6.43)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RemediationActionDTO implements Serializable {

    private UUID id;

    @NotNull
    private RemediationKind kind;

    @NotNull
    @Size(max = 2000)
    private String description;

    @NotNull
    @Schema(description = "Porteur nominatif de la tâche.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID ownerId;

    @NotNull
    private LocalDate dueDate;

    @NotNull
    private Instant requestedAt;

    @Size(max = 50)
    @Schema(description = "Référence de la tâche rendue par MS-PILOTAGE, par exemple TSK-2026-0712.")
    private String taskRef;

    @NotNull
    private RemediationState state;

    private Instant completedAt;

    @NotNull
    private IncidentDTO incident;

    @Schema(description = "Décision de crise à l'origine de l'action, le cas échéant.")
    private CrisisDecisionDTO crisisDecision;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RemediationKind getKind() {
        return kind;
    }

    public void setKind(RemediationKind kind) {
        this.kind = kind;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getTaskRef() {
        return taskRef;
    }

    public void setTaskRef(String taskRef) {
        this.taskRef = taskRef;
    }

    public RemediationState getState() {
        return state;
    }

    public void setState(RemediationState state) {
        this.state = state;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public IncidentDTO getIncident() {
        return incident;
    }

    public void setIncident(IncidentDTO incident) {
        this.incident = incident;
    }

    public CrisisDecisionDTO getCrisisDecision() {
        return crisisDecision;
    }

    public void setCrisisDecision(CrisisDecisionDTO crisisDecision) {
        this.crisisDecision = crisisDecision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemediationActionDTO)) {
            return false;
        }

        RemediationActionDTO remediationActionDTO = (RemediationActionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, remediationActionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RemediationActionDTO{" +
            "id='" + getId() + "'" +
            ", kind='" + getKind() + "'" +
            ", description='" + getDescription() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", dueDate='" + getDueDate() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", taskRef='" + getTaskRef() + "'" +
            ", state='" + getState() + "'" +
            ", completedAt='" + getCompletedAt() + "'" +
            ", incident=" + getIncident() +
            ", crisisDecision=" + getCrisisDecision() +
            "}";
    }
}
