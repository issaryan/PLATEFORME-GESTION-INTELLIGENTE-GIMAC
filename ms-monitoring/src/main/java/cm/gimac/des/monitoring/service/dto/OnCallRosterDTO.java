package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import cm.gimac.des.monitoring.domain.enumeration.SolverStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.OnCallRoster} entity.
 */
@Schema(
    description = "Grille d'astreinte hebdomadaire, une ligne par révision (algorithme 6.45).\nUnicité (semaine, révision) posée en base."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OnCallRosterDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 8)
    @Pattern(regexp = "^[0-9]{4}-S[0-9]{2}$")
    @Schema(description = "Semaine ISO, par exemple 2026-S39.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String isoWeek;

    @NotNull
    @Min(value = 1)
    private Integer revision;

    @NotNull
    private RosterState state;

    @NotNull
    private SolverStatus solverStatus;

    private BigDecimal objectiveValue;

    @DecimalMin(value = "0")
    @Schema(description = "Écart d'optimalité en pourcentage (par exemple 0,8).")
    private BigDecimal gap;

    private Duration solveDuration;

    @Schema(description = "Plages non couvertes et contraintes saturées qui l'expliquent, en JSON.")
    @Lob
    private String uncoveredSlots;

    @NotNull
    private Instant generatedAt;

    @NotNull
    private UUID generatedBy;

    private UUID validatedBy;

    private Instant validatedAt;

    private Instant publishedAt;

    @Schema(description = "Révision précédente de la même semaine.")
    private OnCallRosterDTO previousRevision;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIsoWeek() {
        return isoWeek;
    }

    public void setIsoWeek(String isoWeek) {
        this.isoWeek = isoWeek;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public RosterState getState() {
        return state;
    }

    public void setState(RosterState state) {
        this.state = state;
    }

    public SolverStatus getSolverStatus() {
        return solverStatus;
    }

    public void setSolverStatus(SolverStatus solverStatus) {
        this.solverStatus = solverStatus;
    }

    public BigDecimal getObjectiveValue() {
        return objectiveValue;
    }

    public void setObjectiveValue(BigDecimal objectiveValue) {
        this.objectiveValue = objectiveValue;
    }

    public BigDecimal getGap() {
        return gap;
    }

    public void setGap(BigDecimal gap) {
        this.gap = gap;
    }

    public Duration getSolveDuration() {
        return solveDuration;
    }

    public void setSolveDuration(Duration solveDuration) {
        this.solveDuration = solveDuration;
    }

    public String getUncoveredSlots() {
        return uncoveredSlots;
    }

    public void setUncoveredSlots(String uncoveredSlots) {
        this.uncoveredSlots = uncoveredSlots;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public UUID getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(UUID generatedBy) {
        this.generatedBy = generatedBy;
    }

    public UUID getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(UUID validatedBy) {
        this.validatedBy = validatedBy;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OnCallRosterDTO getPreviousRevision() {
        return previousRevision;
    }

    public void setPreviousRevision(OnCallRosterDTO previousRevision) {
        this.previousRevision = previousRevision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OnCallRosterDTO)) {
            return false;
        }

        OnCallRosterDTO onCallRosterDTO = (OnCallRosterDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, onCallRosterDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OnCallRosterDTO{" +
            "id='" + getId() + "'" +
            ", isoWeek='" + getIsoWeek() + "'" +
            ", revision=" + getRevision() +
            ", state='" + getState() + "'" +
            ", solverStatus='" + getSolverStatus() + "'" +
            ", objectiveValue=" + getObjectiveValue() +
            ", gap=" + getGap() +
            ", solveDuration='" + getSolveDuration() + "'" +
            ", uncoveredSlots='" + getUncoveredSlots() + "'" +
            ", generatedAt='" + getGeneratedAt() + "'" +
            ", generatedBy='" + getGeneratedBy() + "'" +
            ", validatedBy='" + getValidatedBy() + "'" +
            ", validatedAt='" + getValidatedAt() + "'" +
            ", publishedAt='" + getPublishedAt() + "'" +
            ", previousRevision=" + getPreviousRevision() +
            "}";
    }
}
