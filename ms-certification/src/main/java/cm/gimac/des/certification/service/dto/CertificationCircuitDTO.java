package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.EtatCircuit;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.CertificationCircuit} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CertificationCircuitDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID liaisonId;

    private UUID institutionId;

    @NotNull
    private Integer cycle;

    @NotNull
    @Size(max = 64)
    private String socleEmpreinte;

    @NotNull
    private EtatCircuit etat;

    private Instant dateOuverture;

    private Instant dateFin;

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

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public Integer getCycle() {
        return cycle;
    }

    public void setCycle(Integer cycle) {
        this.cycle = cycle;
    }

    public String getSocleEmpreinte() {
        return socleEmpreinte;
    }

    public void setSocleEmpreinte(String socleEmpreinte) {
        this.socleEmpreinte = socleEmpreinte;
    }

    public EtatCircuit getEtat() {
        return etat;
    }

    public void setEtat(EtatCircuit etat) {
        this.etat = etat;
    }

    public Instant getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(Instant dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public Instant getDateFin() {
        return dateFin;
    }

    public void setDateFin(Instant dateFin) {
        this.dateFin = dateFin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificationCircuitDTO)) {
            return false;
        }

        CertificationCircuitDTO certificationCircuitDTO = (CertificationCircuitDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, certificationCircuitDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CertificationCircuitDTO{" +
            "id='" + getId() + "'" +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", cycle=" + getCycle() +
            ", socleEmpreinte='" + getSocleEmpreinte() + "'" +
            ", etat='" + getEtat() + "'" +
            ", dateOuverture='" + getDateOuverture() + "'" +
            ", dateFin='" + getDateFin() + "'" +
            "}";
    }
}
