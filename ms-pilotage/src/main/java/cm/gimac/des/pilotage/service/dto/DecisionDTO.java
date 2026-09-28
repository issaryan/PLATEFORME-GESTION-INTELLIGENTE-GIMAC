package cm.gimac.des.pilotage.service.dto;

import cm.gimac.des.pilotage.domain.enumeration.DecisionState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.Decision} entity.
 */
@Schema(
    description = "Décision de réunion saisie de façon structurée (D-27) : texte, porteur\nunique, échéance, critères. Elle engendre exactement une tâche (RG-51) et\nn'est plus modifiable après le dépôt du compte rendu."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DecisionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 2000)
    private String text;

    @NotNull
    @Schema(description = "Porteur unique de la décision (MS-IAM), responsable de la tâche.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID ownerId;

    @NotNull
    private LocalDate dueDate;

    @Schema(description = "Critères d'acceptation de la tâche : liste JSON, au moins un.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Lob
    private String acceptanceCriteria;

    @NotNull
    private DecisionState state;

    @Size(max = 500)
    @Schema(description = "Motif du renvoi au secrétaire (porteur inactif, absent ou échéance invalide).")
    private String returnReason;

    private Instant convertedAt;

    @Schema(description = "Tâche engendrée par la décision (une et une seule, algorithme 6.55).")
    private TaskDTO task;

    @NotNull
    private MeetingDTO meeting;

    @Schema(description = "Décision rectifiée par une décision ultérieure (le compte rendu est scellé).")
    private DecisionDTO rectifies;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public DecisionState getState() {
        return state;
    }

    public void setState(DecisionState state) {
        this.state = state;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(Instant convertedAt) {
        this.convertedAt = convertedAt;
    }

    public TaskDTO getTask() {
        return task;
    }

    public void setTask(TaskDTO task) {
        this.task = task;
    }

    public MeetingDTO getMeeting() {
        return meeting;
    }

    public void setMeeting(MeetingDTO meeting) {
        this.meeting = meeting;
    }

    public DecisionDTO getRectifies() {
        return rectifies;
    }

    public void setRectifies(DecisionDTO rectifies) {
        this.rectifies = rectifies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DecisionDTO)) {
            return false;
        }

        DecisionDTO decisionDTO = (DecisionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, decisionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DecisionDTO{" +
            "id='" + getId() + "'" +
            ", text='" + getText() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", dueDate='" + getDueDate() + "'" +
            ", acceptanceCriteria='" + getAcceptanceCriteria() + "'" +
            ", state='" + getState() + "'" +
            ", returnReason='" + getReturnReason() + "'" +
            ", convertedAt='" + getConvertedAt() + "'" +
            ", task=" + getTask() +
            ", meeting=" + getMeeting() +
            ", rectifies=" + getRectifies() +
            "}";
    }
}
