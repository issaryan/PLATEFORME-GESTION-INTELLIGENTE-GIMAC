package cm.gimac.des.certification.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Attestation de certification PDF/A-3 signée PAdES B-LTA avec la clé de cachet
 * de la plateforme, déposée en WORM dans MS-DOCUMENT (algorithme 6.38).
 */
@Entity
@Table(name = "certification_attestation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CertificationAttestation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Numéro d'attestation, par exemple ATT-CERT-2026-0009.
     */
    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^ATT-CERT-[0-9]{4}-[0-9]{4,}$")
    @Column(name = "attestation_number", length = 30, nullable = false, unique = true)
    private String attestationNumber;

    /**
     * Pièce dans MS-DOCUMENT (référence, BNF-04).
     */
    @NotNull
    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "pdf_digest", length = 64, nullable = false)
    private String pdfDigest;

    /**
     * Niveau de signature, PAdES B-LTA.
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "signature_level", length = 20, nullable = false)
    private String signatureLevel;

    /**
     * Pièces jointes embarquées : liste JSON (nom, type de média, relation, empreinte).
     */
    @Lob
    @Column(name = "attachments", nullable = false)
    private String attachments;

    @NotNull
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    /**
     * Confirmation reçue par document.document-sealed.v1 (condition C3).
     */
    @NotNull
    @Column(name = "seal_confirmed", nullable = false)
    private Boolean sealConfirmed;

    @JsonIgnoreProperties(value = { "changeRecord", "attestation", "commissioningReport", "quituses", "pieces" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "attestation")
    private CommissioningFile commissioningFile;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CertificationAttestation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAttestationNumber() {
        return this.attestationNumber;
    }

    public CertificationAttestation attestationNumber(String attestationNumber) {
        this.setAttestationNumber(attestationNumber);
        return this;
    }

    public void setAttestationNumber(String attestationNumber) {
        this.attestationNumber = attestationNumber;
    }

    public UUID getDocumentId() {
        return this.documentId;
    }

    public CertificationAttestation documentId(UUID documentId) {
        this.setDocumentId(documentId);
        return this;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getPdfDigest() {
        return this.pdfDigest;
    }

    public CertificationAttestation pdfDigest(String pdfDigest) {
        this.setPdfDigest(pdfDigest);
        return this;
    }

    public void setPdfDigest(String pdfDigest) {
        this.pdfDigest = pdfDigest;
    }

    public String getSignatureLevel() {
        return this.signatureLevel;
    }

    public CertificationAttestation signatureLevel(String signatureLevel) {
        this.setSignatureLevel(signatureLevel);
        return this;
    }

    public void setSignatureLevel(String signatureLevel) {
        this.signatureLevel = signatureLevel;
    }

    public String getAttachments() {
        return this.attachments;
    }

    public CertificationAttestation attachments(String attachments) {
        this.setAttachments(attachments);
        return this;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    public Instant getIssuedAt() {
        return this.issuedAt;
    }

    public CertificationAttestation issuedAt(Instant issuedAt) {
        this.setIssuedAt(issuedAt);
        return this;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Boolean getSealConfirmed() {
        return this.sealConfirmed;
    }

    public CertificationAttestation sealConfirmed(Boolean sealConfirmed) {
        this.setSealConfirmed(sealConfirmed);
        return this;
    }

    public void setSealConfirmed(Boolean sealConfirmed) {
        this.sealConfirmed = sealConfirmed;
    }

    public CommissioningFile getCommissioningFile() {
        return this.commissioningFile;
    }

    public void setCommissioningFile(CommissioningFile commissioningFile) {
        if (this.commissioningFile != null) {
            this.commissioningFile.setAttestation(null);
        }
        if (commissioningFile != null) {
            commissioningFile.setAttestation(this);
        }
        this.commissioningFile = commissioningFile;
    }

    public CertificationAttestation commissioningFile(CommissioningFile commissioningFile) {
        this.setCommissioningFile(commissioningFile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificationAttestation)) {
            return false;
        }
        return getId() != null && getId().equals(((CertificationAttestation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CertificationAttestation{" +
            "id=" + getId() +
            ", attestationNumber='" + getAttestationNumber() + "'" +
            ", documentId='" + getDocumentId() + "'" +
            ", pdfDigest='" + getPdfDigest() + "'" +
            ", signatureLevel='" + getSignatureLevel() + "'" +
            ", attachments='" + getAttachments() + "'" +
            ", issuedAt='" + getIssuedAt() + "'" +
            ", sealConfirmed='" + getSealConfirmed() + "'" +
            "}";
    }
}
