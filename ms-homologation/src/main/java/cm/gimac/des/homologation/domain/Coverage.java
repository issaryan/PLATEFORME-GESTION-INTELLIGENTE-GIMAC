package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A Coverage.
 */
@Entity
@Table(name = "coverage")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Coverage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 40)
    @Column(name = "service_code", length = 40, nullable = false)
    private String serviceCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false)
    private NatureCouverture nature;

    @Column(name = "numerateur")
    private Integer numerateur;

    @Column(name = "denominateur")
    private Integer denominateur;

    @Column(name = "taux", precision = 21, scale = 2)
    private BigDecimal taux;

    @NotNull
    @Column(name = "calculable", nullable = false)
    private Boolean calculable;

    @Size(max = 200)
    @Column(name = "motif_non_calculable", length = 200)
    private String motifNonCalculable;

    @NotNull
    @Column(name = "date_calcul", nullable = false)
    private Instant dateCalcul;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "version", "contractualScope" }, allowSetters = true)
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

    public NatureCouverture getNature() {
        return this.nature;
    }

    public Coverage nature(NatureCouverture nature) {
        this.setNature(nature);
        return this;
    }

    public void setNature(NatureCouverture nature) {
        this.nature = nature;
    }

    public Integer getNumerateur() {
        return this.numerateur;
    }

    public Coverage numerateur(Integer numerateur) {
        this.setNumerateur(numerateur);
        return this;
    }

    public void setNumerateur(Integer numerateur) {
        this.numerateur = numerateur;
    }

    public Integer getDenominateur() {
        return this.denominateur;
    }

    public Coverage denominateur(Integer denominateur) {
        this.setDenominateur(denominateur);
        return this;
    }

    public void setDenominateur(Integer denominateur) {
        this.denominateur = denominateur;
    }

    public BigDecimal getTaux() {
        return this.taux;
    }

    public Coverage taux(BigDecimal taux) {
        this.setTaux(taux);
        return this;
    }

    public void setTaux(BigDecimal taux) {
        this.taux = taux;
    }

    public Boolean getCalculable() {
        return this.calculable;
    }

    public Coverage calculable(Boolean calculable) {
        this.setCalculable(calculable);
        return this;
    }

    public void setCalculable(Boolean calculable) {
        this.calculable = calculable;
    }

    public String getMotifNonCalculable() {
        return this.motifNonCalculable;
    }

    public Coverage motifNonCalculable(String motifNonCalculable) {
        this.setMotifNonCalculable(motifNonCalculable);
        return this;
    }

    public void setMotifNonCalculable(String motifNonCalculable) {
        this.motifNonCalculable = motifNonCalculable;
    }

    public Instant getDateCalcul() {
        return this.dateCalcul;
    }

    public Coverage dateCalcul(Instant dateCalcul) {
        this.setDateCalcul(dateCalcul);
        return this;
    }

    public void setDateCalcul(Instant dateCalcul) {
        this.dateCalcul = dateCalcul;
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
            ", nature='" + getNature() + "'" +
            ", numerateur=" + getNumerateur() +
            ", denominateur=" + getDenominateur() +
            ", taux=" + getTaux() +
            ", calculable='" + getCalculable() + "'" +
            ", motifNonCalculable='" + getMotifNonCalculable() + "'" +
            ", dateCalcul='" + getDateCalcul() + "'" +
            "}";
    }
}
