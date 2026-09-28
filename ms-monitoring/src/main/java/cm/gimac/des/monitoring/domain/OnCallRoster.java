package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import cm.gimac.des.monitoring.domain.enumeration.SolverStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Grille d'astreinte hebdomadaire, une ligne par révision (algorithme 6.45).
 * Unicité (semaine, révision) posée en base.
 */
@Entity
@Table(name = "on_call_roster")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OnCallRoster implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Semaine ISO, par exemple 2026-S39.
     */
    @NotNull
    @Size(max = 8)
    @Pattern(regexp = "^[0-9]{4}-S[0-9]{2}$")
    @Column(name = "iso_week", length = 8, nullable = false)
    private String isoWeek;

    @NotNull
    @Min(value = 1)
    @Column(name = "revision", nullable = false)
    private Integer revision;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private RosterState state;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "solver_status", nullable = false)
    private SolverStatus solverStatus;

    @Column(name = "objective_value", precision = 21, scale = 2)
    private BigDecimal objectiveValue;

    /**
     * Écart d'optimalité en pourcentage (par exemple 0,8).
     */
    @DecimalMin(value = "0")
    @Column(name = "gap", precision = 21, scale = 2)
    private BigDecimal gap;

    @Column(name = "solve_duration")
    private Duration solveDuration;

    /**
     * Plages non couvertes et contraintes saturées qui l'expliquent, en JSON.
     */
    @Lob
    @Column(name = "uncovered_slots")
    private String uncoveredSlots;

    @NotNull
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @NotNull
    @Column(name = "generated_by", nullable = false)
    private UUID generatedBy;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Révision précédente de la même semaine.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "previousRevision" }, allowSetters = true)
    private OnCallRoster previousRevision;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public OnCallRoster id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIsoWeek() {
        return this.isoWeek;
    }

    public OnCallRoster isoWeek(String isoWeek) {
        this.setIsoWeek(isoWeek);
        return this;
    }

    public void setIsoWeek(String isoWeek) {
        this.isoWeek = isoWeek;
    }

    public Integer getRevision() {
        return this.revision;
    }

    public OnCallRoster revision(Integer revision) {
        this.setRevision(revision);
        return this;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public RosterState getState() {
        return this.state;
    }

    public OnCallRoster state(RosterState state) {
        this.setState(state);
        return this;
    }

    public void setState(RosterState state) {
        this.state = state;
    }

    public SolverStatus getSolverStatus() {
        return this.solverStatus;
    }

    public OnCallRoster solverStatus(SolverStatus solverStatus) {
        this.setSolverStatus(solverStatus);
        return this;
    }

    public void setSolverStatus(SolverStatus solverStatus) {
        this.solverStatus = solverStatus;
    }

    public BigDecimal getObjectiveValue() {
        return this.objectiveValue;
    }

    public OnCallRoster objectiveValue(BigDecimal objectiveValue) {
        this.setObjectiveValue(objectiveValue);
        return this;
    }

    public void setObjectiveValue(BigDecimal objectiveValue) {
        this.objectiveValue = objectiveValue;
    }

    public BigDecimal getGap() {
        return this.gap;
    }

    public OnCallRoster gap(BigDecimal gap) {
        this.setGap(gap);
        return this;
    }

    public void setGap(BigDecimal gap) {
        this.gap = gap;
    }

    public Duration getSolveDuration() {
        return this.solveDuration;
    }

    public OnCallRoster solveDuration(Duration solveDuration) {
        this.setSolveDuration(solveDuration);
        return this;
    }

    public void setSolveDuration(Duration solveDuration) {
        this.solveDuration = solveDuration;
    }

    public String getUncoveredSlots() {
        return this.uncoveredSlots;
    }

    public OnCallRoster uncoveredSlots(String uncoveredSlots) {
        this.setUncoveredSlots(uncoveredSlots);
        return this;
    }

    public void setUncoveredSlots(String uncoveredSlots) {
        this.uncoveredSlots = uncoveredSlots;
    }

    public Instant getGeneratedAt() {
        return this.generatedAt;
    }

    public OnCallRoster generatedAt(Instant generatedAt) {
        this.setGeneratedAt(generatedAt);
        return this;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public UUID getGeneratedBy() {
        return this.generatedBy;
    }

    public OnCallRoster generatedBy(UUID generatedBy) {
        this.setGeneratedBy(generatedBy);
        return this;
    }

    public void setGeneratedBy(UUID generatedBy) {
        this.generatedBy = generatedBy;
    }

    public UUID getValidatedBy() {
        return this.validatedBy;
    }

    public OnCallRoster validatedBy(UUID validatedBy) {
        this.setValidatedBy(validatedBy);
        return this;
    }

    public void setValidatedBy(UUID validatedBy) {
        this.validatedBy = validatedBy;
    }

    public Instant getValidatedAt() {
        return this.validatedAt;
    }

    public OnCallRoster validatedAt(Instant validatedAt) {
        this.setValidatedAt(validatedAt);
        return this;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Instant getPublishedAt() {
        return this.publishedAt;
    }

    public OnCallRoster publishedAt(Instant publishedAt) {
        this.setPublishedAt(publishedAt);
        return this;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OnCallRoster getPreviousRevision() {
        return this.previousRevision;
    }

    public void setPreviousRevision(OnCallRoster onCallRoster) {
        this.previousRevision = onCallRoster;
    }

    public OnCallRoster previousRevision(OnCallRoster onCallRoster) {
        this.setPreviousRevision(onCallRoster);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OnCallRoster)) {
            return false;
        }
        return getId() != null && getId().equals(((OnCallRoster) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OnCallRoster{" +
            "id=" + getId() +
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
            "}";
    }
}
