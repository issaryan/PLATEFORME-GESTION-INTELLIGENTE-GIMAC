package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.EtatDossier;
import cm.gimac.des.certification.domain.enumeration.EtatScellement;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.GoLiveDossier} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GoLiveDossierDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID liaisonId;

    private UUID institutionId;

    @NotNull
    private EtatDossier etat;

    private Instant dateOuverture;

    private Instant dateAutorisation;

    private Instant dateMiseEnService;

    private Instant dateCompletude;

    private EtatScellement etatScellement;

    @Size(max = 120)
    private String referenceFicheChangement;

    @NotNull
    private CertificationCircuitDTO circuit;

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

    public EtatDossier getEtat() {
        return etat;
    }

    public void setEtat(EtatDossier etat) {
        this.etat = etat;
    }

    public Instant getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(Instant dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public Instant getDateAutorisation() {
        return dateAutorisation;
    }

    public void setDateAutorisation(Instant dateAutorisation) {
        this.dateAutorisation = dateAutorisation;
    }

    public Instant getDateMiseEnService() {
        return dateMiseEnService;
    }

    public void setDateMiseEnService(Instant dateMiseEnService) {
        this.dateMiseEnService = dateMiseEnService;
    }

    public Instant getDateCompletude() {
        return dateCompletude;
    }

    public void setDateCompletude(Instant dateCompletude) {
        this.dateCompletude = dateCompletude;
    }

    public EtatScellement getEtatScellement() {
        return etatScellement;
    }

    public void setEtatScellement(EtatScellement etatScellement) {
        this.etatScellement = etatScellement;
    }

    public String getReferenceFicheChangement() {
        return referenceFicheChangement;
    }

    public void setReferenceFicheChangement(String referenceFicheChangement) {
        this.referenceFicheChangement = referenceFicheChangement;
    }

    public CertificationCircuitDTO getCircuit() {
        return circuit;
    }

    public void setCircuit(CertificationCircuitDTO circuit) {
        this.circuit = circuit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GoLiveDossierDTO)) {
            return false;
        }

        GoLiveDossierDTO goLiveDossierDTO = (GoLiveDossierDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, goLiveDossierDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GoLiveDossierDTO{" +
            "id='" + getId() + "'" +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", etat='" + getEtat() + "'" +
            ", dateOuverture='" + getDateOuverture() + "'" +
            ", dateAutorisation='" + getDateAutorisation() + "'" +
            ", dateMiseEnService='" + getDateMiseEnService() + "'" +
            ", dateCompletude='" + getDateCompletude() + "'" +
            ", etatScellement='" + getEtatScellement() + "'" +
            ", referenceFicheChangement='" + getReferenceFicheChangement() + "'" +
            ", circuit=" + getCircuit() +
            "}";
    }
}
