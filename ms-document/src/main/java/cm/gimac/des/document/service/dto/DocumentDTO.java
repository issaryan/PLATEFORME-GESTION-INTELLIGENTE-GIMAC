package cm.gimac.des.document.service.dto;

import cm.gimac.des.document.domain.enumeration.DocumentState;
import cm.gimac.des.document.domain.enumeration.DocumentType;
import cm.gimac.des.document.domain.enumeration.DocumentVisibility;
import cm.gimac.des.document.domain.enumeration.RetentionMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.document.domain.Document} entity.
 */
@Schema(
    description = "Version d'une pièce : un objet distinct du dépôt WORM, avec sa propre\nempreinte et son propre verrou (RG-04, RG-05, RG-16). Les objets-valeurs du\nCC (RetentionPolicy, RetentionLock, ScanResult, MaskingVerdict) y sont aplatis."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DocumentDTO implements Serializable {

    private UUID id;

    @NotNull
    private DocumentType type;

    @NotNull
    @Size(max = 255)
    private String title;

    @NotNull
    @Min(value = 1)
    @Schema(
        description = "Numéro de version dans la lignée, à partir de 1 ; (lignée, version) unique.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer version;

    @NotNull
    @Schema(
        description = "Déposant : utilisateur, ou identifiant technique du service appelant.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID authorId;

    @Size(max = 50)
    @Schema(description = "Service appelant pour un dépôt interne, par exemple MS-CERTIFICATION.")
    private String callerService;

    @Schema(description = "Institution propriétaire (MS-PARTICIPANT) ; vide pour une pièce interne.")
    private UUID institutionId;

    @NotNull
    private DocumentVisibility visibility;

    @Size(max = 100)
    @Schema(description = "Service du DES propriétaire, pour la visibilité UN_SERVICE.")
    private String ownerUnit;

    @Size(max = 1000)
    @Schema(description = "Lecteurs désignés d'une pièce RESTREINT (valideur...), UUID séparés par des virgules (D-29).")
    private String restrictedReaderIds;

    @NotNull
    private Instant depositedAt;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "SHA-256 du contenu déposé (après masquage éventuel) ; absent tant que la pièce n'est pas scellée.")
    private String sha256;

    @Min(value = 0L)
    private Long sizeBytes;

    @NotNull
    @Size(max = 100)
    private String mediaType;

    @Size(max = 255)
    @Schema(description = "Clé d'objet propre à la version : type/lignée/vN ; absente avant le dépôt.")
    private String objectKey;

    @NotNull
    private DocumentState state;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^P([0-9]+Y)?([0-9]+M)?([0-9]+D)?$")
    @Schema(
        description = "Politique appliquée au dépôt, copiée du catalogue : durée ISO 8601.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String retentionPeriod;

    @NotNull
    private RetentionMode retentionMode;

    @NotNull
    @Schema(
        description = "Conservation légale (litige, inspection) : suspend l'échéance sans limite.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean legalHold;

    @Schema(description = "Échéance du verrou relue sur le dépôt objet (lockOf).")
    private Instant retainUntil;

    private Instant lockedAt;

    @Schema(description = "Verdict antiviral : vrai si sain.")
    private Boolean scanClean;

    @Size(max = 255)
    @Schema(description = "Nom de la signature détectée : jamais renvoyé au déposant.")
    private String scanSignatureName;

    @Size(max = 100)
    private String scanEngineVersion;

    private Instant scannedAt;

    @Min(value = 0)
    @Schema(description = "Verdict de masquage : séquences candidates de 13 à 19 chiffres.")
    private Integer maskingCandidates;

    @Min(value = 0)
    private Integer maskingLuhnValid;

    @Min(value = 0)
    private Integer maskingMasked;

    private Boolean maskingRejected;

    @Schema(description = "Déclaration PDF/A-3 vérifiée, pour une pièce générée.")
    private Boolean pdfaCompliant;

    @Size(max = 500)
    @Schema(description = "Motif de rejet, sans détail sur la signature détectée.")
    private String rejectionReason;

    private Instant sealedAt;

    private Instant purgedAt;

    @NotNull
    private DocumentLineageDTO lineage;

    @NotNull
    @Schema(description = "Entrée du catalogue appliquée au dépôt.")
    private DocumentTypePolicyDTO policy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getCallerService() {
        return callerService;
    }

    public void setCallerService(String callerService) {
        this.callerService = callerService;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public DocumentVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(DocumentVisibility visibility) {
        this.visibility = visibility;
    }

    public String getOwnerUnit() {
        return ownerUnit;
    }

    public void setOwnerUnit(String ownerUnit) {
        this.ownerUnit = ownerUnit;
    }

    public String getRestrictedReaderIds() {
        return restrictedReaderIds;
    }

    public void setRestrictedReaderIds(String restrictedReaderIds) {
        this.restrictedReaderIds = restrictedReaderIds;
    }

    public Instant getDepositedAt() {
        return depositedAt;
    }

    public void setDepositedAt(Instant depositedAt) {
        this.depositedAt = depositedAt;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public DocumentState getState() {
        return state;
    }

    public void setState(DocumentState state) {
        this.state = state;
    }

    public String getRetentionPeriod() {
        return retentionPeriod;
    }

    public void setRetentionPeriod(String retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public RetentionMode getRetentionMode() {
        return retentionMode;
    }

    public void setRetentionMode(RetentionMode retentionMode) {
        this.retentionMode = retentionMode;
    }

    public Boolean getLegalHold() {
        return legalHold;
    }

    public void setLegalHold(Boolean legalHold) {
        this.legalHold = legalHold;
    }

    public Instant getRetainUntil() {
        return retainUntil;
    }

    public void setRetainUntil(Instant retainUntil) {
        this.retainUntil = retainUntil;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public Boolean getScanClean() {
        return scanClean;
    }

    public void setScanClean(Boolean scanClean) {
        this.scanClean = scanClean;
    }

    public String getScanSignatureName() {
        return scanSignatureName;
    }

    public void setScanSignatureName(String scanSignatureName) {
        this.scanSignatureName = scanSignatureName;
    }

    public String getScanEngineVersion() {
        return scanEngineVersion;
    }

    public void setScanEngineVersion(String scanEngineVersion) {
        this.scanEngineVersion = scanEngineVersion;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(Instant scannedAt) {
        this.scannedAt = scannedAt;
    }

    public Integer getMaskingCandidates() {
        return maskingCandidates;
    }

    public void setMaskingCandidates(Integer maskingCandidates) {
        this.maskingCandidates = maskingCandidates;
    }

    public Integer getMaskingLuhnValid() {
        return maskingLuhnValid;
    }

    public void setMaskingLuhnValid(Integer maskingLuhnValid) {
        this.maskingLuhnValid = maskingLuhnValid;
    }

    public Integer getMaskingMasked() {
        return maskingMasked;
    }

    public void setMaskingMasked(Integer maskingMasked) {
        this.maskingMasked = maskingMasked;
    }

    public Boolean getMaskingRejected() {
        return maskingRejected;
    }

    public void setMaskingRejected(Boolean maskingRejected) {
        this.maskingRejected = maskingRejected;
    }

    public Boolean getPdfaCompliant() {
        return pdfaCompliant;
    }

    public void setPdfaCompliant(Boolean pdfaCompliant) {
        this.pdfaCompliant = pdfaCompliant;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
    }

    public Instant getPurgedAt() {
        return purgedAt;
    }

    public void setPurgedAt(Instant purgedAt) {
        this.purgedAt = purgedAt;
    }

    public DocumentLineageDTO getLineage() {
        return lineage;
    }

    public void setLineage(DocumentLineageDTO lineage) {
        this.lineage = lineage;
    }

    public DocumentTypePolicyDTO getPolicy() {
        return policy;
    }

    public void setPolicy(DocumentTypePolicyDTO policy) {
        this.policy = policy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentDTO)) {
            return false;
        }

        DocumentDTO documentDTO = (DocumentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, documentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DocumentDTO{" +
            "id='" + getId() + "'" +
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
            ", lineage=" + getLineage() +
            ", policy=" + getPolicy() +
            "}";
    }
}
