package cm.gimac.des.pilotage.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Critère d'acceptation d'une tâche ; au moins un par tâche (CC §6.1.12.1).
 */
@Entity
@Table(name = "acceptance_criterion")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AcceptanceCriterion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 500)
    @Column(name = "label", length = 500, nullable = false)
    private String label;

    @NotNull
    @Column(name = "satisfied", nullable = false)
    private Boolean satisfied;

    /**
     * Une pièce justificative en GED est exigée pour satisfaire ce critère.
     */
    @NotNull
    @Column(name = "evidence_required", nullable = false)
    private Boolean evidenceRequired;

    /**
     * Pièce justificative : identifiant du document dans MS-DOCUMENT.
     */
    @Column(name = "evidence_ref")
    private UUID evidenceRef;

    @Column(name = "satisfied_at")
    private Instant satisfiedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "board", "boardColumn", "actionSheet", "decision" }, allowSetters = true)
    private Task task;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AcceptanceCriterion id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public AcceptanceCriterion label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getSatisfied() {
        return this.satisfied;
    }

    public AcceptanceCriterion satisfied(Boolean satisfied) {
        this.setSatisfied(satisfied);
        return this;
    }

    public void setSatisfied(Boolean satisfied) {
        this.satisfied = satisfied;
    }

    public Boolean getEvidenceRequired() {
        return this.evidenceRequired;
    }

    public AcceptanceCriterion evidenceRequired(Boolean evidenceRequired) {
        this.setEvidenceRequired(evidenceRequired);
        return this;
    }

    public void setEvidenceRequired(Boolean evidenceRequired) {
        this.evidenceRequired = evidenceRequired;
    }

    public UUID getEvidenceRef() {
        return this.evidenceRef;
    }

    public AcceptanceCriterion evidenceRef(UUID evidenceRef) {
        this.setEvidenceRef(evidenceRef);
        return this;
    }

    public void setEvidenceRef(UUID evidenceRef) {
        this.evidenceRef = evidenceRef;
    }

    public Instant getSatisfiedAt() {
        return this.satisfiedAt;
    }

    public AcceptanceCriterion satisfiedAt(Instant satisfiedAt) {
        this.setSatisfiedAt(satisfiedAt);
        return this;
    }

    public void setSatisfiedAt(Instant satisfiedAt) {
        this.satisfiedAt = satisfiedAt;
    }

    public Task getTask() {
        return this.task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public AcceptanceCriterion task(Task task) {
        this.setTask(task);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AcceptanceCriterion)) {
            return false;
        }
        return getId() != null && getId().equals(((AcceptanceCriterion) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AcceptanceCriterion{" +
            "id=" + getId() +
            ", label='" + getLabel() + "'" +
            ", satisfied='" + getSatisfied() + "'" +
            ", evidenceRequired='" + getEvidenceRequired() + "'" +
            ", evidenceRef='" + getEvidenceRef() + "'" +
            ", satisfiedAt='" + getSatisfiedAt() + "'" +
            "}";
    }
}
