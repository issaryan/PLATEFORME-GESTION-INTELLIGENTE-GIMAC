package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.EvidenceKind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Pièce du dossier, référencée par identifiant et empreinte ; l'empreinte est
 * recontrôlée contre MS-DOCUMENT avant autorisation (algorithme 6.36, étape 4).
 */
@Entity
@Table(name = "evidence_item")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvidenceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private EvidenceKind kind;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "digest", length = 64, nullable = false)
    private String digest;

    @Column(name = "sealed_at")
    private Instant sealedAt;

    /**
     * Confirmation reçue par document.document-sealed.v1.
     */
    @NotNull
    @Column(name = "seal_confirmed", nullable = false)
    private Boolean sealConfirmed;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @NotNull
    @Column(name = "attached_by", nullable = false)
    private UUID attachedBy;

    @NotNull
    @Column(name = "attached_at", nullable = false)
    private Instant attachedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "changeRecord", "attestation", "commissioningReport", "quituses", "pieces" }, allowSetters = true)
    private CommissioningFile file;

    @JsonIgnoreProperties(value = { "changeRecord", "attestation", "commissioningReport", "quituses", "pieces" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "commissioningReport")
    private CommissioningFile commissioningFile;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public EvidenceItem id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentId() {
        return this.documentId;
    }

    public EvidenceItem documentId(UUID documentId) {
        this.setDocumentId(documentId);
        return this;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public EvidenceKind getKind() {
        return this.kind;
    }

    public EvidenceItem kind(EvidenceKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(EvidenceKind kind) {
        this.kind = kind;
    }

    public String getDigest() {
        return this.digest;
    }

    public EvidenceItem digest(String digest) {
        this.setDigest(digest);
        return this;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Instant getSealedAt() {
        return this.sealedAt;
    }

    public EvidenceItem sealedAt(Instant sealedAt) {
        this.setSealedAt(sealedAt);
        return this;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public Boolean getSealConfirmed() {
        return this.sealConfirmed;
    }

    public EvidenceItem sealConfirmed(Boolean sealConfirmed) {
        this.setSealConfirmed(sealConfirmed);
        return this;
    }

    public void setSealConfirmed(Boolean sealConfirmed) {
        this.sealConfirmed = sealConfirmed;
    }

    public Instant getLastVerifiedAt() {
        return this.lastVerifiedAt;
    }

    public EvidenceItem lastVerifiedAt(Instant lastVerifiedAt) {
        this.setLastVerifiedAt(lastVerifiedAt);
        return this;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public UUID getAttachedBy() {
        return this.attachedBy;
    }

    public EvidenceItem attachedBy(UUID attachedBy) {
        this.setAttachedBy(attachedBy);
        return this;
    }

    public void setAttachedBy(UUID attachedBy) {
        this.attachedBy = attachedBy;
    }

    public Instant getAttachedAt() {
        return this.attachedAt;
    }

    public EvidenceItem attachedAt(Instant attachedAt) {
        this.setAttachedAt(attachedAt);
        return this;
    }

    public void setAttachedAt(Instant attachedAt) {
        this.attachedAt = attachedAt;
    }

    public CommissioningFile getFile() {
        return this.file;
    }

    public void setFile(CommissioningFile commissioningFile) {
        this.file = commissioningFile;
    }

    public EvidenceItem file(CommissioningFile commissioningFile) {
        this.setFile(commissioningFile);
        return this;
    }

    public CommissioningFile getCommissioningFile() {
        return this.commissioningFile;
    }

    public void setCommissioningFile(CommissioningFile commissioningFile) {
        if (this.commissioningFile != null) {
            this.commissioningFile.setCommissioningReport(null);
        }
        if (commissioningFile != null) {
            commissioningFile.setCommissioningReport(this);
        }
        this.commissioningFile = commissioningFile;
    }

    public EvidenceItem commissioningFile(CommissioningFile commissioningFile) {
        this.setCommissioningFile(commissioningFile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvidenceItem)) {
            return false;
        }
        return getId() != null && getId().equals(((EvidenceItem) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvidenceItem{" +
            "id=" + getId() +
            ", documentId='" + getDocumentId() + "'" +
            ", kind='" + getKind() + "'" +
            ", digest='" + getDigest() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            ", sealConfirmed='" + getSealConfirmed() + "'" +
            ", lastVerifiedAt='" + getLastVerifiedAt() + "'" +
            ", attachedBy='" + getAttachedBy() + "'" +
            ", attachedAt='" + getAttachedAt() + "'" +
            "}";
    }
}
