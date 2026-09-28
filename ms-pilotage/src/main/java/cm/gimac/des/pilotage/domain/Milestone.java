package cm.gimac.des.pilotage.domain;

import cm.gimac.des.pilotage.domain.enumeration.MilestoneState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Jalon d'un projet, estimé par trois durées (formule 6.61).
 */
@Entity
@Table(name = "milestone")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Milestone implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 255)
    @Column(name = "label", length = 255, nullable = false)
    private String label;

    /**
     * Durée optimiste o.
     */
    @NotNull
    @Column(name = "optimistic", nullable = false)
    private Duration optimistic;

    /**
     * Durée la plus probable m.
     */
    @NotNull
    @Column(name = "most_likely", nullable = false)
    private Duration mostLikely;

    /**
     * Durée pessimiste p (o <= m <= p).
     */
    @NotNull
    @Column(name = "pessimistic", nullable = false)
    private Duration pessimistic;

    /**
     * Durée espérée t = (o + 4m + p) / 6, ou durée réelle si le jalon est atteint.
     */
    @Column(name = "expected_duration")
    private Duration expectedDuration;

    /**
     * Variance ((p - o) / 6)^2 en jours carrés ; nulle si le jalon est atteint.
     */
    @DecimalMin(value = "0")
    @Column(name = "variance", precision = 21, scale = 2)
    private BigDecimal variance;

    @Column(name = "earliest_start")
    private LocalDate earliestStart;

    @Column(name = "latest_start")
    private LocalDate latestStart;

    @Column(name = "total_float")
    private Duration totalFloat;

    @Column(name = "free_float")
    private Duration freeFloat;

    /**
     * Marge totale nulle à une demi-journée près.
     */
    @NotNull
    @Column(name = "critical", nullable = false)
    private Boolean critical;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private MilestoneState state;

    /**
     * Durée réelle, qui remplace l'estimation une fois le jalon atteint.
     */
    @Column(name = "actual_duration")
    private Duration actualDuration;

    @Column(name = "reached_at")
    private Instant reachedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "dependencies", "dependents" }, allowSetters = true)
    private Project project;

    /**
     * Dépendances du graphe des jalons, acyclique (tri de Kahn).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_milestone__predecessors",
        joinColumns = @JoinColumn(name = "milestone_id"),
        inverseJoinColumns = @JoinColumn(name = "predecessors_id")
    )
    @JsonIgnoreProperties(value = { "project", "predecessors", "successors" }, allowSetters = true)
    private Set<Milestone> predecessors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "predecessors")
    @JsonIgnoreProperties(value = { "project", "predecessors", "successors" }, allowSetters = true)
    private Set<Milestone> successors = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Milestone id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public Milestone label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Duration getOptimistic() {
        return this.optimistic;
    }

    public Milestone optimistic(Duration optimistic) {
        this.setOptimistic(optimistic);
        return this;
    }

    public void setOptimistic(Duration optimistic) {
        this.optimistic = optimistic;
    }

    public Duration getMostLikely() {
        return this.mostLikely;
    }

    public Milestone mostLikely(Duration mostLikely) {
        this.setMostLikely(mostLikely);
        return this;
    }

    public void setMostLikely(Duration mostLikely) {
        this.mostLikely = mostLikely;
    }

    public Duration getPessimistic() {
        return this.pessimistic;
    }

    public Milestone pessimistic(Duration pessimistic) {
        this.setPessimistic(pessimistic);
        return this;
    }

    public void setPessimistic(Duration pessimistic) {
        this.pessimistic = pessimistic;
    }

    public Duration getExpectedDuration() {
        return this.expectedDuration;
    }

    public Milestone expectedDuration(Duration expectedDuration) {
        this.setExpectedDuration(expectedDuration);
        return this;
    }

    public void setExpectedDuration(Duration expectedDuration) {
        this.expectedDuration = expectedDuration;
    }

    public BigDecimal getVariance() {
        return this.variance;
    }

    public Milestone variance(BigDecimal variance) {
        this.setVariance(variance);
        return this;
    }

    public void setVariance(BigDecimal variance) {
        this.variance = variance;
    }

    public LocalDate getEarliestStart() {
        return this.earliestStart;
    }

    public Milestone earliestStart(LocalDate earliestStart) {
        this.setEarliestStart(earliestStart);
        return this;
    }

    public void setEarliestStart(LocalDate earliestStart) {
        this.earliestStart = earliestStart;
    }

    public LocalDate getLatestStart() {
        return this.latestStart;
    }

    public Milestone latestStart(LocalDate latestStart) {
        this.setLatestStart(latestStart);
        return this;
    }

    public void setLatestStart(LocalDate latestStart) {
        this.latestStart = latestStart;
    }

    public Duration getTotalFloat() {
        return this.totalFloat;
    }

    public Milestone totalFloat(Duration totalFloat) {
        this.setTotalFloat(totalFloat);
        return this;
    }

    public void setTotalFloat(Duration totalFloat) {
        this.totalFloat = totalFloat;
    }

    public Duration getFreeFloat() {
        return this.freeFloat;
    }

    public Milestone freeFloat(Duration freeFloat) {
        this.setFreeFloat(freeFloat);
        return this;
    }

    public void setFreeFloat(Duration freeFloat) {
        this.freeFloat = freeFloat;
    }

    public Boolean getCritical() {
        return this.critical;
    }

    public Milestone critical(Boolean critical) {
        this.setCritical(critical);
        return this;
    }

    public void setCritical(Boolean critical) {
        this.critical = critical;
    }

    public MilestoneState getState() {
        return this.state;
    }

    public Milestone state(MilestoneState state) {
        this.setState(state);
        return this;
    }

    public void setState(MilestoneState state) {
        this.state = state;
    }

    public Duration getActualDuration() {
        return this.actualDuration;
    }

    public Milestone actualDuration(Duration actualDuration) {
        this.setActualDuration(actualDuration);
        return this;
    }

    public void setActualDuration(Duration actualDuration) {
        this.actualDuration = actualDuration;
    }

    public Instant getReachedAt() {
        return this.reachedAt;
    }

    public Milestone reachedAt(Instant reachedAt) {
        this.setReachedAt(reachedAt);
        return this;
    }

    public void setReachedAt(Instant reachedAt) {
        this.reachedAt = reachedAt;
    }

    public Project getProject() {
        return this.project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Milestone project(Project project) {
        this.setProject(project);
        return this;
    }

    public Set<Milestone> getPredecessors() {
        return this.predecessors;
    }

    public void setPredecessors(Set<Milestone> milestones) {
        this.predecessors = milestones;
    }

    public Milestone predecessors(Set<Milestone> milestones) {
        this.setPredecessors(milestones);
        return this;
    }

    public Milestone addPredecessors(Milestone milestone) {
        this.predecessors.add(milestone);
        return this;
    }

    public Milestone removePredecessors(Milestone milestone) {
        this.predecessors.remove(milestone);
        return this;
    }

    public Set<Milestone> getSuccessors() {
        return this.successors;
    }

    public void setSuccessors(Set<Milestone> milestones) {
        if (this.successors != null) {
            this.successors.forEach(i -> i.removePredecessors(this));
        }
        if (milestones != null) {
            milestones.forEach(i -> i.addPredecessors(this));
        }
        this.successors = milestones;
    }

    public Milestone successors(Set<Milestone> milestones) {
        this.setSuccessors(milestones);
        return this;
    }

    public Milestone addSuccessors(Milestone milestone) {
        this.successors.add(milestone);
        milestone.getPredecessors().add(this);
        return this;
    }

    public Milestone removeSuccessors(Milestone milestone) {
        this.successors.remove(milestone);
        milestone.getPredecessors().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Milestone)) {
            return false;
        }
        return getId() != null && getId().equals(((Milestone) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Milestone{" +
            "id=" + getId() +
            ", label='" + getLabel() + "'" +
            ", optimistic='" + getOptimistic() + "'" +
            ", mostLikely='" + getMostLikely() + "'" +
            ", pessimistic='" + getPessimistic() + "'" +
            ", expectedDuration='" + getExpectedDuration() + "'" +
            ", variance=" + getVariance() +
            ", earliestStart='" + getEarliestStart() + "'" +
            ", latestStart='" + getLatestStart() + "'" +
            ", totalFloat='" + getTotalFloat() + "'" +
            ", freeFloat='" + getFreeFloat() + "'" +
            ", critical='" + getCritical() + "'" +
            ", state='" + getState() + "'" +
            ", actualDuration='" + getActualDuration() + "'" +
            ", reachedAt='" + getReachedAt() + "'" +
            "}";
    }
}
