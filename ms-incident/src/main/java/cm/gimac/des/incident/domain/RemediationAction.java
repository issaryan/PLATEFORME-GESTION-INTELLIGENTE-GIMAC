package cm.gimac.des.incident.domain;

import cm.gimac.des.incident.domain.enumeration.RemediationKind;
import cm.gimac.des.incident.domain.enumeration.RemediationState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Action de remédiation demandée à MS-PILOTAGE (incident.remediation-requested.v1),
 * dont l'état suit les événements de tâche (écart CC complété : entité exigée
 * par le critère « remédiation » de l'algorithme 6.43).
 */
@Entity
@Table(name = "remediation_action")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RemediationAction implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private RemediationKind kind;

    @NotNull
    @Size(max = 2000)
    @Column(name = "description", length = 2000, nullable = false)
    private String description;

    /**
     * Porteur nominatif de la tâche.
     */
    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    /**
     * Référence de la tâche rendue par MS-PILOTAGE, par exemple TSK-2026-0712.
     */
    @Size(max = 50)
    @Column(name = "task_ref", length = 50)
    private String taskRef;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private RemediationState state;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "ticket", "matrix" }, allowSetters = true)
    private Incident incident;

    /**
     * Décision de crise à l'origine de l'action, le cas échéant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "cell" }, allowSetters = true)
    private CrisisDecision crisisDecision;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RemediationAction id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RemediationKind getKind() {
        return this.kind;
    }

    public RemediationAction kind(RemediationKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(RemediationKind kind) {
        this.kind = kind;
    }

    public String getDescription() {
        return this.description;
    }

    public RemediationAction description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public RemediationAction ownerId(UUID ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public RemediationAction dueDate(LocalDate dueDate) {
        this.setDueDate(dueDate);
        return this;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getRequestedAt() {
        return this.requestedAt;
    }

    public RemediationAction requestedAt(Instant requestedAt) {
        this.setRequestedAt(requestedAt);
        return this;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getTaskRef() {
        return this.taskRef;
    }

    public RemediationAction taskRef(String taskRef) {
        this.setTaskRef(taskRef);
        return this;
    }

    public void setTaskRef(String taskRef) {
        this.taskRef = taskRef;
    }

    public RemediationState getState() {
        return this.state;
    }

    public RemediationAction state(RemediationState state) {
        this.setState(state);
        return this;
    }

    public void setState(RemediationState state) {
        this.state = state;
    }

    public Instant getCompletedAt() {
        return this.completedAt;
    }

    public RemediationAction completedAt(Instant completedAt) {
        this.setCompletedAt(completedAt);
        return this;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Incident getIncident() {
        return this.incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public RemediationAction incident(Incident incident) {
        this.setIncident(incident);
        return this;
    }

    public CrisisDecision getCrisisDecision() {
        return this.crisisDecision;
    }

    public void setCrisisDecision(CrisisDecision crisisDecision) {
        this.crisisDecision = crisisDecision;
    }

    public RemediationAction crisisDecision(CrisisDecision crisisDecision) {
        this.setCrisisDecision(crisisDecision);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemediationAction)) {
            return false;
        }
        return getId() != null && getId().equals(((RemediationAction) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RemediationAction{" +
            "id=" + getId() +
            ", kind='" + getKind() + "'" +
            ", description='" + getDescription() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", dueDate='" + getDueDate() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", taskRef='" + getTaskRef() + "'" +
            ", state='" + getState() + "'" +
            ", completedAt='" + getCompletedAt() + "'" +
            "}";
    }
}
