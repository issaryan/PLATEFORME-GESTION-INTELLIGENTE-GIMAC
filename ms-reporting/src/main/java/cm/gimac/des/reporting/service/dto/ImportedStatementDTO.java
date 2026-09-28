package cm.gimac.des.reporting.service.dto;

import cm.gimac.des.reporting.domain.enumeration.StatementSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.ImportedStatement} entity.
 */
@Schema(
    description = "État déclaré par fichier : agrégats périodiques par participant, distinct\nde son homologue de MS-RAPPROCHEMENT. Versionné sans écrasement ; le\nrésultat du contrôle de masquage (MaskingCheckResult du CC) est aplati."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ImportedStatementDTO implements Serializable {

    private UUID id;

    @NotNull
    private StatementSource source;

    @NotNull
    @Schema(description = "Institution (MS-PARTICIPANT).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID institutionId;

    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[0-9]{4}(-[0-9]{2}|-W[0-9]{2}|-[0-9]{2}-[0-9]{2})?$")
    @Schema(description = "Période déclarée, par exemple 2026-08.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String period;

    @NotNull
    private Instant importedAt;

    @NotNull
    @Schema(description = "Déposant (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID importerId;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "SHA256 du fichier : clé d'idempotence.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String digest;

    @NotNull
    private Boolean maskingPassed;

    @NotNull
    @Min(value = 0)
    private Integer rejectedRows;

    @NotNull
    private Instant maskingCheckedAt;

    @NotNull
    @Min(value = 0)
    private Integer rowCount;

    @NotNull
    @Schema(description = "Original scellé : identifiant du document dans MS-DOCUMENT.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID documentRef;

    @NotNull
    @Schema(description = "Version courante pour le triplet (source, institution, période).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean currentVersion;

    @Schema(description = "Version qui remplace cet état (versionnage sans écrasement).")
    private ImportedStatementDTO supersededBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
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

    public Integer getRejectedRows() {
        return rejectedRows;
    }

    public void setRejectedRows(Integer rejectedRows) {
        this.rejectedRows = rejectedRows;
    }

    public Instant getMaskingCheckedAt() {
        return maskingCheckedAt;
    }

    public void setMaskingCheckedAt(Instant maskingCheckedAt) {
        this.maskingCheckedAt = maskingCheckedAt;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public Boolean getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Boolean currentVersion) {
        this.currentVersion = currentVersion;
    }

    public ImportedStatementDTO getSupersededBy() {
        return supersededBy;
    }

    public void setSupersededBy(ImportedStatementDTO supersededBy) {
        this.supersededBy = supersededBy;
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
            ", supersededBy=" + getSupersededBy() +
            "}";
    }
}
