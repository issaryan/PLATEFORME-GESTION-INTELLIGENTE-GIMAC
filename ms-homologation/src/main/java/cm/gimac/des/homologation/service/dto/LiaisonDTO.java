package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.Liaison} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LiaisonDTO implements Serializable {

    private UUID id;

    @NotNull
    private TypeProjet typeProjet;

    @NotNull
    private UUID institutionId;

    @Size(max = 200)
    private String institutionNom;

    @NotNull
    private EtatLiaison etat;

    @NotNull
    private UUID chefDeProjetId;

    private UUID banquePartenaireId;

    @NotNull
    private Instant dateOuverture;

    @Size(max = 500)
    private String motifEtat;

    @NotNull
    private Integer cycleAptitude;

    @Lob
    private String ordreInstanciation;

    @Lob
    private String etatAvantGel;

    @NotNull
    private ReferentialVersionDTO version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TypeProjet getTypeProjet() {
        return typeProjet;
    }

    public void setTypeProjet(TypeProjet typeProjet) {
        this.typeProjet = typeProjet;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionNom() {
        return institutionNom;
    }

    public void setInstitutionNom(String institutionNom) {
        this.institutionNom = institutionNom;
    }

    public EtatLiaison getEtat() {
        return etat;
    }

    public void setEtat(EtatLiaison etat) {
        this.etat = etat;
    }

    public UUID getChefDeProjetId() {
        return chefDeProjetId;
    }

    public void setChefDeProjetId(UUID chefDeProjetId) {
        this.chefDeProjetId = chefDeProjetId;
    }

    public UUID getBanquePartenaireId() {
        return banquePartenaireId;
    }

    public void setBanquePartenaireId(UUID banquePartenaireId) {
        this.banquePartenaireId = banquePartenaireId;
    }

    public Instant getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(Instant dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public String getMotifEtat() {
        return motifEtat;
    }

    public void setMotifEtat(String motifEtat) {
        this.motifEtat = motifEtat;
    }

    public Integer getCycleAptitude() {
        return cycleAptitude;
    }

    public void setCycleAptitude(Integer cycleAptitude) {
        this.cycleAptitude = cycleAptitude;
    }

    public String getOrdreInstanciation() {
        return ordreInstanciation;
    }

    public void setOrdreInstanciation(String ordreInstanciation) {
        this.ordreInstanciation = ordreInstanciation;
    }

    public String getEtatAvantGel() {
        return etatAvantGel;
    }

    public void setEtatAvantGel(String etatAvantGel) {
        this.etatAvantGel = etatAvantGel;
    }

    public ReferentialVersionDTO getVersion() {
        return version;
    }

    public void setVersion(ReferentialVersionDTO version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiaisonDTO)) {
            return false;
        }

        LiaisonDTO liaisonDTO = (LiaisonDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, liaisonDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LiaisonDTO{" +
            "id='" + getId() + "'" +
            ", typeProjet='" + getTypeProjet() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", institutionNom='" + getInstitutionNom() + "'" +
            ", etat='" + getEtat() + "'" +
            ", chefDeProjetId='" + getChefDeProjetId() + "'" +
            ", banquePartenaireId='" + getBanquePartenaireId() + "'" +
            ", dateOuverture='" + getDateOuverture() + "'" +
            ", motifEtat='" + getMotifEtat() + "'" +
            ", cycleAptitude=" + getCycleAptitude() +
            ", ordreInstanciation='" + getOrdreInstanciation() + "'" +
            ", etatAvantGel='" + getEtatAvantGel() + "'" +
            ", version=" + getVersion() +
            "}";
    }
}
