package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.EtatAttestation;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.CertificationRegisterEntry} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CertificationRegisterEntryDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    private String numeroAttestation;

    @NotNull
    private UUID institutionId;

    @Size(max = 40)
    private String typeProjet;

    @Lob
    private String services;

    private Instant dateEmission;

    private LocalDate echeanceRenouvellement;

    private EtatAttestation etat;

    private UUID precedenteId;

    @Size(max = 30)
    private String dernierSeuilNotifie;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumeroAttestation() {
        return numeroAttestation;
    }

    public void setNumeroAttestation(String numeroAttestation) {
        this.numeroAttestation = numeroAttestation;
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

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public Instant getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(Instant dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getEcheanceRenouvellement() {
        return echeanceRenouvellement;
    }

    public void setEcheanceRenouvellement(LocalDate echeanceRenouvellement) {
        this.echeanceRenouvellement = echeanceRenouvellement;
    }

    public EtatAttestation getEtat() {
        return etat;
    }

    public void setEtat(EtatAttestation etat) {
        this.etat = etat;
    }

    public UUID getPrecedenteId() {
        return precedenteId;
    }

    public void setPrecedenteId(UUID precedenteId) {
        this.precedenteId = precedenteId;
    }

    public String getDernierSeuilNotifie() {
        return dernierSeuilNotifie;
    }

    public void setDernierSeuilNotifie(String dernierSeuilNotifie) {
        this.dernierSeuilNotifie = dernierSeuilNotifie;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificationRegisterEntryDTO)) {
            return false;
        }

        CertificationRegisterEntryDTO certificationRegisterEntryDTO = (CertificationRegisterEntryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, certificationRegisterEntryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CertificationRegisterEntryDTO{" +
            "id='" + getId() + "'" +
            ", numeroAttestation='" + getNumeroAttestation() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", typeProjet='" + getTypeProjet() + "'" +
            ", services='" + getServices() + "'" +
            ", dateEmission='" + getDateEmission() + "'" +
            ", echeanceRenouvellement='" + getEcheanceRenouvellement() + "'" +
            ", etat='" + getEtat() + "'" +
            ", precedenteId='" + getPrecedenteId() + "'" +
            ", dernierSeuilNotifie='" + getDernierSeuilNotifie() + "'" +
            "}";
    }
}
