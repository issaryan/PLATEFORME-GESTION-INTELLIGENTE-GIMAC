package cm.gimac.des.pilotage.domain;

import cm.gimac.des.pilotage.domain.enumeration.DecisionState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Décision de réunion saisie de façon structurée (D-27) : texte, porteur
 * unique, échéance, critères. Elle engendre exactement une tâche (RG-51) et
 * n'est plus modifiable après le dépôt du compte rendu.
 */
@Entity
@Table(name = "decision")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Decision implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 2000)
    @Column(name = "text", length = 2000, nullable = false)
    private String text;

    /**
     * Porteur unique de la décision (MS-IAM), responsable de la tâche.
     */
    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Critères d'acceptation de la tâche : liste JSON, au moins un.
     */
    @Lob
    @Column(name = "acceptance_criteria", nullable = false)
    private String acceptanceCriteria;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private DecisionState state;

    /**
     * Motif du renvoi au secrétaire (porteur inactif, absent ou échéance invalide).
     */
    @Size(max = 500)
    @Column(name = "return_reason", length = 500)
    private String returnReason;

    @Column(name = "converted_at")
    private Instant convertedAt;

    /**
     * Tâche engendrée par la décision (une et une seule, algorithme 6.55).
     */
    @JsonIgnoreProperties(value = { "board", "boardColumn", "actionSheet", "decision" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private Task task;

    @ManyToOne(optional = false)
    @NotNull
    private Meeting meeting;

    /**
     * Décision rectifiée par une décision ultérieure (le compte rendu est scellé).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "task", "meeting", "rectifies" }, allowSetters = true)
    private Decision rectifies;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Decision id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getText() {
        return this.text;
    }

    public Decision text(String text) {
        this.setText(text);
        return this;
    }

    public void setText(String text) {
        this.text = text;
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public Decision ownerId(UUID ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public Decision dueDate(LocalDate dueDate) {
        this.setDueDate(dueDate);
        return this;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getAcceptanceCriteria() {
        return this.acceptanceCriteria;
    }

    public Decision acceptanceCriteria(String acceptanceCriteria) {
        this.setAcceptanceCriteria(acceptanceCriteria);
        return this;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public DecisionState getState() {
        return this.state;
    }

    public Decision state(DecisionState state) {
        this.setState(state);
        return this;
    }

    public void setState(DecisionState state) {
        this.state = state;
    }

    public String getReturnReason() {
        return this.returnReason;
    }

    public Decision returnReason(String returnReason) {
        this.setReturnReason(returnReason);
        return this;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public Instant getConvertedAt() {
        return this.convertedAt;
    }

    public Decision convertedAt(Instant convertedAt) {
        this.setConvertedAt(convertedAt);
        return this;
    }

    public void setConvertedAt(Instant convertedAt) {
        this.convertedAt = convertedAt;
    }

    public Task getTask() {
        return this.task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Decision task(Task task) {
        this.setTask(task);
        return this;
    }

    public Meeting getMeeting() {
        return this.meeting;
    }

    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }

    public Decision meeting(Meeting meeting) {
        this.setMeeting(meeting);
        return this;
    }

    public Decision getRectifies() {
        return this.rectifies;
    }

    public void setRectifies(Decision decision) {
        this.rectifies = decision;
    }

    public Decision rectifies(Decision decision) {
        this.setRectifies(decision);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Decision)) {
            return false;
        }
        return getId() != null && getId().equals(((Decision) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Decision{" +
            "id=" + getId() +
            ", text='" + getText() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", dueDate='" + getDueDate() + "'" +
            ", acceptanceCriteria='" + getAcceptanceCriteria() + "'" +
            ", state='" + getState() + "'" +
            ", returnReason='" + getReturnReason() + "'" +
            ", convertedAt='" + getConvertedAt() + "'" +
            "}";
    }
}
