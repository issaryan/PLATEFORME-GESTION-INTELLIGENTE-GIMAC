package cm.gimac.des.certification.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.CoverageSnapshot} entity.
 */
@Schema(
    description = "Projection de la dernière couverture reçue par service (algorithme 6.36,\nétape 1), depuis homologation.liaison-ready-for-certification.v1 et\nhomologation.coverage-updated.v1 ; un événement plus ancien est ignoré.\nClé fonctionnelle (liaisonId, serviceCode), unicité posée par Liquibase."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CoverageSnapshotDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Liaison tenue par MS-HOMOLOGATION (référence, BNF-04).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID liaisonId;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    private String serviceCode;

    @NotNull
    private Boolean included;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "1")
    private BigDecimal certificationRate;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "1")
    private BigDecimal commissioningRate;

    @NotNull
    private UUID sourceEventId;

    @NotNull
    @Schema(
        description = "Date de calcul portée par l'événement, pour ignorer un événement plus ancien.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant computedAt;

    @NotNull
    private Instant receivedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Boolean getIncluded() {
        return included;
    }

    public void setIncluded(Boolean included) {
        this.included = included;
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

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoverageSnapshotDTO)) {
            return false;
        }

        CoverageSnapshotDTO coverageSnapshotDTO = (CoverageSnapshotDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, coverageSnapshotDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CoverageSnapshotDTO{" +
            "id='" + getId() + "'" +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", included='" + getIncluded() + "'" +
            ", certificationRate=" + getCertificationRate() +
            ", commissioningRate=" + getCommissioningRate() +
            ", sourceEventId='" + getSourceEventId() + "'" +
            ", computedAt='" + getComputedAt() + "'" +
            ", receivedAt='" + getReceivedAt() + "'" +
            "}";
    }
}
