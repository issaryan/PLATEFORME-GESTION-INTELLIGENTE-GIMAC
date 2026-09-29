package cm.gimac.des.certification.service.dto;

import cm.gimac.des.certification.domain.enumeration.TypeDemandeStatut;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.AttestationStatusDecision} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AttestationStatusDecisionDTO implements Serializable {

    private UUID id;

    @NotNull
    private TypeDemandeStatut typeDemande;

    @NotNull
    @Size(max = 1000)
    private String motif;

    @Size(max = 120)
    private String pieceRef;

    @NotNull
    private UUID demandeurId;

    private UUID approbateurId;

    @NotNull
    private Instant dateDemande;

    private Instant dateApprobation;

    @Size(max = 30)
    private String etat;

    @Size(max = 120)
    private String avisDocumentRef;

    @Size(max = 500)
    private String conditionLevee;

    @NotNull
    private AttestationDTO attestation;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TypeDemandeStatut getTypeDemande() {
        return typeDemande;
    }

    public void setTypeDemande(TypeDemandeStatut typeDemande) {
        this.typeDemande = typeDemande;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getPieceRef() {
        return pieceRef;
    }

    public void setPieceRef(String pieceRef) {
        this.pieceRef = pieceRef;
    }

    public UUID getDemandeurId() {
        return demandeurId;
    }

    public void setDemandeurId(UUID demandeurId) {
        this.demandeurId = demandeurId;
    }

    public UUID getApprobateurId() {
        return approbateurId;
    }

    public void setApprobateurId(UUID approbateurId) {
        this.approbateurId = approbateurId;
    }

    public Instant getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(Instant dateDemande) {
        this.dateDemande = dateDemande;
    }

    public Instant getDateApprobation() {
        return dateApprobation;
    }

    public void setDateApprobation(Instant dateApprobation) {
        this.dateApprobation = dateApprobation;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public String getAvisDocumentRef() {
        return avisDocumentRef;
    }

    public void setAvisDocumentRef(String avisDocumentRef) {
        this.avisDocumentRef = avisDocumentRef;
    }

    public String getConditionLevee() {
        return conditionLevee;
    }

    public void setConditionLevee(String conditionLevee) {
        this.conditionLevee = conditionLevee;
    }

    public AttestationDTO getAttestation() {
        return attestation;
    }

    public void setAttestation(AttestationDTO attestation) {
        this.attestation = attestation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttestationStatusDecisionDTO)) {
            return false;
        }

        AttestationStatusDecisionDTO attestationStatusDecisionDTO = (AttestationStatusDecisionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, attestationStatusDecisionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AttestationStatusDecisionDTO{" +
            "id='" + getId() + "'" +
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
            ", attestation=" + getAttestation() +
            "}";
    }
}
