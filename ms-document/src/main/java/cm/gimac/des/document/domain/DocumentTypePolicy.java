package cm.gimac.des.document.domain;

import cm.gimac.des.document.domain.enumeration.DocumentType;
import cm.gimac.des.document.domain.enumeration.MaskingRule;
import cm.gimac.des.document.domain.enumeration.RetentionMode;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Catalogue versionné des types de pièces (DOC-02) : origine admise, règle de
 * masquage, rétention et contraintes d'admission (tableau 6.18 du CC).
 * Valeurs initiales : 10 ans en conformité, sauf ETAT_IMPORTE (5 ans) ;
 * JUSTIFICATIF_ABSENCE : restreint, durée proposée de 5 ans en conformité,
 * à confirmer par le RSSI (D-29).
 */
@Entity
@Table(name = "document_type_policy")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DocumentTypePolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    /**
     * Version du catalogue ; le couple (type, version) est unique.
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion;

    /**
     * Durée de rétention au format ISO 8601, par exemple P10Y.
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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "masking_rule", nullable = false)
    private MaskingRule maskingRule;

    /**
     * Vrai pour un type à accès restreint (justificatif d'absence, D-29).
     */
    @NotNull
    @Column(name = "restricted_access", nullable = false)
    private Boolean restrictedAccess;

    /**
     * Vrai si la déclaration PDF/A-3 est exigée (pièces générées, ISO 19005-3).
     */
    @NotNull
    @Column(name = "pdfa_required", nullable = false)
    private Boolean pdfaRequired;

    /**
     * Types de média admis, séparés par des virgules.
     */
    @NotNull
    @Size(max = 500)
    @Column(name = "allowed_media_types", length = 500, nullable = false)
    private String allowedMediaTypes;

    @NotNull
    @Min(value = 1L)
    @Column(name = "max_size_bytes", nullable = false)
    private Long maxSizeBytes;

    /**
     * Services appelants autorisés (liste mTLS), séparés par des virgules ; vide pour un dépôt utilisateur seul.
     */
    @Size(max = 500)
    @Column(name = "allowed_callers", length = 500)
    private String allowedCallers;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public DocumentTypePolicy id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DocumentType getDocumentType() {
        return this.documentType;
    }

    public DocumentTypePolicy documentType(DocumentType documentType) {
        this.setDocumentType(documentType);
        return this;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public Integer getPolicyVersion() {
        return this.policyVersion;
    }

    public DocumentTypePolicy policyVersion(Integer policyVersion) {
        this.setPolicyVersion(policyVersion);
        return this;
    }

    public void setPolicyVersion(Integer policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getRetentionPeriod() {
        return this.retentionPeriod;
    }

    public DocumentTypePolicy retentionPeriod(String retentionPeriod) {
        this.setRetentionPeriod(retentionPeriod);
        return this;
    }

    public void setRetentionPeriod(String retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public RetentionMode getRetentionMode() {
        return this.retentionMode;
    }

    public DocumentTypePolicy retentionMode(RetentionMode retentionMode) {
        this.setRetentionMode(retentionMode);
        return this;
    }

    public void setRetentionMode(RetentionMode retentionMode) {
        this.retentionMode = retentionMode;
    }

    public MaskingRule getMaskingRule() {
        return this.maskingRule;
    }

    public DocumentTypePolicy maskingRule(MaskingRule maskingRule) {
        this.setMaskingRule(maskingRule);
        return this;
    }

    public void setMaskingRule(MaskingRule maskingRule) {
        this.maskingRule = maskingRule;
    }

    public Boolean getRestrictedAccess() {
        return this.restrictedAccess;
    }

    public DocumentTypePolicy restrictedAccess(Boolean restrictedAccess) {
        this.setRestrictedAccess(restrictedAccess);
        return this;
    }

    public void setRestrictedAccess(Boolean restrictedAccess) {
        this.restrictedAccess = restrictedAccess;
    }

    public Boolean getPdfaRequired() {
        return this.pdfaRequired;
    }

    public DocumentTypePolicy pdfaRequired(Boolean pdfaRequired) {
        this.setPdfaRequired(pdfaRequired);
        return this;
    }

    public void setPdfaRequired(Boolean pdfaRequired) {
        this.pdfaRequired = pdfaRequired;
    }

    public String getAllowedMediaTypes() {
        return this.allowedMediaTypes;
    }

    public DocumentTypePolicy allowedMediaTypes(String allowedMediaTypes) {
        this.setAllowedMediaTypes(allowedMediaTypes);
        return this;
    }

    public void setAllowedMediaTypes(String allowedMediaTypes) {
        this.allowedMediaTypes = allowedMediaTypes;
    }

    public Long getMaxSizeBytes() {
        return this.maxSizeBytes;
    }

    public DocumentTypePolicy maxSizeBytes(Long maxSizeBytes) {
        this.setMaxSizeBytes(maxSizeBytes);
        return this;
    }

    public void setMaxSizeBytes(Long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public String getAllowedCallers() {
        return this.allowedCallers;
    }

    public DocumentTypePolicy allowedCallers(String allowedCallers) {
        this.setAllowedCallers(allowedCallers);
        return this;
    }

    public void setAllowedCallers(String allowedCallers) {
        this.allowedCallers = allowedCallers;
    }

    public Instant getEffectiveFrom() {
        return this.effectiveFrom;
    }

    public DocumentTypePolicy effectiveFrom(Instant effectiveFrom) {
        this.setEffectiveFrom(effectiveFrom);
        return this;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Boolean getActive() {
        return this.active;
    }

    public DocumentTypePolicy active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentTypePolicy)) {
            return false;
        }
        return getId() != null && getId().equals(((DocumentTypePolicy) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DocumentTypePolicy{" +
            "id=" + getId() +
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
