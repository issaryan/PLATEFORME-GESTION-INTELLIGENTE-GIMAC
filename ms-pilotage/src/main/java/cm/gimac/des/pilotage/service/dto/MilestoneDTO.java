package cm.gimac.des.pilotage.service.dto;

import cm.gimac.des.pilotage.domain.enumeration.MilestoneState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.Milestone} entity.
 */
@Schema(description = "Jalon d'un projet, estimé par trois durées (formule 6.61).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MilestoneDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 255)
    private String label;

    @NotNull
    @Schema(description = "Durée optimiste o.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration optimistic;

    @NotNull
    @Schema(description = "Durée la plus probable m.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration mostLikely;

    @NotNull
    @Schema(description = "Durée pessimiste p (o <= m <= p).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration pessimistic;

    @Schema(description = "Durée espérée t = (o + 4m + p) / 6, ou durée réelle si le jalon est atteint.")
    private Duration expectedDuration;

    @DecimalMin(value = "0")
    @Schema(description = "Variance ((p - o) / 6)^2 en jours carrés ; nulle si le jalon est atteint.")
    private BigDecimal variance;

    private LocalDate earliestStart;

    private LocalDate latestStart;

    private Duration totalFloat;

    private Duration freeFloat;

    @NotNull
    @Schema(description = "Marge totale nulle à une demi-journée près.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean critical;

    @NotNull
    private MilestoneState state;

    @Schema(description = "Durée réelle, qui remplace l'estimation une fois le jalon atteint.")
    private Duration actualDuration;

    private Instant reachedAt;

    @NotNull
    private ProjectDTO project;

    @Schema(description = "Dépendances du graphe des jalons, acyclique (tri de Kahn).")
    private Set<MilestoneDTO> predecessors = new HashSet<>();

    private Set<MilestoneDTO> successors = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Duration getOptimistic() {
        return optimistic;
    }

    public void setOptimistic(Duration optimistic) {
        this.optimistic = optimistic;
    }

    public Duration getMostLikely() {
        return mostLikely;
    }

    public void setMostLikely(Duration mostLikely) {
        this.mostLikely = mostLikely;
    }

    public Duration getPessimistic() {
        return pessimistic;
    }

    public void setPessimistic(Duration pessimistic) {
        this.pessimistic = pessimistic;
    }

    public Duration getExpectedDuration() {
        return expectedDuration;
    }

    public void setExpectedDuration(Duration expectedDuration) {
        this.expectedDuration = expectedDuration;
    }

    public BigDecimal getVariance() {
        return variance;
    }

    public void setVariance(BigDecimal variance) {
        this.variance = variance;
    }

    public LocalDate getEarliestStart() {
        return earliestStart;
    }

    public void setEarliestStart(LocalDate earliestStart) {
        this.earliestStart = earliestStart;
    }

    public LocalDate getLatestStart() {
        return latestStart;
    }

    public void setLatestStart(LocalDate latestStart) {
        this.latestStart = latestStart;
    }

    public Duration getTotalFloat() {
        return totalFloat;
    }

    public void setTotalFloat(Duration totalFloat) {
        this.totalFloat = totalFloat;
    }

    public Duration getFreeFloat() {
        return freeFloat;
    }

    public void setFreeFloat(Duration freeFloat) {
        this.freeFloat = freeFloat;
    }

    public Boolean getCritical() {
        return critical;
    }

    public void setCritical(Boolean critical) {
        this.critical = critical;
    }

    public MilestoneState getState() {
        return state;
    }

    public void setState(MilestoneState state) {
        this.state = state;
    }

    public Duration getActualDuration() {
        return actualDuration;
    }

    public void setActualDuration(Duration actualDuration) {
        this.actualDuration = actualDuration;
    }

    public Instant getReachedAt() {
        return reachedAt;
    }

    public void setReachedAt(Instant reachedAt) {
        this.reachedAt = reachedAt;
    }

    public ProjectDTO getProject() {
        return project;
    }

    public void setProject(ProjectDTO project) {
        this.project = project;
    }

    public Set<MilestoneDTO> getPredecessors() {
        return predecessors;
    }

    public void setPredecessors(Set<MilestoneDTO> predecessors) {
        this.predecessors = predecessors;
    }

    public Set<MilestoneDTO> getSuccessors() {
        return successors;
    }

    public void setSuccessors(Set<MilestoneDTO> successors) {
        this.successors = successors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MilestoneDTO)) {
            return false;
        }

        MilestoneDTO milestoneDTO = (MilestoneDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, milestoneDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MilestoneDTO{" +
            "id='" + getId() + "'" +
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
            ", project=" + getProject() +
            ", predecessors=" + getPredecessors() +
            ", successors=" + getSuccessors() +
            "}";
    }
}
