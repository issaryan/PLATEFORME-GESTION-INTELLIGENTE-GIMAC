package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.TypeDemandeStatut;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A AttestationStatusDecision.
 */
@Entity
@Table(name = "attestation_status_decision")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AttestationStatusDecision implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_demande", nullable = false)
    private TypeDemandeStatut typeDemande;

    @NotNull
    @Size(max = 1000)
    @Column(name = "motif", length = 1000, nullable = false)
    private String motif;

    @Size(max = 120)
    @Column(name = "piece_ref", length = 120)
    private String pieceRef;

    @NotNull
    @Column(name = "demandeur_id", nullable = false)
    private UUID demandeurId;

    @Column(name = "approbateur_id")
    private UUID approbateurId;

    @NotNull
    @Column(name = "date_demande", nullable = false)
    private Instant dateDemande;

    @Column(name = "date_approbation")
    private Instant dateApprobation;

    @Size(max = 30)
    @Column(name = "etat", length = 30)
    private String etat;

    @Size(max = 120)
    @Column(name = "avis_document_ref", length = 120)
    private String avisDocumentRef;

    @Size(max = 500)
    @Column(name = "condition_levee", length = 500)
    private String conditionLevee;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "dossier" }, allowSetters = true)
    private Attestation attestation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AttestationStatusDecision id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TypeDemandeStatut getTypeDemande() {
        return this.typeDemande;
    }

    public AttestationStatusDecision typeDemande(TypeDemandeStatut typeDemande) {
        this.setTypeDemande(typeDemande);
        return this;
    }

    public void setTypeDemande(TypeDemandeStatut typeDemande) {
        this.typeDemande = typeDemande;
    }

    public String getMotif() {
        return this.motif;
    }

    public AttestationStatusDecision motif(String motif) {
        this.setMotif(motif);
        return this;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getPieceRef() {
        return this.pieceRef;
    }

    public AttestationStatusDecision pieceRef(String pieceRef) {
        this.setPieceRef(pieceRef);
        return this;
    }

    public void setPieceRef(String pieceRef) {
        this.pieceRef = pieceRef;
    }

    public UUID getDemandeurId() {
        return this.demandeurId;
    }

    public AttestationStatusDecision demandeurId(UUID demandeurId) {
        this.setDemandeurId(demandeurId);
        return this;
    }

    public void setDemandeurId(UUID demandeurId) {
        this.demandeurId = demandeurId;
    }

    public UUID getApprobateurId() {
        return this.approbateurId;
    }

    public AttestationStatusDecision approbateurId(UUID approbateurId) {
        this.setApprobateurId(approbateurId);
        return this;
    }

    public void setApprobateurId(UUID approbateurId) {
        this.approbateurId = approbateurId;
    }

    public Instant getDateDemande() {
        return this.dateDemande;
    }

    public AttestationStatusDecision dateDemande(Instant dateDemande) {
        this.setDateDemande(dateDemande);
        return this;
    }

    public void setDateDemande(Instant dateDemande) {
        this.dateDemande = dateDemande;
    }

    public Instant getDateApprobation() {
        return this.dateApprobation;
    }

    public AttestationStatusDecision dateApprobation(Instant dateApprobation) {
        this.setDateApprobation(dateApprobation);
        return this;
    }

    public void setDateApprobation(Instant dateApprobation) {
        this.dateApprobation = dateApprobation;
    }

    public String getEtat() {
        return this.etat;
    }

    public AttestationStatusDecision etat(String etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public String getAvisDocumentRef() {
        return this.avisDocumentRef;
    }

    public AttestationStatusDecision avisDocumentRef(String avisDocumentRef) {
        this.setAvisDocumentRef(avisDocumentRef);
        return this;
    }

    public void setAvisDocumentRef(String avisDocumentRef) {
        this.avisDocumentRef = avisDocumentRef;
    }

    public String getConditionLevee() {
        return this.conditionLevee;
    }

    public AttestationStatusDecision conditionLevee(String conditionLevee) {
        this.setConditionLevee(conditionLevee);
        return this;
    }

    public void setConditionLevee(String conditionLevee) {
        this.conditionLevee = conditionLevee;
    }

    public Attestation getAttestation() {
        return this.attestation;
    }

    public void setAttestation(Attestation attestation) {
        this.attestation = attestation;
    }

    public AttestationStatusDecision attestation(Attestation attestation) {
        this.setAttestation(attestation);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttestationStatusDecision)) {
            return false;
        }
        return getId() != null && getId().equals(((AttestationStatusDecision) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AttestationStatusDecision{" +
            "id=" + getId() +
            ", typeDemande='" + getTypeDemande() + "'" +
            ", motif='" + getMotif() + "'" +
            ", pieceRef='" + getPieceRef() + "'" +
            ", demandeurId='" + getDemandeurId() + "'" +
            ", approbateurId='" + getApprobateurId() + "'" +
            ", dateDemande='" + getDateDemande() + "'" +
            ", dateApprobation='" + getDateApprobation() + "'" +
            ", etat='" + getEtat() + "'" +
            ", avisDocumentRef='" + getAvisDocumentRef() + "'" +
            ", conditionLevee='" + getConditionLevee() + "'" +
            "}";
    }
}
