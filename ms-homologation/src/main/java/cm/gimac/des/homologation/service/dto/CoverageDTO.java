package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.Coverage} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CoverageDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 40)
    private String serviceCode;

    @NotNull
    private NatureCouverture nature;

    private Integer numerateur;

    private Integer denominateur;

    private BigDecimal taux;

    @NotNull
    private Boolean calculable;

    @Size(max = 200)
    private String motifNonCalculable;

    @NotNull
    private Instant dateCalcul;

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

    public NatureCouverture getNature() {
        return nature;
    }

    public void setNature(NatureCouverture nature) {
        this.nature = nature;
    }

    public Integer getNumerateur() {
        return numerateur;
    }

    public void setNumerateur(Integer numerateur) {
        this.numerateur = numerateur;
    }

    public Integer getDenominateur() {
        return denominateur;
    }

    public void setDenominateur(Integer denominateur) {
        this.denominateur = denominateur;
    }

    public BigDecimal getTaux() {
        return taux;
    }

    public void setTaux(BigDecimal taux) {
        this.taux = taux;
    }

    public Boolean getCalculable() {
        return calculable;
    }

    public void setCalculable(Boolean calculable) {
        this.calculable = calculable;
    }

    public String getMotifNonCalculable() {
        return motifNonCalculable;
    }

    public void setMotifNonCalculable(String motifNonCalculable) {
        this.motifNonCalculable = motifNonCalculable;
    }

    public Instant getDateCalcul() {
        return dateCalcul;
    }

    public void setDateCalcul(Instant dateCalcul) {
        this.dateCalcul = dateCalcul;
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
            ", nature='" + getNature() + "'" +
            ", numerateur=" + getNumerateur() +
            ", denominateur=" + getDenominateur() +
            ", taux=" + getTaux() +
            ", calculable='" + getCalculable() + "'" +
            ", motifNonCalculable='" + getMotifNonCalculable() + "'" +
            ", dateCalcul='" + getDateCalcul() + "'" +
            ", liaison=" + getLiaison() +
            "}";
    }
}
