package cm.gimac.des.certification.domain;

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
 * A SignedDecision.
 */
@Entity
@Table(name = "signed_decision")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SignedDecision implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "decision_favorable", nullable = false)
    private Boolean decisionFavorable;

    @NotNull
    @Size(max = 1000)
    @Column(name = "motif", length = 1000, nullable = false)
    private String motif;

    @NotNull
    @Column(name = "signataire_id", nullable = false)
    private UUID signataireId;

    @Size(max = 60)
    @Column(name = "signataire_role", length = 60)
    private String signataireRole;

    @NotNull
    @Column(name = "date_signature", nullable = false)
    private Instant dateSignature;

    @NotNull
    @Size(max = 64)
    @Column(name = "socle_empreinte", length = 64, nullable = false)
    private String socleEmpreinte;

    @NotNull
    @Size(max = 64)
    @Column(name = "empreinte_decision", length = 64, nullable = false)
    private String empreinteDecision;

    @Lob
    @Column(name = "signature", nullable = false)
    private String signature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operations_designees", columnDefinition = "jsonb")
    private String operationsDesignees;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "services_designes", columnDefinition = "jsonb")
    private String servicesDesignes;

    @Size(max = 120)
    @Column(name = "reference_audit", length = 120)
    private String referenceAudit;

    @JsonIgnoreProperties(value = { "circuit", "signedDecision" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private Quitus quitus;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public SignedDecision id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Boolean getDecisionFavorable() {
        return this.decisionFavorable;
    }

    public SignedDecision decisionFavorable(Boolean decisionFavorable) {
        this.setDecisionFavorable(decisionFavorable);
        return this;
    }

    public void setDecisionFavorable(Boolean decisionFavorable) {
        this.decisionFavorable = decisionFavorable;
    }

    public String getMotif() {
        return this.motif;
    }

    public SignedDecision motif(String motif) {
        this.setMotif(motif);
        return this;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public UUID getSignataireId() {
        return this.signataireId;
    }

    public SignedDecision signataireId(UUID signataireId) {
        this.setSignataireId(signataireId);
        return this;
    }

    public void setSignataireId(UUID signataireId) {
        this.signataireId = signataireId;
    }

    public String getSignataireRole() {
        return this.signataireRole;
    }

    public SignedDecision signataireRole(String signataireRole) {
        this.setSignataireRole(signataireRole);
        return this;
    }

    public void setSignataireRole(String signataireRole) {
        this.signataireRole = signataireRole;
    }

    public Instant getDateSignature() {
        return this.dateSignature;
    }

    public SignedDecision dateSignature(Instant dateSignature) {
        this.setDateSignature(dateSignature);
        return this;
    }

    public void setDateSignature(Instant dateSignature) {
        this.dateSignature = dateSignature;
    }

    public String getSocleEmpreinte() {
        return this.socleEmpreinte;
    }

    public SignedDecision socleEmpreinte(String socleEmpreinte) {
        this.setSocleEmpreinte(socleEmpreinte);
        return this;
    }

    public void setSocleEmpreinte(String socleEmpreinte) {
        this.socleEmpreinte = socleEmpreinte;
    }

    public String getEmpreinteDecision() {
        return this.empreinteDecision;
    }

    public SignedDecision empreinteDecision(String empreinteDecision) {
        this.setEmpreinteDecision(empreinteDecision);
        return this;
    }

    public void setEmpreinteDecision(String empreinteDecision) {
        this.empreinteDecision = empreinteDecision;
    }

    public String getSignature() {
        return this.signature;
    }

    public SignedDecision signature(String signature) {
        this.setSignature(signature);
        return this;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getOperationsDesignees() {
        return this.operationsDesignees;
    }

    public SignedDecision operationsDesignees(String operationsDesignees) {
        this.setOperationsDesignees(operationsDesignees);
        return this;
    }

    public void setOperationsDesignees(String operationsDesignees) {
        this.operationsDesignees = operationsDesignees;
    }

    public String getServicesDesignes() {
        return this.servicesDesignes;
    }

    public SignedDecision servicesDesignes(String servicesDesignes) {
        this.setServicesDesignes(servicesDesignes);
        return this;
    }

    public void setServicesDesignes(String servicesDesignes) {
        this.servicesDesignes = servicesDesignes;
    }

    public String getReferenceAudit() {
        return this.referenceAudit;
    }

    public SignedDecision referenceAudit(String referenceAudit) {
        this.setReferenceAudit(referenceAudit);
        return this;
    }

    public void setReferenceAudit(String referenceAudit) {
        this.referenceAudit = referenceAudit;
    }

    public Quitus getQuitus() {
        return this.quitus;
    }

    public void setQuitus(Quitus quitus) {
        this.quitus = quitus;
    }

    public SignedDecision quitus(Quitus quitus) {
        this.setQuitus(quitus);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SignedDecision)) {
            return false;
        }
        return getId() != null && getId().equals(((SignedDecision) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SignedDecision{" +
            "id=" + getId() +
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
            "}";
    }
}
