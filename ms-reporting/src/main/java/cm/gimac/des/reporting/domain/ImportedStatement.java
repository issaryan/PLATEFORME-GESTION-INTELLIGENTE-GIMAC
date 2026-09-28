package cm.gimac.des.reporting.domain;

import cm.gimac.des.reporting.domain.enumeration.StatementSource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * État déclaré par fichier : agrégats périodiques par participant, distinct
 * de son homologue de MS-RAPPROCHEMENT. Versionné sans écrasement ; le
 * résultat du contrôle de masquage (MaskingCheckResult du CC) est aplati.
 */
@Entity
@Table(name = "imported_statement")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ImportedStatement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private StatementSource source;

    /**
     * Institution (MS-PARTICIPANT).
     */
    @NotNull
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    /**
     * Période déclarée, par exemple 2026-08.
     */
    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[0-9]{4}(-[0-9]{2}|-W[0-9]{2}|-[0-9]{2}-[0-9]{2})?$")
    @Column(name = "period", length = 10, nullable = false)
    private String period;

    @NotNull
    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    /**
     * Déposant (MS-IAM).
     */
    @NotNull
    @Column(name = "importer_id", nullable = false)
    private UUID importerId;

    /**
     * SHA256 du fichier : clé d'idempotence.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "digest", length = 64, nullable = false, unique = true)
    private String digest;

    @NotNull
    @Column(name = "masking_passed", nullable = false)
    private Boolean maskingPassed;

    @NotNull
    @Min(value = 0)
    @Column(name = "rejected_rows", nullable = false)
    private Integer rejectedRows;

    @NotNull
    @Column(name = "masking_checked_at", nullable = false)
    private Instant maskingCheckedAt;

    @NotNull
    @Min(value = 0)
    @Column(name = "row_count", nullable = false)
    private Integer rowCount;

    /**
     * Original scellé : identifiant du document dans MS-DOCUMENT.
     */
    @NotNull
    @Column(name = "document_ref", nullable = false)
    private UUID documentRef;

    /**
     * Version courante pour le triplet (source, institution, période).
     */
    @NotNull
    @Column(name = "current_version", nullable = false)
    private Boolean currentVersion;

    /**
     * Version qui remplace cet état (versionnage sans écrasement).
     */
    @JsonIgnoreProperties(value = { "supersededBy", "importedStatement" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private ImportedStatement supersededBy;

    @JsonIgnoreProperties(value = { "supersededBy", "importedStatement" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "supersededBy")
    private ImportedStatement importedStatement;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ImportedStatement id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StatementSource getSource() {
        return this.source;
    }

    public ImportedStatement source(StatementSource source) {
        this.setSource(source);
        return this;
    }

    public void setSource(StatementSource source) {
        this.source = source;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public ImportedStatement institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getPeriod() {
        return this.period;
    }

    public ImportedStatement period(String period) {
        this.setPeriod(period);
        return this;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Instant getImportedAt() {
        return this.importedAt;
    }

    public ImportedStatement importedAt(Instant importedAt) {
        this.setImportedAt(importedAt);
        return this;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }

    public UUID getImporterId() {
        return this.importerId;
    }

    public ImportedStatement importerId(UUID importerId) {
        this.setImporterId(importerId);
        return this;
    }

    public void setImporterId(UUID importerId) {
        this.importerId = importerId;
    }

    public String getDigest() {
        return this.digest;
    }

    public ImportedStatement digest(String digest) {
        this.setDigest(digest);
        return this;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Boolean getMaskingPassed() {
        return this.maskingPassed;
    }

    public ImportedStatement maskingPassed(Boolean maskingPassed) {
        this.setMaskingPassed(maskingPassed);
        return this;
    }

    public void setMaskingPassed(Boolean maskingPassed) {
        this.maskingPassed = maskingPassed;
    }

    public Integer getRejectedRows() {
        return this.rejectedRows;
    }

    public ImportedStatement rejectedRows(Integer rejectedRows) {
        this.setRejectedRows(rejectedRows);
        return this;
    }

    public void setRejectedRows(Integer rejectedRows) {
        this.rejectedRows = rejectedRows;
    }

    public Instant getMaskingCheckedAt() {
        return this.maskingCheckedAt;
    }

    public ImportedStatement maskingCheckedAt(Instant maskingCheckedAt) {
        this.setMaskingCheckedAt(maskingCheckedAt);
        return this;
    }

    public void setMaskingCheckedAt(Instant maskingCheckedAt) {
        this.maskingCheckedAt = maskingCheckedAt;
    }

    public Integer getRowCount() {
        return this.rowCount;
    }

    public ImportedStatement rowCount(Integer rowCount) {
        this.setRowCount(rowCount);
        return this;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public UUID getDocumentRef() {
        return this.documentRef;
    }

    public ImportedStatement documentRef(UUID documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public Boolean getCurrentVersion() {
        return this.currentVersion;
    }

    public ImportedStatement currentVersion(Boolean currentVersion) {
        this.setCurrentVersion(currentVersion);
        return this;
    }

    public void setCurrentVersion(Boolean currentVersion) {
        this.currentVersion = currentVersion;
    }

    public ImportedStatement getSupersededBy() {
        return this.supersededBy;
    }

    public void setSupersededBy(ImportedStatement importedStatement) {
        this.supersededBy = importedStatement;
    }

    public ImportedStatement supersededBy(ImportedStatement importedStatement) {
        this.setSupersededBy(importedStatement);
        return this;
    }

    public ImportedStatement getImportedStatement() {
        return this.importedStatement;
    }

    public void setImportedStatement(ImportedStatement importedStatement) {
        if (this.importedStatement != null) {
            this.importedStatement.setSupersededBy(null);
        }
        if (importedStatement != null) {
            importedStatement.setSupersededBy(this);
        }
        this.importedStatement = importedStatement;
    }

    public ImportedStatement importedStatement(ImportedStatement importedStatement) {
        this.setImportedStatement(importedStatement);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImportedStatement)) {
            return false;
        }
        return getId() != null && getId().equals(((ImportedStatement) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ImportedStatement{" +
            "id=" + getId() +
            ", source='" + getSource() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", period='" + getPeriod() + "'" +
            ", importedAt='" + getImportedAt() + "'" +
            ", importerId='" + getImporterId() + "'" +
            ", digest='" + getDigest() + "'" +
            ", maskingPassed='" + getMaskingPassed() + "'" +
            ", rejectedRows=" + getRejectedRows() +
            ", maskingCheckedAt='" + getMaskingCheckedAt() + "'" +
            ", rowCount=" + getRowCount() +
            ", documentRef='" + getDocumentRef() + "'" +
            ", currentVersion='" + getCurrentVersion() + "'" +
            "}";
    }
}
