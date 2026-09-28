package cm.gimac.des.pilotage.service.dto;

import cm.gimac.des.pilotage.domain.enumeration.OriginKind;
import cm.gimac.des.pilotage.domain.enumeration.TaskState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.Task} entity.
 */
@Schema(
    description = "Tâche nominative à responsable unique (RG-20). Son origine est une\nréférence typée ; pour une origine externe (ticket, opération, indicateur),\nseule la référence est conservée (BNF-04)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TaskDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 255)
    private String title;

    @NotNull
    @Schema(description = "Responsable unique : identifiant du collaborateur (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID assigneeId;

    @NotNull
    private LocalDate dueDate;

    @NotNull
    private OriginKind originKind;

    @Size(max = 100)
    @Schema(description = "Référence de l'origine, par exemple TCK-000318 ou l'identifiant d'une décision.")
    private String originReference;

    @Size(max = 50)
    @Schema(description = "Service source de l'origine, par exemple MS-INCIDENT.")
    private String originService;

    private Instant startedAt;

    private Instant completedAt;

    @NotNull
    private TaskState state;

    @NotNull
    @Min(value = 0)
    @Max(value = 4)
    @Schema(description = "Niveau de relance atteint, de 0 à 4 (formule 6.67).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reminderLevel;

    @Schema(description = "Clôture proposée au responsable sur événement d'origine, jamais déplacée d'office.")
    private Instant closureProposedAt;

    @NotNull
    private BoardDTO board;

    @NotNull
    private BoardColumnDTO boardColumn;

    @Schema(description = "Fiche d'action dont la tâche est une action concrète (origine FICHE_ACTION).")
    private ActionSheetDTO actionSheet;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public OriginKind getOriginKind() {
        return originKind;
    }

    public void setOriginKind(OriginKind originKind) {
        this.originKind = originKind;
    }

    public String getOriginReference() {
        return originReference;
    }

    public void setOriginReference(String originReference) {
        this.originReference = originReference;
    }

    public String getOriginService() {
        return originService;
    }

    public void setOriginService(String originService) {
        this.originService = originService;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public Integer getReminderLevel() {
        return reminderLevel;
    }

    public void setReminderLevel(Integer reminderLevel) {
        this.reminderLevel = reminderLevel;
    }

    public Instant getClosureProposedAt() {
        return closureProposedAt;
    }

    public void setClosureProposedAt(Instant closureProposedAt) {
        this.closureProposedAt = closureProposedAt;
    }

    public BoardDTO getBoard() {
        return board;
    }

    public void setBoard(BoardDTO board) {
        this.board = board;
    }

    public BoardColumnDTO getBoardColumn() {
        return boardColumn;
    }

    public void setBoardColumn(BoardColumnDTO boardColumn) {
        this.boardColumn = boardColumn;
    }

    public ActionSheetDTO getActionSheet() {
        return actionSheet;
    }

    public void setActionSheet(ActionSheetDTO actionSheet) {
        this.actionSheet = actionSheet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaskDTO)) {
            return false;
        }

        TaskDTO taskDTO = (TaskDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, taskDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TaskDTO{" +
            "id='" + getId() + "'" +
            ", title='" + getTitle() + "'" +
            ", assigneeId='" + getAssigneeId() + "'" +
            ", dueDate='" + getDueDate() + "'" +
            ", originKind='" + getOriginKind() + "'" +
            ", originReference='" + getOriginReference() + "'" +
            ", originService='" + getOriginService() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", completedAt='" + getCompletedAt() + "'" +
            ", state='" + getState() + "'" +
            ", reminderLevel=" + getReminderLevel() +
            ", closureProposedAt='" + getClosureProposedAt() + "'" +
            ", board=" + getBoard() +
            ", boardColumn=" + getBoardColumn() +
            ", actionSheet=" + getActionSheet() +
            "}";
    }
}
