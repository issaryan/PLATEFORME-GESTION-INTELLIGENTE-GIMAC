package cm.gimac.des.homologation.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.Coverage} entity.
 */
@Schema(
    description = "Couvertures d'un service (algorithme 6.33) : γ (certification) et µ (mise en\nservice), entre 0 et 1. Seules les opérations closes selon la règle des\nquatre éléments comptent ; aucune pondération manuelle. Seuil d'aptitude\nτseuil = 1, non paramétrable (D-18)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CoverageDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    private String serviceCode;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "1")
    private BigDecimal certificationRate;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "1")
    private BigDecimal commissioningRate;

    @NotNull
    @Min(value = 0)
    private Integer requiredCount;

    @NotNull
    @Min(value = 0)
    private Integer closedCount;

    @NotNull
    private Instant computedAt;

    @NotNull
    private LiaisonDTO liaison;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public BigDecimal getCertificationRate() {
        return certificationRate;
    }

    public void setCertificationRate(BigDecimal certificationRate) {
        this.certificationRate = certificationRate;
    }

    public BigDecimal getCommissioningRate() {
        return commissioningRate;
    }

    public void setCommissioningRate(BigDecimal commissioningRate) {
        this.commissioningRate = commissioningRate;
    }

    public Integer getRequiredCount() {
        return requiredCount;
    }

    public void setRequiredCount(Integer requiredCount) {
        this.requiredCount = requiredCount;
    }

    public Integer getClosedCount() {
        return closedCount;
    }

    public void setClosedCount(Integer closedCount) {
        this.closedCount = closedCount;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoverageDTO)) {
            return false;
        }

        CoverageDTO coverageDTO = (CoverageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, coverageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CoverageDTO{" +
            "id='" + getId() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", certificationRate=" + getCertificationRate() +
            ", commissioningRate=" + getCommissioningRate() +
            ", requiredCount=" + getRequiredCount() +
            ", closedCount=" + getClosedCount() +
            ", computedAt='" + getComputedAt() + "'" +
            ", liaison=" + getLiaison() +
            "}";
    }
}
