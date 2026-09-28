package cm.gimac.des.rapprochement.service.dto;

import cm.gimac.des.rapprochement.domain.enumeration.StatementFormat;
import cm.gimac.des.rapprochement.domain.enumeration.StatementSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.ImportedStatement} entity.
 */
@Schema(
    description = "État importé, porté localement (CC §6.1.11.1). Idempotence par empreinte ;\naucun fichier ayant échoué au masquage n'est persisté."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ImportedStatementDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Schema(description = "Référence lisible, par exemple ETA-2026-0913-C.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reference;

    @NotNull
    private StatementSource source;

    @NotNull
    private UUID institutionId;

    @NotNull
    private LocalDate businessDate;

    @NotNull
    private Instant importedAt;

    @NotNull
    private UUID importerId;

    @NotNull
    private StatementFormat format;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "SHA256(F) du fichier déposé.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String digest;

    @NotNull
    @Schema(
        description = "Résultat du contrôle de masquage (MaskingCheckResult du CC, aplati).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean maskingPassed;

    @NotNull
    @Min(value = 0)
    private Integer maskingRejectedLines;

    @NotNull
    private Instant maskingCheckedAt;

    @NotNull
    @Min(value = 0)
    private Integer lineCount;

    @NotNull
    @Schema(description = "Fichier original déposé dans MS-DOCUMENT (WORM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID documentRef;

    @NotNull
    @Schema(description = "Fin de conservation des lignes en base (400 jours).", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate linesRetainedUntil;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public StatementSource getSource() {
        return source;
    }

    public void setSource(StatementSource source) {
        this.source = source;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }

    public UUID getImporterId() {
        return importerId;
    }

    public void setImporterId(UUID importerId) {
        this.importerId = importerId;
    }

    public StatementFormat getFormat() {
        return format;
    }

    public void setFormat(StatementFormat format) {
        this.format = format;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Boolean getMaskingPassed() {
        return maskingPassed;
    }

    public void setMaskingPassed(Boolean maskingPassed) {
        this.maskingPassed = maskingPassed;
    }

    public Integer getMaskingRejectedLines() {
        return maskingRejectedLines;
    }

    public void setMaskingRejectedLines(Integer maskingRejectedLines) {
        this.maskingRejectedLines = maskingRejectedLines;
    }

    public Instant getMaskingCheckedAt() {
        return maskingCheckedAt;
    }

    public void setMaskingCheckedAt(Instant maskingCheckedAt) {
        this.maskingCheckedAt = maskingCheckedAt;
    }

    public Integer getLineCount() {
        return lineCount;
    }

    public void setLineCount(Integer lineCount) {
        this.lineCount = lineCount;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public LocalDate getLinesRetainedUntil() {
        return linesRetainedUntil;
    }

    public void setLinesRetainedUntil(LocalDate linesRetainedUntil) {
        this.linesRetainedUntil = linesRetainedUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImportedStatementDTO)) {
            return false;
        }

        ImportedStatementDTO importedStatementDTO = (ImportedStatementDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, importedStatementDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ImportedStatementDTO{" +
            "id='" + getId() + "'" +
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
