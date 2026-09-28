package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.QrForm;
import cm.gimac.des.homologation.domain.enumeration.ScenarioOutcome;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vérification du code QR marchand, rattachée à une opération de la branche
 * paiement mobile (HOM-08). Le service n'en génère ni n'en décode aucun.
 */
@Entity
@Table(name = "qr_verification")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class QrVerification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "form", nullable = false)
    private QrForm form;

    /**
     * Résultat constaté par l'ingénieur d'homologation.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private ScenarioOutcome outcome;

    @NotNull
    @Column(name = "verified_on", nullable = false)
    private LocalDate verifiedOn;

    @NotNull
    @Column(name = "verifier_id", nullable = false)
    private UUID verifierId;

    @NotNull
    @Column(name = "evidence_document_id", nullable = false)
    private UUID evidenceDocumentId;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "evidence_digest", length = 64, nullable = false)
    private String evidenceDigest;

    @Column(name = "evidence_sealed_at")
    private Instant evidenceSealedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "catalog", "campaignSync" }, allowSetters = true)
    private Liaison liaison;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "liaison", "reference" }, allowSetters = true)
    private Operation operation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public QrVerification id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public QrForm getForm() {
        return this.form;
    }

    public QrVerification form(QrForm form) {
        this.setForm(form);
        return this;
    }

    public void setForm(QrForm form) {
        this.form = form;
    }

    public ScenarioOutcome getOutcome() {
        return this.outcome;
    }

    public QrVerification outcome(ScenarioOutcome outcome) {
        this.setOutcome(outcome);
        return this;
    }

    public void setOutcome(ScenarioOutcome outcome) {
        this.outcome = outcome;
    }

    public LocalDate getVerifiedOn() {
        return this.verifiedOn;
    }

    public QrVerification verifiedOn(LocalDate verifiedOn) {
        this.setVerifiedOn(verifiedOn);
        return this;
    }

    public void setVerifiedOn(LocalDate verifiedOn) {
        this.verifiedOn = verifiedOn;
    }

    public UUID getVerifierId() {
        return this.verifierId;
    }

    public QrVerification verifierId(UUID verifierId) {
        this.setVerifierId(verifierId);
        return this;
    }

    public void setVerifierId(UUID verifierId) {
        this.verifierId = verifierId;
    }

    public UUID getEvidenceDocumentId() {
        return this.evidenceDocumentId;
    }

    public QrVerification evidenceDocumentId(UUID evidenceDocumentId) {
        this.setEvidenceDocumentId(evidenceDocumentId);
        return this;
    }

    public void setEvidenceDocumentId(UUID evidenceDocumentId) {
        this.evidenceDocumentId = evidenceDocumentId;
    }

    public String getEvidenceDigest() {
        return this.evidenceDigest;
    }

    public QrVerification evidenceDigest(String evidenceDigest) {
        this.setEvidenceDigest(evidenceDigest);
        return this;
    }

    public void setEvidenceDigest(String evidenceDigest) {
        this.evidenceDigest = evidenceDigest;
    }

    public Instant getEvidenceSealedAt() {
        return this.evidenceSealedAt;
    }

    public QrVerification evidenceSealedAt(Instant evidenceSealedAt) {
        this.setEvidenceSealedAt(evidenceSealedAt);
        return this;
    }

    public void setEvidenceSealedAt(Instant evidenceSealedAt) {
        this.evidenceSealedAt = evidenceSealedAt;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public QrVerification liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public QrVerification operation(Operation operation) {
        this.setOperation(operation);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QrVerification)) {
            return false;
        }
        return getId() != null && getId().equals(((QrVerification) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "QrVerification{" +
            "id=" + getId() +
            ", form='" + getForm() + "'" +
            ", outcome='" + getOutcome() + "'" +
            ", verifiedOn='" + getVerifiedOn() + "'" +
            ", verifierId='" + getVerifierId() + "'" +
            ", evidenceDocumentId='" + getEvidenceDocumentId() + "'" +
            ", evidenceDigest='" + getEvidenceDigest() + "'" +
            ", evidenceSealedAt='" + getEvidenceSealedAt() + "'" +
            "}";
    }
}
