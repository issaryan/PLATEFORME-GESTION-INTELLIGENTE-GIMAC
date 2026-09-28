package cm.gimac.des.document.domain;

import cm.gimac.des.document.domain.enumeration.DocumentState;
import cm.gimac.des.document.domain.enumeration.DocumentType;
import cm.gimac.des.document.domain.enumeration.DocumentVisibility;
import cm.gimac.des.document.domain.enumeration.RetentionMode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Version d'une pièce : un objet distinct du dépôt WORM, avec sa propre
 * empreinte et son propre verrou (RG-04, RG-05, RG-16). Les objets-valeurs du
 * CC (RetentionPolicy, RetentionLock, ScanResult, MaskingVerdict) y sont aplatis.
 */
@Entity
@Table(name = "document")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DocumentType type;

    @NotNull
    @Size(max = 255)
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /**
     * Numéro de version dans la lignée, à partir de 1 ; (lignée, version) unique.
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Déposant : utilisateur, ou identifiant technique du service appelant.
     */
    @NotNull
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    /**
     * Service appelant pour un dépôt interne, par exemple MS-CERTIFICATION.
     */
    @Size(max = 50)
    @Column(name = "caller_service", length = 50)
    private String callerService;

    /**
     * Institution propriétaire (MS-PARTICIPANT) ; vide pour une pièce interne.
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private DocumentVisibility visibility;

    /**
     * Service du DES propriétaire, pour la visibilité UN_SERVICE.
     */
    @Size(max = 100)
    @Column(name = "owner_unit", length = 100)
    private String ownerUnit;

    /**
     * Lecteurs désignés d'une pièce RESTREINT (valideur...), UUID séparés par des virgules (D-29).
     */
    @Size(max = 1000)
    @Column(name = "restricted_reader_ids", length = 1000)
    private String restrictedReaderIds;

    @NotNull
    @Column(name = "deposited_at", nullable = false)
    private Instant depositedAt;

    /**
     * SHA-256 du contenu déposé (après masquage éventuel) ; absent tant que la pièce n'est pas scellée.
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "sha_256", length = 64)
    private String sha256;

    @Min(value = 0L)
    @Column(name = "size_bytes")
    private Long sizeBytes;

    @NotNull
    @Size(max = 100)
    @Column(name = "media_type", length = 100, nullable = false)
    private String mediaType;

    /**
     * Clé d'objet propre à la version : type/lignée/vN ; absente avant le dépôt.
     */
    @Size(max = 255)
    @Column(name = "object_key", length = 255, unique = true)
    private String objectKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private DocumentState state;

    /**
     * Politique appliquée au dépôt, copiée du catalogue : durée ISO 8601.
     */
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^P([0-9]+Y)?([0-9]+M)?([0-9]+D)?$")
    @Column(name = "retention_period", length = 20, nullable = false)
    private String retentionPeriod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "retention_mode", nullable = false)
    private RetentionMode retentionMode;

    /**
     * Conservation légale (litige, inspection) : suspend l'échéance sans limite.
     */
    @NotNull
    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold;

    /**
     * Échéance du verrou relue sur le dépôt objet (lockOf).
     */
    @Column(name = "retain_until")
    private Instant retainUntil;

    @Column(name = "locked_at")
    private Instant lockedAt;

    /**
     * Verdict antiviral : vrai si sain.
     */
    @Column(name = "scan_clean")
    private Boolean scanClean;

    /**
     * Nom de la signature détectée : jamais renvoyé au déposant.
     */
    @Size(max = 255)
    @Column(name = "scan_signature_name", length = 255)
    private String scanSignatureName;

    @Size(max = 100)
    @Column(name = "scan_engine_version", length = 100)
    private String scanEngineVersion;

    @Column(name = "scanned_at")
    private Instant scannedAt;

    /**
     * Verdict de masquage : séquences candidates de 13 à 19 chiffres.
     */
    @Min(value = 0)
    @Column(name = "masking_candidates")
    private Integer maskingCandidates;

    @Min(value = 0)
    @Column(name = "masking_luhn_valid")
    private Integer maskingLuhnValid;

    @Min(value = 0)
    @Column(name = "masking_masked")
    private Integer maskingMasked;

    @Column(name = "masking_rejected")
    private Boolean maskingRejected;

    /**
     * Déclaration PDF/A-3 vérifiée, pour une pièce générée.
     */
    @Column(name = "pdfa_compliant")
    private Boolean pdfaCompliant;

    /**
     * Motif de rejet, sans détail sur la signature détectée.
     */
    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "sealed_at")
    private Instant sealedAt;

    @Column(name = "purged_at")
    private Instant purgedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "currentDocument" }, allowSetters = true)
    private DocumentLineage lineage;

    /**
     * Entrée du catalogue appliquée au dépôt.
     */
    @ManyToOne(optional = false)
    @NotNull
    private DocumentTypePolicy policy;

    @JsonIgnoreProperties(value = { "currentDocument" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "currentDocument")
    private DocumentLineage documentLineage;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Document id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DocumentType getType() {
        return this.type;
    }

    public Document type(DocumentType type) {
        this.setType(type);
        return this;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getTitle() {
        return this.title;
    }

    public Document title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getVersion() {
        return this.version;
    }

    public Document version(Integer version) {
        this.setVersion(version);
        return this;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public UUID getAuthorId() {
        return this.authorId;
    }

    public Document authorId(UUID authorId) {
        this.setAuthorId(authorId);
        return this;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getCallerService() {
        return this.callerService;
    }

    public Document callerService(String callerService) {
        this.setCallerService(callerService);
        return this;
    }

    public void setCallerService(String callerService) {
        this.callerService = callerService;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Document institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public DocumentVisibility getVisibility() {
        return this.visibility;
    }

    public Document visibility(DocumentVisibility visibility) {
        this.setVisibility(visibility);
        return this;
    }

    public void setVisibility(DocumentVisibility visibility) {
        this.visibility = visibility;
    }

    public String getOwnerUnit() {
        return this.ownerUnit;
    }

    public Document ownerUnit(String ownerUnit) {
        this.setOwnerUnit(ownerUnit);
        return this;
    }

    public void setOwnerUnit(String ownerUnit) {
        this.ownerUnit = ownerUnit;
    }

    public String getRestrictedReaderIds() {
        return this.restrictedReaderIds;
    }

    public Document restrictedReaderIds(String restrictedReaderIds) {
        this.setRestrictedReaderIds(restrictedReaderIds);
        return this;
    }

    public void setRestrictedReaderIds(String restrictedReaderIds) {
        this.restrictedReaderIds = restrictedReaderIds;
    }

    public Instant getDepositedAt() {
        return this.depositedAt;
    }

    public Document depositedAt(Instant depositedAt) {
        this.setDepositedAt(depositedAt);
        return this;
    }

    public void setDepositedAt(Instant depositedAt) {
        this.depositedAt = depositedAt;
    }

    public String getSha256() {
        return this.sha256;
    }

    public Document sha256(String sha256) {
        this.setSha256(sha256);
        return this;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Long getSizeBytes() {
        return this.sizeBytes;
    }

    public Document sizeBytes(Long sizeBytes) {
        this.setSizeBytes(sizeBytes);
        return this;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public Document mediaType(String mediaType) {
        this.setMediaType(mediaType);
        return this;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getObjectKey() {
        return this.objectKey;
    }

    public Document objectKey(String objectKey) {
        this.setObjectKey(objectKey);
        return this;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public DocumentState getState() {
        return this.state;
    }

    public Document state(DocumentState state) {
        this.setState(state);
        return this;
    }

    public void setState(DocumentState state) {
        this.state = state;
    }

    public String getRetentionPeriod() {
        return this.retentionPeriod;
    }

    public Document retentionPeriod(String retentionPeriod) {
        this.setRetentionPeriod(retentionPeriod);
        return this;
    }

    public void setRetentionPeriod(String retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public RetentionMode getRetentionMode() {
        return this.retentionMode;
    }

    public Document retentionMode(RetentionMode retentionMode) {
        this.setRetentionMode(retentionMode);
        return this;
    }

    public void setRetentionMode(RetentionMode retentionMode) {
        this.retentionMode = retentionMode;
    }

    public Boolean getLegalHold() {
        return this.legalHold;
    }

    public Document legalHold(Boolean legalHold) {
        this.setLegalHold(legalHold);
        return this;
    }

    public void setLegalHold(Boolean legalHold) {
        this.legalHold = legalHold;
    }

    public Instant getRetainUntil() {
        return this.retainUntil;
    }

    public Document retainUntil(Instant retainUntil) {
        this.setRetainUntil(retainUntil);
        return this;
    }

    public void setRetainUntil(Instant retainUntil) {
        this.retainUntil = retainUntil;
    }

    public Instant getLockedAt() {
        return this.lockedAt;
    }

    public Document lockedAt(Instant lockedAt) {
        this.setLockedAt(lockedAt);
        return this;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public Boolean getScanClean() {
        return this.scanClean;
    }

    public Document scanClean(Boolean scanClean) {
        this.setScanClean(scanClean);
        return this;
    }

    public void setScanClean(Boolean scanClean) {
        this.scanClean = scanClean;
    }

    public String getScanSignatureName() {
        return this.scanSignatureName;
    }

    public Document scanSignatureName(String scanSignatureName) {
        this.setScanSignatureName(scanSignatureName);
        return this;
    }

    public void setScanSignatureName(String scanSignatureName) {
        this.scanSignatureName = scanSignatureName;
    }

    public String getScanEngineVersion() {
        return this.scanEngineVersion;
    }

    public Document scanEngineVersion(String scanEngineVersion) {
        this.setScanEngineVersion(scanEngineVersion);
        return this;
    }

    public void setScanEngineVersion(String scanEngineVersion) {
        this.scanEngineVersion = scanEngineVersion;
    }

    public Instant getScannedAt() {
        return this.scannedAt;
    }

    public Document scannedAt(Instant scannedAt) {
        this.setScannedAt(scannedAt);
        return this;
    }

    public void setScannedAt(Instant scannedAt) {
        this.scannedAt = scannedAt;
    }

    public Integer getMaskingCandidates() {
        return this.maskingCandidates;
    }

    public Document maskingCandidates(Integer maskingCandidates) {
        this.setMaskingCandidates(maskingCandidates);
        return this;
    }

    public void setMaskingCandidates(Integer maskingCandidates) {
        this.maskingCandidates = maskingCandidates;
    }

    public Integer getMaskingLuhnValid() {
        return this.maskingLuhnValid;
    }

    public Document maskingLuhnValid(Integer maskingLuhnValid) {
        this.setMaskingLuhnValid(maskingLuhnValid);
        return this;
    }

    public void setMaskingLuhnValid(Integer maskingLuhnValid) {
        this.maskingLuhnValid = maskingLuhnValid;
    }

    public Integer getMaskingMasked() {
        return this.maskingMasked;
    }

    public Document maskingMasked(Integer maskingMasked) {
        this.setMaskingMasked(maskingMasked);
        return this;
    }

    public void setMaskingMasked(Integer maskingMasked) {
        this.maskingMasked = maskingMasked;
    }

    public Boolean getMaskingRejected() {
        return this.maskingRejected;
    }

    public Document maskingRejected(Boolean maskingRejected) {
        this.setMaskingRejected(maskingRejected);
        return this;
    }

    public void setMaskingRejected(Boolean maskingRejected) {
        this.maskingRejected = maskingRejected;
    }

    public Boolean getPdfaCompliant() {
        return this.pdfaCompliant;
    }

    public Document pdfaCompliant(Boolean pdfaCompliant) {
        this.setPdfaCompliant(pdfaCompliant);
        return this;
    }

    public void setPdfaCompliant(Boolean pdfaCompliant) {
        this.pdfaCompliant = pdfaCompliant;
    }

    public String getRejectionReason() {
        return this.rejectionReason;
    }

    public Document rejectionReason(String rejectionReason) {
        this.setRejectionReason(rejectionReason);
        return this;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getSealedAt() {
        return this.sealedAt;
    }

    public Document sealedAt(Instant sealedAt) {
        this.setSealedAt(sealedAt);
        return this;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public Instant getPurgedAt() {
        return this.purgedAt;
    }

    public Document purgedAt(Instant purgedAt) {
        this.setPurgedAt(purgedAt);
        return this;
    }

    public void setPurgedAt(Instant purgedAt) {
        this.purgedAt = purgedAt;
    }

    public DocumentLineage getLineage() {
        return this.lineage;
    }

    public void setLineage(DocumentLineage documentLineage) {
        this.lineage = documentLineage;
    }

    public Document lineage(DocumentLineage documentLineage) {
        this.setLineage(documentLineage);
        return this;
    }

    public DocumentTypePolicy getPolicy() {
        return this.policy;
    }

    public void setPolicy(DocumentTypePolicy documentTypePolicy) {
        this.policy = documentTypePolicy;
    }

    public Document policy(DocumentTypePolicy documentTypePolicy) {
        this.setPolicy(documentTypePolicy);
        return this;
    }

    public DocumentLineage getDocumentLineage() {
        return this.documentLineage;
    }

    public void setDocumentLineage(DocumentLineage documentLineage) {
        if (this.documentLineage != null) {
            this.documentLineage.setCurrentDocument(null);
        }
        if (documentLineage != null) {
            documentLineage.setCurrentDocument(this);
        }
        this.documentLineage = documentLineage;
    }

    public Document documentLineage(DocumentLineage documentLineage) {
        this.setDocumentLineage(documentLineage);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document)) {
            return false;
        }
        return getId() != null && getId().equals(((Document) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Document{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", title='" + getTitle() + "'" +
            ", version=" + getVersion() +
            ", authorId='" + getAuthorId() + "'" +
            ", callerService='" + getCallerService() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", visibility='" + getVisibility() + "'" +
            ", ownerUnit='" + getOwnerUnit() + "'" +
            ", restrictedReaderIds='" + getRestrictedReaderIds() + "'" +
            ", depositedAt='" + getDepositedAt() + "'" +
            ", sha256='" + getSha256() + "'" +
            ", sizeBytes=" + getSizeBytes() +
            ", mediaType='" + getMediaType() + "'" +
            ", objectKey='" + getObjectKey() + "'" +
            ", state='" + getState() + "'" +
            ", retentionPeriod='" + getRetentionPeriod() + "'" +
            ", retentionMode='" + getRetentionMode() + "'" +
            ", legalHold='" + getLegalHold() + "'" +
            ", retainUntil='" + getRetainUntil() + "'" +
            ", lockedAt='" + getLockedAt() + "'" +
            ", scanClean='" + getScanClean() + "'" +
            ", scanSignatureName='" + getScanSignatureName() + "'" +
            ", scanEngineVersion='" + getScanEngineVersion() + "'" +
            ", scannedAt='" + getScannedAt() + "'" +
            ", maskingCandidates=" + getMaskingCandidates() +
            ", maskingLuhnValid=" + getMaskingLuhnValid() +
            ", maskingMasked=" + getMaskingMasked() +
            ", maskingRejected='" + getMaskingRejected() + "'" +
            ", pdfaCompliant='" + getPdfaCompliant() + "'" +
            ", rejectionReason='" + getRejectionReason() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            ", purgedAt='" + getPurgedAt() + "'" +
            "}";
    }
}
