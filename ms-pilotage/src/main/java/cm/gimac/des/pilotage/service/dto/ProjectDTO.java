package cm.gimac.des.pilotage.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.Project} entity.
 */
@Schema(description = "Projet structurant du groupement, ordonnancé par PERT/CPM (algorithme 6.52).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProjectDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String scope;

    @NotNull
    @Schema(description = "Responsable du projet (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID ownerId;

    @DecimalMin(value = "0")
    private BigDecimal budget;

    @NotNull
    @Min(value = 0)
    @Max(value = 100)
    @Schema(
        description = "Avancement pondéré par les durées espérées des jalons atteints, en %.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer progress;

    @NotNull
    @Schema(
        description = "Date de début D0. Écart CC corrigé : entrée de l'algorithme 6.52 absente du diagramme de classes.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate startDate;

    @NotNull
    @Schema(description = "Date cible Dc.", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate targetDate;

    @DecimalMin(value = "0")
    @Schema(description = "Durée espérée du projet T, en jours ouvrés (sortie de l'algorithme 6.52).")
    private BigDecimal expectedDurationDays;

    @DecimalMin(value = "0")
    @DecimalMax(value = "1")
    @Schema(description = "Probabilité Pr(T <= Dc), entre 0 et 1 (formule 6.63).")
    private BigDecimal onTimeProbability;

    @Schema(description = "Chemin critique : liste JSON ordonnée des identifiants de jalons.")
    @Lob
    private String criticalPath;

    private Instant scheduledAt;

    @Schema(description = "Dépendances entre projets structurants.")
    private Set<ProjectDTO> dependencies = new HashSet<>();

    private Set<ProjectDTO> dependents = new HashSet<>();

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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public BigDecimal getExpectedDurationDays() {
        return expectedDurationDays;
    }

    public void setExpectedDurationDays(BigDecimal expectedDurationDays) {
        this.expectedDurationDays = expectedDurationDays;
    }

    public BigDecimal getOnTimeProbability() {
        return onTimeProbability;
    }

    public void setOnTimeProbability(BigDecimal onTimeProbability) {
        this.onTimeProbability = onTimeProbability;
    }

    public String getCriticalPath() {
        return criticalPath;
    }

    public void setCriticalPath(String criticalPath) {
        this.criticalPath = criticalPath;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Set<ProjectDTO> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<ProjectDTO> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<ProjectDTO> getDependents() {
        return dependents;
    }

    public void setDependents(Set<ProjectDTO> dependents) {
        this.dependents = dependents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectDTO)) {
            return false;
        }

        ProjectDTO projectDTO = (ProjectDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, projectDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProjectDTO{" +
            "id='" + getId() + "'" +
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
            ", dependencies=" + getDependencies() +
            ", dependents=" + getDependents() +
            "}";
    }
}
