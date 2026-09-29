package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.EtatAttestation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A Attestation.
 */
@Entity
@Table(name = "attestation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Attestation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 30)
    @Column(name = "numero", length = 30, nullable = false, unique = true)
    private String numero;

    @Column(name = "institution_id")
    private UUID institutionId;

    @Size(max = 30)
    @Column(name = "type_projet", length = 30)
    private String typeProjet;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "services_certifies", columnDefinition = "jsonb")
    private String servicesCertifies;

    @Column(name = "date_emission")
    private Instant dateEmission;

    @Size(max = 64)
    @Column(name = "socle_empreinte", length = 64)
    private String socleEmpreinte;

    @Size(max = 120)
    @Column(name = "document_ref", length = 120)
    private String documentRef;

    @Size(max = 64)
    @Column(name = "empreinte_document", length = 64)
    private String empreinteDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat")
    private EtatAttestation etat;

    @Size(max = 120)
    @Column(name = "reference_audit", length = 120)
    private String referenceAudit;

    @JsonIgnoreProperties(value = { "circuit", "goLiveReport", "attestation" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private GoLiveDossier dossier;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Attestation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumero() {
        return this.numero;
    }

    public Attestation numero(String numero) {
        this.setNumero(numero);
        return this;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Attestation institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getTypeProjet() {
        return this.typeProjet;
    }

    public Attestation typeProjet(String typeProjet) {
        this.setTypeProjet(typeProjet);
        return this;
    }

    public void setTypeProjet(String typeProjet) {
        this.typeProjet = typeProjet;
    }

    public String getServicesCertifies() {
        return this.servicesCertifies;
    }

    public Attestation servicesCertifies(String servicesCertifies) {
        this.setServicesCertifies(servicesCertifies);
        return this;
    }

    public void setServicesCertifies(String servicesCertifies) {
        this.servicesCertifies = servicesCertifies;
    }

    public Instant getDateEmission() {
        return this.dateEmission;
    }

    public Attestation dateEmission(Instant dateEmission) {
        this.setDateEmission(dateEmission);
        return this;
    }

    public void setDateEmission(Instant dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getSocleEmpreinte() {
        return this.socleEmpreinte;
    }

    public Attestation socleEmpreinte(String socleEmpreinte) {
        this.setSocleEmpreinte(socleEmpreinte);
        return this;
    }

    public void setSocleEmpreinte(String socleEmpreinte) {
        this.socleEmpreinte = socleEmpreinte;
    }

    public String getDocumentRef() {
        return this.documentRef;
    }

    public Attestation documentRef(String documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public String getEmpreinteDocument() {
        return this.empreinteDocument;
    }

    public Attestation empreinteDocument(String empreinteDocument) {
        this.setEmpreinteDocument(empreinteDocument);
        return this;
    }

    public void setEmpreinteDocument(String empreinteDocument) {
        this.empreinteDocument = empreinteDocument;
    }

    public EtatAttestation getEtat() {
        return this.etat;
    }

    public Attestation etat(EtatAttestation etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatAttestation etat) {
        this.etat = etat;
    }

    public String getReferenceAudit() {
        return this.referenceAudit;
    }

    public Attestation referenceAudit(String referenceAudit) {
        this.setReferenceAudit(referenceAudit);
        return this;
    }

    public void setReferenceAudit(String referenceAudit) {
        this.referenceAudit = referenceAudit;
    }

    public GoLiveDossier getDossier() {
        return this.dossier;
    }

    public void setDossier(GoLiveDossier goLiveDossier) {
        this.dossier = goLiveDossier;
    }

    public Attestation dossier(GoLiveDossier goLiveDossier) {
        this.setDossier(goLiveDossier);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Attestation)) {
            return false;
        }
        return getId() != null && getId().equals(((Attestation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Attestation{" +
            "id=" + getId() +
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
            "}";
    }
}
