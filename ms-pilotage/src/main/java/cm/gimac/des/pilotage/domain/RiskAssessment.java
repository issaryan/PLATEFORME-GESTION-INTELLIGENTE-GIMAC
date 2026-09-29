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
 * Cotation datée d'un risque (algorithme 6.53, étape 2), historisée sans
 * écrasement. Écart CC corrigé : l'historique est implicite dans le CC.
 */
@Entity
@Table(name = "risk_assessment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RiskAssessment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Column(name = "probability", nullable = false)
    private Integer probability;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Column(name = "impact", nullable = false)
    private Integer impact;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "0.9") // JDL max(0.9), tronqué à "0" par le générateur
    @Column(name = "control_coefficient", precision = 21, scale = 2, nullable = false)
    private BigDecimal controlCoefficient;

    @NotNull
    @Min(value = 1)
    @Max(value = 25)
    @Column(name = "gross_score", nullable = false)
    private Integer grossScore;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "25")
    @Column(name = "residual_score", precision = 21, scale = 2, nullable = false)
    private BigDecimal residualScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_zone")
    private RiskZone previousZone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false)
    private RiskZone zone;

    /**
     * Auteur de la cotation (MS-IAM).
     */
    @NotNull
    @Column(name = "assessor_id", nullable = false)
    private UUID assessorId;

    @NotNull
    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "project" }, allowSetters = true)
    private Risk risk;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RiskAssessment id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getProbability() {
        return this.probability;
    }

    public RiskAssessment probability(Integer probability) {
        this.setProbability(probability);
        return this;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer getImpact() {
        return this.impact;
    }

    public RiskAssessment impact(Integer impact) {
        this.setImpact(impact);
        return this;
    }

    public void setImpact(Integer impact) {
        this.impact = impact;
    }

    public BigDecimal getControlCoefficient() {
        return this.controlCoefficient;
    }

    public RiskAssessment controlCoefficient(BigDecimal controlCoefficient) {
        this.setControlCoefficient(controlCoefficient);
        return this;
    }

    public void setControlCoefficient(BigDecimal controlCoefficient) {
        this.controlCoefficient = controlCoefficient;
    }

    public Integer getGrossScore() {
        return this.grossScore;
    }

    public RiskAssessment grossScore(Integer grossScore) {
        this.setGrossScore(grossScore);
        return this;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public BigDecimal getResidualScore() {
        return this.residualScore;
    }

    public RiskAssessment residualScore(BigDecimal residualScore) {
        this.setResidualScore(residualScore);
        return this;
    }

    public void setResidualScore(BigDecimal residualScore) {
        this.residualScore = residualScore;
    }

    public RiskZone getPreviousZone() {
        return this.previousZone;
    }

    public RiskAssessment previousZone(RiskZone previousZone) {
        this.setPreviousZone(previousZone);
        return this;
    }

    public void setPreviousZone(RiskZone previousZone) {
        this.previousZone = previousZone;
    }

    public RiskZone getZone() {
        return this.zone;
    }

    public RiskAssessment zone(RiskZone zone) {
        this.setZone(zone);
        return this;
    }

    public void setZone(RiskZone zone) {
        this.zone = zone;
    }

    public UUID getAssessorId() {
        return this.assessorId;
    }

    public RiskAssessment assessorId(UUID assessorId) {
        this.setAssessorId(assessorId);
        return this;
    }

    public void setAssessorId(UUID assessorId) {
        this.assessorId = assessorId;
    }

    public Instant getAssessedAt() {
        return this.assessedAt;
    }

    public RiskAssessment assessedAt(Instant assessedAt) {
        this.setAssessedAt(assessedAt);
        return this;
    }

    public void setAssessedAt(Instant assessedAt) {
        this.assessedAt = assessedAt;
    }

    public Risk getRisk() {
        return this.risk;
    }

    public void setRisk(Risk risk) {
        this.risk = risk;
    }

    public RiskAssessment risk(Risk risk) {
        this.setRisk(risk);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskAssessment)) {
            return false;
        }
        return getId() != null && getId().equals(((RiskAssessment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RiskAssessment{" +
            "id=" + getId() +
            ", probability=" + getProbability() +
            ", impact=" + getImpact() +
            ", controlCoefficient=" + getControlCoefficient() +
            ", grossScore=" + getGrossScore() +
            ", residualScore=" + getResidualScore() +
            ", previousZone='" + getPreviousZone() + "'" +
            ", zone='" + getZone() + "'" +
            ", assessorId='" + getAssessorId() + "'" +
            ", assessedAt='" + getAssessedAt() + "'" +
            "}";
    }
}
