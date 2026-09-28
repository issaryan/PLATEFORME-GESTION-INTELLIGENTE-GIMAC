package cm.gimac.des.pilotage.service.dto;

import cm.gimac.des.pilotage.domain.enumeration.RiskZone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.Risk} entity.
 */
@Schema(description = "Risque coté sur la matrice 5x5 (algorithme 6.53).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RiskDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Périmètre : projet ou actif du département.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scope;

    @NotNull
    @Size(max = 2000)
    private String description;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Schema(description = "Probabilité P, de 1 à 5.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer probability;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Schema(description = "Impact I, de 1 à 5.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer impact;

    @Schema(description = "Contrôles en place : liste JSON {libellé, poids wk, efficacité ek}.")
    @Lob
    private String controls;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "0")
    @Schema(description = "Coefficient de maîtrise c, plafonné à 0,9 (formule 6.64).", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal controlCoefficient;

    @NotNull
    @Min(value = 1)
    @Max(value = 25)
    @Schema(description = "Risque brut Rb = P x I, de 1 à 25.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer grossScore;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "25")
    @Schema(description = "Risque résiduel Rr = Rb x (1 - c).", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal residualScore;

    @NotNull
    private RiskZone zone;

    @NotNull
    @Schema(description = "Propriétaire du risque (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID ownerId;

    @Size(max = 4000)
    private String mitigationPlan;

    private Instant reviewedAt;

    private ProjectDTO project;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getControls() {
        return controls;
    }

    public void setControls(String controls) {
        this.controls = controls;
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

    public RiskZone getZone() {
        return zone;
    }

    public void setZone(RiskZone zone) {
        this.zone = zone;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getMitigationPlan() {
        return mitigationPlan;
    }

    public void setMitigationPlan(String mitigationPlan) {
        this.mitigationPlan = mitigationPlan;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public ProjectDTO getProject() {
        return project;
    }

    public void setProject(ProjectDTO project) {
        this.project = project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDTO)) {
            return false;
        }

        RiskDTO riskDTO = (RiskDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, riskDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RiskDTO{" +
            "id='" + getId() + "'" +
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
            ", project=" + getProject() +
            "}";
    }
}
