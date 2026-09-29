package cm.gimac.des.certification.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.SignedDecision} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SignedDecisionDTO implements Serializable {

    private UUID id;

    @NotNull
    private Boolean decisionFavorable;

    @NotNull
    @Size(max = 1000)
    private String motif;

    @NotNull
    private UUID signataireId;

    @Size(max = 60)
    private String signataireRole;

    @NotNull
    private Instant dateSignature;

    @NotNull
    @Size(max = 64)
    private String socleEmpreinte;

    @NotNull
    @Size(max = 64)
    private String empreinteDecision;

    @Lob
    private String signature;

    @Lob
    private String operationsDesignees;

    @Lob
    private String servicesDesignes;

    @Size(max = 120)
    private String referenceAudit;

    @NotNull
    private QuitusDTO quitus;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Boolean getDecisionFavorable() {
        return decisionFavorable;
    }

    public void setDecisionFavorable(Boolean decisionFavorable) {
        this.decisionFavorable = decisionFavorable;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public UUID getSignataireId() {
        return signataireId;
    }

    public void setSignataireId(UUID signataireId) {
        this.signataireId = signataireId;
    }

    public String getSignataireRole() {
        return signataireRole;
    }

    public void setSignataireRole(String signataireRole) {
        this.signataireRole = signataireRole;
    }

    public Instant getDateSignature() {
        return dateSignature;
    }

    public void setDateSignature(Instant dateSignature) {
        this.dateSignature = dateSignature;
    }

    public String getSocleEmpreinte() {
        return socleEmpreinte;
    }

    public void setSocleEmpreinte(String socleEmpreinte) {
        this.socleEmpreinte = socleEmpreinte;
    }

    public String getEmpreinteDecision() {
        return empreinteDecision;
    }

    public void setEmpreinteDecision(String empreinteDecision) {
        this.empreinteDecision = empreinteDecision;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getOperationsDesignees() {
        return operationsDesignees;
    }

    public void setOperationsDesignees(String operationsDesignees) {
        this.operationsDesignees = operationsDesignees;
    }

    public String getServicesDesignes() {
        return servicesDesignes;
    }

    public void setServicesDesignes(String servicesDesignes) {
        this.servicesDesignes = servicesDesignes;
    }

    public String getReferenceAudit() {
        return referenceAudit;
    }

    public void setReferenceAudit(String referenceAudit) {
        this.referenceAudit = referenceAudit;
    }

    public QuitusDTO getQuitus() {
        return quitus;
    }

    public void setQuitus(QuitusDTO quitus) {
        this.quitus = quitus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SignedDecisionDTO)) {
            return false;
        }

        SignedDecisionDTO signedDecisionDTO = (SignedDecisionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, signedDecisionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SignedDecisionDTO{" +
            "id='" + getId() + "'" +
            ", decisionFavorable='" + getDecisionFavorable() + "'" +
            ", motif='" + getMotif() + "'" +
            ", signataireId='" + getSignataireId() + "'" +
            ", signataireRole='" + getSignataireRole() + "'" +
            ", dateSignature='" + getDateSignature() + "'" +
            ", socleEmpreinte='" + getSocleEmpreinte() + "'" +
            ", empreinteDecision='" + getEmpreinteDecision() + "'" +
            ", signature='" + getSignature() + "'" +
            ", operationsDesignees='" + getOperationsDesignees() + "'" +
            ", servicesDesignes='" + getServicesDesignes() + "'" +
            ", referenceAudit='" + getReferenceAudit() + "'" +
            ", quitus=" + getQuitus() +
            "}";
    }
}
