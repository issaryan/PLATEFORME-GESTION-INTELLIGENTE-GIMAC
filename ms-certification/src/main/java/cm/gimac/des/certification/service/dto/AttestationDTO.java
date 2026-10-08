package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.EtatAttestation;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.Attestation} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AttestationDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    private String numero;

    private UUID institutionId;

    @Size(max = 40)
    private String typeProjet;

    @Lob
    private String servicesCertifies;

    private Instant dateEmission;

    @Size(max = 64)
    private String socleEmpreinte;

    @Size(max = 120)
    private String documentRef;

    @Size(max = 64)
    private String empreinteDocument;

    private EtatAttestation etat;

    @Size(max = 120)
    private String referenceAudit;

    @NotNull
    private GoLiveDossierDTO dossier;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getTypeProjet() {
        return typeProjet;
    }

    public void setTypeProjet(String typeProjet) {
        this.typeProjet = typeProjet;
    }

    public String getServicesCertifies() {
        return servicesCertifies;
    }

    public void setServicesCertifies(String servicesCertifies) {
        this.servicesCertifies = servicesCertifies;
    }

    public Instant getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(Instant dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getSocleEmpreinte() {
        return socleEmpreinte;
    }

    public void setSocleEmpreinte(String socleEmpreinte) {
        this.socleEmpreinte = socleEmpreinte;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public String getEmpreinteDocument() {
        return empreinteDocument;
    }

    public void setEmpreinteDocument(String empreinteDocument) {
        this.empreinteDocument = empreinteDocument;
    }

    public EtatAttestation getEtat() {
        return etat;
    }

    public void setEtat(EtatAttestation etat) {
        this.etat = etat;
    }

    public String getReferenceAudit() {
        return referenceAudit;
    }

    public void setReferenceAudit(String referenceAudit) {
        this.referenceAudit = referenceAudit;
    }

    public GoLiveDossierDTO getDossier() {
        return dossier;
    }

    public void setDossier(GoLiveDossierDTO dossier) {
        this.dossier = dossier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttestationDTO)) {
            return false;
        }

        AttestationDTO attestationDTO = (AttestationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, attestationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AttestationDTO{" +
            "id='" + getId() + "'" +
            ", numero='" + getNumero() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", typeProjet='" + getTypeProjet() + "'" +
            ", servicesCertifies='" + getServicesCertifies() + "'" +
            ", dateEmission='" + getDateEmission() + "'" +
            ", socleEmpreinte='" + getSocleEmpreinte() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", empreinteDocument='" + getEmpreinteDocument() + "'" +
            ", etat='" + getEtat() + "'" +
            ", referenceAudit='" + getReferenceAudit() + "'" +
            ", dossier=" + getDossier() +
            "}";
    }
}
