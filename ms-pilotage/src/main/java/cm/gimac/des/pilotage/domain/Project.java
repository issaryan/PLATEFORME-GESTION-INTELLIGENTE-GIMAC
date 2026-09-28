package cm.gimac.des.pilotage.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Projet structurant du groupement, ordonnancé par PERT/CPM (algorithme 6.52).
 */
@Entity
@Table(name = "project")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Project implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 255)
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Size(max = 2000)
    @Column(name = "scope", length = 2000)
    private String scope;

    /**
     * Responsable du projet (MS-IAM).
     */
    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @DecimalMin(value = "0")
    @Column(name = "budget", precision = 21, scale = 2)
    private BigDecimal budget;

    /**
     * Avancement pondéré par les durées espérées des jalons atteints, en %.
     */
    @NotNull
    @Min(value = 0)
    @Max(value = 100)
    @Column(name = "progress", nullable = false)
    private Integer progress;

    /**
     * Date de début D0. Écart CC corrigé : entrée de l'algorithme 6.52 absente du diagramme de classes.
     */
    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Date cible Dc.
     */
    @NotNull
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    /**
     * Durée espérée du projet T, en jours ouvrés (sortie de l'algorithme 6.52).
     */
    @DecimalMin(value = "0")
    @Column(name = "expected_duration_days", precision = 21, scale = 2)
    private BigDecimal expectedDurationDays;

    /**
     * Probabilité Pr(T <= Dc), entre 0 et 1 (formule 6.63).
     */
    @DecimalMin(value = "0")
    @DecimalMax(value = "1")
    @Column(name = "on_time_probability", precision = 21, scale = 2)
    private BigDecimal onTimeProbability;

    /**
     * Chemin critique : liste JSON ordonnée des identifiants de jalons.
     */
    @Lob
    @Column(name = "critical_path")
    private String criticalPath;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    /**
     * Dépendances entre projets structurants.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_project__dependencies",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "dependencies_id")
    )
    @JsonIgnoreProperties(value = { "dependencies", "dependents" }, allowSetters = true)
    private Set<Project> dependencies = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "dependencies")
    @JsonIgnoreProperties(value = { "dependencies", "dependents" }, allowSetters = true)
    private Set<Project> dependents = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Project id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Project title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getScope() {
        return this.scope;
    }

    public Project scope(String scope) {
        this.setScope(scope);
        return this;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public Project ownerId(UUID ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public BigDecimal getBudget() {
        return this.budget;
    }

    public Project budget(BigDecimal budget) {
        this.setBudget(budget);
        return this;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Integer getProgress() {
        return this.progress;
    }

    public Project progress(Integer progress) {
        this.setProgress(progress);
        return this;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public Project startDate(LocalDate startDate) {
        this.setStartDate(startDate);
        return this;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetDate() {
        return this.targetDate;
    }

    public Project targetDate(LocalDate targetDate) {
        this.setTargetDate(targetDate);
        return this;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public BigDecimal getExpectedDurationDays() {
        return this.expectedDurationDays;
    }

    public Project expectedDurationDays(BigDecimal expectedDurationDays) {
        this.setExpectedDurationDays(expectedDurationDays);
        return this;
    }

    public void setExpectedDurationDays(BigDecimal expectedDurationDays) {
        this.expectedDurationDays = expectedDurationDays;
    }

    public BigDecimal getOnTimeProbability() {
        return this.onTimeProbability;
    }

    public Project onTimeProbability(BigDecimal onTimeProbability) {
        this.setOnTimeProbability(onTimeProbability);
        return this;
    }

    public void setOnTimeProbability(BigDecimal onTimeProbability) {
        this.onTimeProbability = onTimeProbability;
    }

    public String getCriticalPath() {
        return this.criticalPath;
    }

    public Project criticalPath(String criticalPath) {
        this.setCriticalPath(criticalPath);
        return this;
    }

    public void setCriticalPath(String criticalPath) {
        this.criticalPath = criticalPath;
    }

    public Instant getScheduledAt() {
        return this.scheduledAt;
    }

    public Project scheduledAt(Instant scheduledAt) {
        this.setScheduledAt(scheduledAt);
        return this;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Set<Project> getDependencies() {
        return this.dependencies;
    }

    public void setDependencies(Set<Project> projects) {
        this.dependencies = projects;
    }

    public Project dependencies(Set<Project> projects) {
        this.setDependencies(projects);
        return this;
    }

    public Project addDependencies(Project project) {
        this.dependencies.add(project);
        return this;
    }

    public Project removeDependencies(Project project) {
        this.dependencies.remove(project);
        return this;
    }

    public Set<Project> getDependents() {
        return this.dependents;
    }

    public void setDependents(Set<Project> projects) {
        if (this.dependents != null) {
            this.dependents.forEach(i -> i.removeDependencies(this));
        }
        if (projects != null) {
            projects.forEach(i -> i.addDependencies(this));
        }
        this.dependents = projects;
    }

    public Project dependents(Set<Project> projects) {
        this.setDependents(projects);
        return this;
    }

    public Project addDependents(Project project) {
        this.dependents.add(project);
        project.getDependencies().add(this);
        return this;
    }

    public Project removeDependents(Project project) {
        this.dependents.remove(project);
        project.getDependencies().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Project)) {
            return false;
        }
        return getId() != null && getId().equals(((Project) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Project{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", scope='" + getScope() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", budget=" + getBudget() +
            ", progress=" + getProgress() +
            ", startDate='" + getStartDate() + "'" +
            ", targetDate='" + getTargetDate() + "'" +
            ", expectedDurationDays=" + getExpectedDurationDays() +
            ", onTimeProbability=" + getOnTimeProbability() +
            ", criticalPath='" + getCriticalPath() + "'" +
            ", scheduledAt='" + getScheduledAt() + "'" +
            "}";
    }
}
