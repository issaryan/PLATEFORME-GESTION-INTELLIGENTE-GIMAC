package cm.gimac.des.pilotage.domain;

import cm.gimac.des.pilotage.domain.enumeration.RiskZone;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Risque coté sur la matrice 5x5 (algorithme 6.53).
 */
@Entity
@Table(name = "risk")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Risk implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Périmètre : projet ou actif du département.
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "scope", length = 255, nullable = false)
    private String scope;

    @NotNull
    @Size(max = 2000)
    @Column(name = "description", length = 2000, nullable = false)
    private String description;

    /**
     * Probabilité P, de 1 à 5.
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Column(name = "probability", nullable = false)
    private Integer probability;

    /**
     * Impact I, de 1 à 5.
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Column(name = "impact", nullable = false)
    private Integer impact;

    /**
     * Contrôles en place : liste JSON {libellé, poids wk, efficacité ek}.
     */
    @Lob
    @Column(name = "controls")
    private String controls;

    /**
     * Coefficient de maîtrise c, plafonné à 0,9 (formule 6.64).
     */
    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "0.9") // JDL max(0.9), tronqué à "0" par le générateur
    @Column(name = "control_coefficient", precision = 21, scale = 2, nullable = false)
    private BigDecimal controlCoefficient;

    /**
     * Risque brut Rb = P x I, de 1 à 25.
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 25)
    @Column(name = "gross_score", nullable = false)
    private Integer grossScore;

    /**
     * Risque résiduel Rr = Rb x (1 - c).
     */
    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "25")
    @Column(name = "residual_score", precision = 21, scale = 2, nullable = false)
    private BigDecimal residualScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false)
    private RiskZone zone;

    /**
     * Propriétaire du risque (MS-IAM).
     */
    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Size(max = 4000)
    @Column(name = "mitigation_plan", length = 4000)
    private String mitigationPlan;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "dependencies", "dependents" }, allowSetters = true)
    private Project project;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Risk id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getScope() {
        return this.scope;
    }

    public Risk scope(String scope) {
        this.setScope(scope);
        return this;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDescription() {
        return this.description;
    }

    public Risk description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getProbability() {
        return this.probability;
    }

    public Risk probability(Integer probability) {
        this.setProbability(probability);
        return this;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer getImpact() {
        return this.impact;
    }

    public Risk impact(Integer impact) {
        this.setImpact(impact);
        return this;
    }

    public void setImpact(Integer impact) {
        this.impact = impact;
    }

    public String getControls() {
        return this.controls;
    }

    public Risk controls(String controls) {
        this.setControls(controls);
        return this;
    }

    public void setControls(String controls) {
        this.controls = controls;
    }

    public BigDecimal getControlCoefficient() {
        return this.controlCoefficient;
    }

    public Risk controlCoefficient(BigDecimal controlCoefficient) {
        this.setControlCoefficient(controlCoefficient);
        return this;
    }

    public void setControlCoefficient(BigDecimal controlCoefficient) {
        this.controlCoefficient = controlCoefficient;
    }

    public Integer getGrossScore() {
        return this.grossScore;
    }

    public Risk grossScore(Integer grossScore) {
        this.setGrossScore(grossScore);
        return this;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public BigDecimal getResidualScore() {
        return this.residualScore;
    }

    public Risk residualScore(BigDecimal residualScore) {
        this.setResidualScore(residualScore);
        return this;
    }

    public void setResidualScore(BigDecimal residualScore) {
        this.residualScore = residualScore;
    }

    public RiskZone getZone() {
        return this.zone;
    }

    public Risk zone(RiskZone zone) {
        this.setZone(zone);
        return this;
    }

    public void setZone(RiskZone zone) {
        this.zone = zone;
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public Risk ownerId(UUID ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getMitigationPlan() {
        return this.mitigationPlan;
    }

    public Risk mitigationPlan(String mitigationPlan) {
        this.setMitigationPlan(mitigationPlan);
        return this;
    }

    public void setMitigationPlan(String mitigationPlan) {
        this.mitigationPlan = mitigationPlan;
    }

    public Instant getReviewedAt() {
        return this.reviewedAt;
    }

    public Risk reviewedAt(Instant reviewedAt) {
        this.setReviewedAt(reviewedAt);
        return this;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Project getProject() {
        return this.project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Risk project(Project project) {
        this.setProject(project);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Risk)) {
            return false;
        }
        return getId() != null && getId().equals(((Risk) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Risk{" +
            "id=" + getId() +
            ", scope='" + getScope() + "'" +
            ", description='" + getDescription() + "'" +
            ", probability=" + getProbability() +
            ", impact=" + getImpact() +
            ", controls='" + getControls() + "'" +
            ", controlCoefficient=" + getControlCoefficient() +
            ", grossScore=" + getGrossScore() +
            ", residualScore=" + getResidualScore() +
            ", zone='" + getZone() + "'" +
            ", ownerId='" + getOwnerId() + "'" +
            ", mitigationPlan='" + getMitigationPlan() + "'" +
            ", reviewedAt='" + getReviewedAt() + "'" +
            "}";
    }
}
