package cm.gimac.des.rapprochement.domain;

import cm.gimac.des.rapprochement.domain.enumeration.StatementFormat;
import cm.gimac.des.rapprochement.domain.enumeration.StatementSource;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * État importé, porté localement (CC §6.1.11.1). Idempotence par empreinte ;
 * aucun fichier ayant échoué au masquage n'est persisté.
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

    /**
     * Référence lisible, par exemple ETA-2026-0913-C.
     */
    @NotNull
    @Size(max = 30)
    @Column(name = "reference", length = 30, nullable = false, unique = true)
    private String reference;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private StatementSource source;

    @NotNull
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @NotNull
    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @NotNull
    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @NotNull
    @Column(name = "importer_id", nullable = false)
    private UUID importerId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private StatementFormat format;

    /**
     * SHA256(F) du fichier déposé.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "digest", length = 64, nullable = false, unique = true)
    private String digest;

    /**
     * Résultat du contrôle de masquage (MaskingCheckResult du CC, aplati).
     */
    @NotNull
    @Column(name = "masking_passed", nullable = false)
    private Boolean maskingPassed;

    @NotNull
    @Min(value = 0)
    @Column(name = "masking_rejected_lines", nullable = false)
    private Integer maskingRejectedLines;

    @NotNull
    @Column(name = "masking_checked_at", nullable = false)
    private Instant maskingCheckedAt;

    @NotNull
    @Min(value = 0)
    @Column(name = "line_count", nullable = false)
    private Integer lineCount;

    /**
     * Fichier original déposé dans MS-DOCUMENT (WORM).
     */
    @NotNull
    @Column(name = "document_ref", nullable = false)
    private UUID documentRef;

    /**
     * Fin de conservation des lignes en base (400 jours).
     */
    @NotNull
    @Column(name = "lines_retained_until", nullable = false)
    private LocalDate linesRetainedUntil;

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

    public String getReference() {
        return this.reference;
    }

    public ImportedStatement reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
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

    public LocalDate getBusinessDate() {
        return this.businessDate;
    }

    public ImportedStatement businessDate(LocalDate businessDate) {
        this.setBusinessDate(businessDate);
        return this;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
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

    public StatementFormat getFormat() {
        return this.format;
    }

    public ImportedStatement format(StatementFormat format) {
        this.setFormat(format);
        return this;
    }

    public void setFormat(StatementFormat format) {
        this.format = format;
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

    public Integer getMaskingRejectedLines() {
        return this.maskingRejectedLines;
    }

    public ImportedStatement maskingRejectedLines(Integer maskingRejectedLines) {
        this.setMaskingRejectedLines(maskingRejectedLines);
        return this;
    }

    public void setMaskingRejectedLines(Integer maskingRejectedLines) {
        this.maskingRejectedLines = maskingRejectedLines;
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

    public Integer getLineCount() {
        return this.lineCount;
    }

    public ImportedStatement lineCount(Integer lineCount) {
        this.setLineCount(lineCount);
        return this;
    }

    public void setLineCount(Integer lineCount) {
        this.lineCount = lineCount;
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

    public LocalDate getLinesRetainedUntil() {
        return this.linesRetainedUntil;
    }

    public ImportedStatement linesRetainedUntil(LocalDate linesRetainedUntil) {
        this.setLinesRetainedUntil(linesRetainedUntil);
        return this;
    }

    public void setLinesRetainedUntil(LocalDate linesRetainedUntil) {
        this.linesRetainedUntil = linesRetainedUntil;
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
            ", reference='" + getReference() + "'" +
            ", source='" + getSource() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", businessDate='" + getBusinessDate() + "'" +
            ", importedAt='" + getImportedAt() + "'" +
            ", importerId='" + getImporterId() + "'" +
            ", format='" + getFormat() + "'" +
            ", digest='" + getDigest() + "'" +
            ", maskingPassed='" + getMaskingPassed() + "'" +
            ", maskingRejectedLines=" + getMaskingRejectedLines() +
            ", maskingCheckedAt='" + getMaskingCheckedAt() + "'" +
            ", lineCount=" + getLineCount() +
            ", documentRef='" + getDocumentRef() + "'" +
            ", linesRetainedUntil='" + getLinesRetainedUntil() + "'" +
            "}";
    }
}
