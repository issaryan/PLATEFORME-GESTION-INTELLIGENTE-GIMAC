package cm.gimac.des.homologation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Couvertures d'un service (algorithme 6.33) : γ (certification) et µ (mise en
 * service), entre 0 et 1. Seules les opérations closes selon la règle des
 * quatre éléments comptent ; aucune pondération manuelle. Seuil d'aptitude
 * τseuil = 1, non paramétrable (D-18).
 */
@Entity
@Table(name = "coverage")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Coverage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Column(name = "service_code", length = 80, nullable = false)
    private String serviceCode;

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
    @Min(value = 0)
    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;

    @NotNull
    @Min(value = 0)
    @Column(name = "closed_count", nullable = false)
    private Integer closedCount;

    @NotNull
    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "catalog", "campaignSync" }, allowSetters = true)
    private Liaison liaison;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Coverage id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public Coverage serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public BigDecimal getCertificationRate() {
        return this.certificationRate;
    }

    public Coverage certificationRate(BigDecimal certificationRate) {
        this.setCertificationRate(certificationRate);
        return this;
    }

    public void setCertificationRate(BigDecimal certificationRate) {
        this.certificationRate = certificationRate;
    }

    public BigDecimal getCommissioningRate() {
        return this.commissioningRate;
    }

    public Coverage commissioningRate(BigDecimal commissioningRate) {
        this.setCommissioningRate(commissioningRate);
        return this;
    }

    public void setCommissioningRate(BigDecimal commissioningRate) {
        this.commissioningRate = commissioningRate;
    }

    public Integer getRequiredCount() {
        return this.requiredCount;
    }

    public Coverage requiredCount(Integer requiredCount) {
        this.setRequiredCount(requiredCount);
        return this;
    }

    public void setRequiredCount(Integer requiredCount) {
        this.requiredCount = requiredCount;
    }

    public Integer getClosedCount() {
        return this.closedCount;
    }

    public Coverage closedCount(Integer closedCount) {
        this.setClosedCount(closedCount);
        return this;
    }

    public void setClosedCount(Integer closedCount) {
        this.closedCount = closedCount;
    }

    public Instant getComputedAt() {
        return this.computedAt;
    }

    public Coverage computedAt(Instant computedAt) {
        this.setComputedAt(computedAt);
        return this;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public Coverage liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Coverage)) {
            return false;
        }
        return getId() != null && getId().equals(((Coverage) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Coverage{" +
            "id=" + getId() +
            ", serviceCode='" + getServiceCode() + "'" +
            ", certificationRate=" + getCertificationRate() +
            ", commissioningRate=" + getCommissioningRate() +
            ", requiredCount=" + getRequiredCount() +
            ", closedCount=" + getClosedCount() +
            ", computedAt='" + getComputedAt() + "'" +
            "}";
    }
}
