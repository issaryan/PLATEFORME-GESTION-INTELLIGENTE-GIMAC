package cm.gimac.des.document.service.dto;

import cm.gimac.des.document.domain.enumeration.DocumentType;
import cm.gimac.des.document.domain.enumeration.MaskingRule;
import cm.gimac.des.document.domain.enumeration.RetentionMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.document.domain.DocumentTypePolicy} entity.
 */
@Schema(
    description = "Catalogue versionné des types de pièces (DOC-02) : origine admise, règle de\nmasquage, rétention et contraintes d'admission (tableau 6.18 du CC).\nValeurs initiales : 10 ans en conformité, sauf ETAT_IMPORTE (5 ans) ;\nJUSTIFICATIF_ABSENCE : restreint, durée proposée de 5 ans en conformité,\nà confirmer par le RSSI (D-29)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DocumentTypePolicyDTO implements Serializable {

    private UUID id;

    @NotNull
    private DocumentType documentType;

    @NotNull
    @Min(value = 1)
    @Schema(description = "Version du catalogue ; le couple (type, version) est unique.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer policyVersion;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^P([0-9]+Y)?([0-9]+M)?([0-9]+D)?$")
    @Schema(description = "Durée de rétention au format ISO 8601, par exemple P10Y.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String retentionPeriod;

    @NotNull
    private RetentionMode retentionMode;

    @NotNull
    private MaskingRule maskingRule;

    @NotNull
    @Schema(
        description = "Vrai pour un type à accès restreint (justificatif d'absence, D-29).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean restrictedAccess;

    @NotNull
    @Schema(
        description = "Vrai si la déclaration PDF/A-3 est exigée (pièces générées, ISO 19005-3).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean pdfaRequired;

    @NotNull
    @Size(max = 500)
    @Schema(description = "Types de média admis, séparés par des virgules.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String allowedMediaTypes;

    @NotNull
    @Min(value = 1L)
    private Long maxSizeBytes;

    @Size(max = 500)
    @Schema(description = "Services appelants autorisés (liste mTLS), séparés par des virgules ; vide pour un dépôt utilisateur seul.")
    private String allowedCallers;

    @NotNull
    private Instant effectiveFrom;

    @NotNull
    private Boolean active;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public Integer getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(Integer policyVersion) {
        this.policyVersion = policyVersion;
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

    public MaskingRule getMaskingRule() {
        return maskingRule;
    }

    public void setMaskingRule(MaskingRule maskingRule) {
        this.maskingRule = maskingRule;
    }

    public Boolean getRestrictedAccess() {
        return restrictedAccess;
    }

    public void setRestrictedAccess(Boolean restrictedAccess) {
        this.restrictedAccess = restrictedAccess;
    }

    public Boolean getPdfaRequired() {
        return pdfaRequired;
    }

    public void setPdfaRequired(Boolean pdfaRequired) {
        this.pdfaRequired = pdfaRequired;
    }

    public String getAllowedMediaTypes() {
        return allowedMediaTypes;
    }

    public void setAllowedMediaTypes(String allowedMediaTypes) {
        this.allowedMediaTypes = allowedMediaTypes;
    }

    public Long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(Long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public String getAllowedCallers() {
        return allowedCallers;
    }

    public void setAllowedCallers(String allowedCallers) {
        this.allowedCallers = allowedCallers;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentTypePolicyDTO)) {
            return false;
        }

        DocumentTypePolicyDTO documentTypePolicyDTO = (DocumentTypePolicyDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, documentTypePolicyDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DocumentTypePolicyDTO{" +
            "id='" + getId() + "'" +
            ", documentType='" + getDocumentType() + "'" +
            ", policyVersion=" + getPolicyVersion() +
            ", retentionPeriod='" + getRetentionPeriod() + "'" +
            ", retentionMode='" + getRetentionMode() + "'" +
            ", maskingRule='" + getMaskingRule() + "'" +
            ", restrictedAccess='" + getRestrictedAccess() + "'" +
            ", pdfaRequired='" + getPdfaRequired() + "'" +
            ", allowedMediaTypes='" + getAllowedMediaTypes() + "'" +
            ", maxSizeBytes=" + getMaxSizeBytes() +
            ", allowedCallers='" + getAllowedCallers() + "'" +
            ", effectiveFrom='" + getEffectiveFrom() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
