package cm.gimac.des.pilotage.domain;

import cm.gimac.des.pilotage.domain.enumeration.OriginKind;
import cm.gimac.des.pilotage.domain.enumeration.TaskState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tâche nominative à responsable unique (RG-20). Son origine est une
 * référence typée ; pour une origine externe (ticket, opération, indicateur),
 * seule la référence est conservée (BNF-04).
 */
@Entity
@Table(name = "task")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 255)
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /**
     * Responsable unique : identifiant du collaborateur (MS-IAM).
     */
    @NotNull
    @Column(name = "assignee_id", nullable = false)
    private UUID assigneeId;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "origin_kind", nullable = false)
    private OriginKind originKind;

    /**
     * Référence de l'origine, par exemple TCK-000318 ou l'identifiant d'une décision.
     */
    @Size(max = 100)
    @Column(name = "origin_reference", length = 100)
    private String originReference;

    /**
     * Service source de l'origine, par exemple MS-INCIDENT.
     */
    @Size(max = 50)
    @Column(name = "origin_service", length = 50)
    private String originService;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private TaskState state;

    /**
     * Niveau de relance atteint, de 0 à 4 (formule 6.67).
     */
    @NotNull
    @Min(value = 0)
    @Max(value = 4)
    @Column(name = "reminder_level", nullable = false)
    private Integer reminderLevel;

    /**
     * Clôture proposée au responsable sur événement d'origine, jamais déplacée d'office.
     */
    @Column(name = "closure_proposed_at")
    private Instant closureProposedAt;

    @ManyToOne(optional = false)
    @NotNull
    private Board board;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "board" }, allowSetters = true)
    private BoardColumn boardColumn;

    /**
     * Fiche d'action dont la tâche est une action concrète (origine FICHE_ACTION).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "risk" }, allowSetters = true)
    private ActionSheet actionSheet;

    @JsonIgnoreProperties(value = { "task", "meeting", "rectifies" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "task")
    private Decision decision;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Task id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Task title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getAssigneeId() {
        return this.assigneeId;
    }

    public Task assigneeId(UUID assigneeId) {
        this.setAssigneeId(assigneeId);
        return this;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public Task dueDate(LocalDate dueDate) {
        this.setDueDate(dueDate);
        return this;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public OriginKind getOriginKind() {
        return this.originKind;
    }

    public Task originKind(OriginKind originKind) {
        this.setOriginKind(originKind);
        return this;
    }

    public void setOriginKind(OriginKind originKind) {
        this.originKind = originKind;
    }

    public String getOriginReference() {
        return this.originReference;
    }

    public Task originReference(String originReference) {
        this.setOriginReference(originReference);
        return this;
    }

    public void setOriginReference(String originReference) {
        this.originReference = originReference;
    }

    public String getOriginService() {
        return this.originService;
    }

    public Task originService(String originService) {
        this.setOriginService(originService);
        return this;
    }

    public void setOriginService(String originService) {
        this.originService = originService;
    }

    public Instant getStartedAt() {
        return this.startedAt;
    }

    public Task startedAt(Instant startedAt) {
        this.setStartedAt(startedAt);
        return this;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return this.completedAt;
    }

    public Task completedAt(Instant completedAt) {
        this.setCompletedAt(completedAt);
        return this;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public TaskState getState() {
        return this.state;
    }

    public Task state(TaskState state) {
        this.setState(state);
        return this;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public Integer getReminderLevel() {
        return this.reminderLevel;
    }

    public Task reminderLevel(Integer reminderLevel) {
        this.setReminderLevel(reminderLevel);
        return this;
    }

    public void setReminderLevel(Integer reminderLevel) {
        this.reminderLevel = reminderLevel;
    }

    public Instant getClosureProposedAt() {
        return this.closureProposedAt;
    }

    public Task closureProposedAt(Instant closureProposedAt) {
        this.setClosureProposedAt(closureProposedAt);
        return this;
    }

    public void setClosureProposedAt(Instant closureProposedAt) {
        this.closureProposedAt = closureProposedAt;
    }

    public Board getBoard() {
        return this.board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public Task board(Board board) {
        this.setBoard(board);
        return this;
    }

    public BoardColumn getBoardColumn() {
        return this.boardColumn;
    }

    public void setBoardColumn(BoardColumn boardColumn) {
        this.boardColumn = boardColumn;
    }

    public Task boardColumn(BoardColumn boardColumn) {
        this.setBoardColumn(boardColumn);
        return this;
    }

    public ActionSheet getActionSheet() {
        return this.actionSheet;
    }

    public void setActionSheet(ActionSheet actionSheet) {
        this.actionSheet = actionSheet;
    }

    public Task actionSheet(ActionSheet actionSheet) {
        this.setActionSheet(actionSheet);
        return this;
    }

    public Decision getDecision() {
        return this.decision;
    }

    public void setDecision(Decision decision) {
        if (this.decision != null) {
            this.decision.setTask(null);
        }
        if (decision != null) {
            decision.setTask(this);
        }
        this.decision = decision;
    }

    public Task decision(Decision decision) {
        this.setDecision(decision);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Task)) {
            return false;
        }
        return getId() != null && getId().equals(((Task) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Task{" +
            "id=" + getId() +
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
            "}";
    }
}
