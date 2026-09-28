package cm.gimac.des.pilotage.service.dto;

import cm.gimac.des.pilotage.domain.enumeration.RiskZone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.RiskAssessment} entity.
 */
@Schema(
    description = "Cotation datée d'un risque (algorithme 6.53, étape 2), historisée sans\nécrasement. Écart CC corrigé : l'historique est implicite dans le CC."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RiskAssessmentDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer probability;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer impact;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "0")
    private BigDecimal controlCoefficient;

    @NotNull
    @Min(value = 1)
    @Max(value = 25)
    private Integer grossScore;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "25")
    private BigDecimal residualScore;

    private RiskZone previousZone;

    @NotNull
    private RiskZone zone;

    @NotNull
    @Schema(description = "Auteur de la cotation (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID assessorId;

    @NotNull
    private Instant assessedAt;

    @NotNull
    private RiskDTO risk;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer getImpact() {
        return impact;
    }

    public void setImpact(Integer impact) {
        this.impact = impact;
    }

    public BigDecimal getControlCoefficient() {
        return controlCoefficient;
    }

    public void setControlCoefficient(BigDecimal controlCoefficient) {
        this.controlCoefficient = controlCoefficient;
    }

    public Integer getGrossScore() {
        return grossScore;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public BigDecimal getResidualScore() {
        return residualScore;
    }

    public void setResidualScore(BigDecimal residualScore) {
        this.residualScore = residualScore;
    }

    public RiskZone getPreviousZone() {
        return previousZone;
    }

    public void setPreviousZone(RiskZone previousZone) {
        this.previousZone = previousZone;
    }

    public RiskZone getZone() {
        return zone;
    }

    public void setZone(RiskZone zone) {
        this.zone = zone;
    }

    public UUID getAssessorId() {
        return assessorId;
    }

    public void setAssessorId(UUID assessorId) {
        this.assessorId = assessorId;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(Instant assessedAt) {
        this.assessedAt = assessedAt;
    }

    public RiskDTO getRisk() {
        return risk;
    }

    public void setRisk(RiskDTO risk) {
        this.risk = risk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskAssessmentDTO)) {
            return false;
        }

        RiskAssessmentDTO riskAssessmentDTO = (RiskAssessmentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, riskAssessmentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RiskAssessmentDTO{" +
            "id='" + getId() + "'" +
            ", probability=" + getProbability() +
            ", impact=" + getImpact() +
            ", controlCoefficient=" + getControlCoefficient() +
            ", grossScore=" + getGrossScore() +
            ", residualScore=" + getResidualScore() +
            ", previousZone='" + getPreviousZone() + "'" +
            ", zone='" + getZone() + "'" +
            ", assessorId='" + getAssessorId() + "'" +
            ", assessedAt='" + getAssessedAt() + "'" +
            ", risk=" + getRisk() +
            "}";
    }
}
