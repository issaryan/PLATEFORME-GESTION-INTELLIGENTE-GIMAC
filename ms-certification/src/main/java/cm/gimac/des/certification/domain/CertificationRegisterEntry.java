package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.EtatAttestation;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A CertificationRegisterEntry.
 */
@Entity
@Table(name = "certification_register_entry")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CertificationRegisterEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 30)
    @Column(name = "numero_attestation", length = 30, nullable = false)
    private String numeroAttestation;

    @NotNull
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Size(max = 30)
    @Column(name = "type_projet", length = 30)
    private String typeProjet;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "services", columnDefinition = "jsonb")
    private String services;

    @Column(name = "date_emission")
    private Instant dateEmission;

    @Column(name = "echeance_renouvellement")
    private LocalDate echeanceRenouvellement;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat")
    private EtatAttestation etat;

    @Column(name = "precedente_id")
    private UUID precedenteId;

    @Size(max = 30)
    @Column(name = "dernier_seuil_notifie", length = 30)
    private String dernierSeuilNotifie;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CertificationRegisterEntry id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumeroAttestation() {
        return this.numeroAttestation;
    }

    public CertificationRegisterEntry numeroAttestation(String numeroAttestation) {
        this.setNumeroAttestation(numeroAttestation);
        return this;
    }

    public void setNumeroAttestation(String numeroAttestation) {
        this.numeroAttestation = numeroAttestation;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public CertificationRegisterEntry institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getTypeProjet() {
        return this.typeProjet;
    }

    public CertificationRegisterEntry typeProjet(String typeProjet) {
        this.setTypeProjet(typeProjet);
        return this;
    }

    public void setTypeProjet(String typeProjet) {
        this.typeProjet = typeProjet;
    }

    public String getServices() {
        return this.services;
    }

    public CertificationRegisterEntry services(String services) {
        this.setServices(services);
        return this;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public Instant getDateEmission() {
        return this.dateEmission;
    }

    public CertificationRegisterEntry dateEmission(Instant dateEmission) {
        this.setDateEmission(dateEmission);
        return this;
    }

    public void setDateEmission(Instant dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getEcheanceRenouvellement() {
        return this.echeanceRenouvellement;
    }

    public CertificationRegisterEntry echeanceRenouvellement(LocalDate echeanceRenouvellement) {
        this.setEcheanceRenouvellement(echeanceRenouvellement);
        return this;
    }

    public void setEcheanceRenouvellement(LocalDate echeanceRenouvellement) {
        this.echeanceRenouvellement = echeanceRenouvellement;
    }

    public EtatAttestation getEtat() {
        return this.etat;
    }

    public CertificationRegisterEntry etat(EtatAttestation etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatAttestation etat) {
        this.etat = etat;
    }

    public UUID getPrecedenteId() {
        return this.precedenteId;
    }

    public CertificationRegisterEntry precedenteId(UUID precedenteId) {
        this.setPrecedenteId(precedenteId);
        return this;
    }

    public void setPrecedenteId(UUID precedenteId) {
        this.precedenteId = precedenteId;
    }

    public String getDernierSeuilNotifie() {
        return this.dernierSeuilNotifie;
    }

    public CertificationRegisterEntry dernierSeuilNotifie(String dernierSeuilNotifie) {
        this.setDernierSeuilNotifie(dernierSeuilNotifie);
        return this;
    }

    public void setDernierSeuilNotifie(String dernierSeuilNotifie) {
        this.dernierSeuilNotifie = dernierSeuilNotifie;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificationRegisterEntry)) {
            return false;
        }
        return getId() != null && getId().equals(((CertificationRegisterEntry) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CertificationRegisterEntry{" +
            "id=" + getId() +
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
