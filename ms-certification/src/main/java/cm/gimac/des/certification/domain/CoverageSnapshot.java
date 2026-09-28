package cm.gimac.des.certification.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Projection de la dernière couverture reçue par service (algorithme 6.36,
 * étape 1), depuis homologation.liaison-ready-for-certification.v1 et
 * homologation.coverage-updated.v1 ; un événement plus ancien est ignoré.
 * Clé fonctionnelle (liaisonId, serviceCode), unicité posée par Liquibase.
 */
@Entity
@Table(name = "coverage_snapshot")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CoverageSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Liaison tenue par MS-HOMOLOGATION (référence, BNF-04).
     */
    @NotNull
    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Column(name = "service_code", length = 80, nullable = false)
    private String serviceCode;

    @NotNull
    @Column(name = "included", nullable = false)
    private Boolean included;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "1")
    @Column(name = "certification_rate", precision = 21, scale = 2, nullable = false)
    private BigDecimal certificationRate;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "1")
    @Column(name = "commissioning_rate", precision = 21, scale = 2, nullable = false)
    private BigDecimal commissioningRate;

    @NotNull
    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    /**
     * Date de calcul portée par l'événement, pour ignorer un événement plus ancien.
     */
    @NotNull
    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @NotNull
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CoverageSnapshot id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLiaisonId() {
        return this.liaisonId;
    }

    public CoverageSnapshot liaisonId(UUID liaisonId) {
        this.setLiaisonId(liaisonId);
        return this;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public CoverageSnapshot serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Boolean getIncluded() {
        return this.included;
    }

    public CoverageSnapshot included(Boolean included) {
        this.setIncluded(included);
        return this;
    }

    public void setIncluded(Boolean included) {
        this.included = included;
    }

    public BigDecimal getCertificationRate() {
        return this.certificationRate;
    }

    public CoverageSnapshot certificationRate(BigDecimal certificationRate) {
        this.setCertificationRate(certificationRate);
        return this;
    }

    public void setCertificationRate(BigDecimal certificationRate) {
        this.certificationRate = certificationRate;
    }

    public BigDecimal getCommissioningRate() {
        return this.commissioningRate;
    }

    public CoverageSnapshot commissioningRate(BigDecimal commissioningRate) {
        this.setCommissioningRate(commissioningRate);
        return this;
    }

    public void setCommissioningRate(BigDecimal commissioningRate) {
        this.commissioningRate = commissioningRate;
    }

    public UUID getSourceEventId() {
        return this.sourceEventId;
    }

    public CoverageSnapshot sourceEventId(UUID sourceEventId) {
        this.setSourceEventId(sourceEventId);
        return this;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Instant getComputedAt() {
        return this.computedAt;
    }

    public CoverageSnapshot computedAt(Instant computedAt) {
        this.setComputedAt(computedAt);
        return this;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public Instant getReceivedAt() {
        return this.receivedAt;
    }

    public CoverageSnapshot receivedAt(Instant receivedAt) {
        this.setReceivedAt(receivedAt);
        return this;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoverageSnapshot)) {
            return false;
        }
        return getId() != null && getId().equals(((CoverageSnapshot) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CoverageSnapshot{" +
            "id=" + getId() +
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
