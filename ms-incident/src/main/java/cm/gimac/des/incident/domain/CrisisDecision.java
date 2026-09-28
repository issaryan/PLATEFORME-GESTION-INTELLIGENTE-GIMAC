package cm.gimac.des.incident.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Décision de la cellule ; une décision portant une action devient une tâche.
 */
@Entity
@Table(name = "crisis_decision")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CrisisDecision implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 2000)
    @Column(name = "statement", length = 2000, nullable = false)
    private String statement;

    @NotNull
    @Column(name = "decided_by", nullable = false)
    private UUID decidedBy;

    @NotNull
    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    /**
     * Référence de tâche rendue par MS-PILOTAGE.
     */
    @Size(max = 50)
    @Column(name = "task_ref", length = 50)
    private String taskRef;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "incident" }, allowSetters = true)
    private CrisisCell cell;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CrisisDecision id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatement() {
        return this.statement;
    }

    public CrisisDecision statement(String statement) {
        this.setStatement(statement);
        return this;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public UUID getDecidedBy() {
        return this.decidedBy;
    }

    public CrisisDecision decidedBy(UUID decidedBy) {
        this.setDecidedBy(decidedBy);
        return this;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public Instant getDecidedAt() {
        return this.decidedAt;
    }

    public CrisisDecision decidedAt(Instant decidedAt) {
        this.setDecidedAt(decidedAt);
        return this;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getTaskRef() {
        return this.taskRef;
    }

    public CrisisDecision taskRef(String taskRef) {
        this.setTaskRef(taskRef);
        return this;
    }

    public void setTaskRef(String taskRef) {
        this.taskRef = taskRef;
    }

    public CrisisCell getCell() {
        return this.cell;
    }

    public void setCell(CrisisCell crisisCell) {
        this.cell = crisisCell;
    }

    public CrisisDecision cell(CrisisCell crisisCell) {
        this.setCell(crisisCell);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrisisDecision)) {
            return false;
        }
        return getId() != null && getId().equals(((CrisisDecision) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CrisisDecision{" +
            "id=" + getId() +
            ", statement='" + getStatement() + "'" +
            ", decidedBy='" + getDecidedBy() + "'" +
            ", decidedAt='" + getDecidedAt() + "'" +
            ", taskRef='" + getTaskRef() + "'" +
            "}";
    }
}
