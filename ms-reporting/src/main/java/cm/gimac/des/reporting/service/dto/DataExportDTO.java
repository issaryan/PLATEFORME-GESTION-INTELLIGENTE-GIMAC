package cm.gimac.des.reporting.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.DataExport} entity.
 */
@Schema(
    description = "Exportation contrôlée d'agrégats (reporting.export-requested.v1) : plafonnée,\nfiligranée, déposée en GED. Écart CC corrigé : la trace de l'exportation\nn'est pas modélisée dans le CC."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataExportDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Demandeur, responsable ou auditeur (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID requesterId;

    @NotNull
    private Instant requestedAt;

    @NotNull
    @Size(max = 2000)
    @Schema(description = "Codes des indicateurs exportés, séparés par des virgules.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String indicatorCodes;

    @NotNull
    @Size(max = 10)
    private String period;

    @Schema(description = "Filtres de dimensions appliqués : JSON.")
    @Lob
    private String dimensionFilter;

    @NotNull
    @Min(value = 0)
    private Integer rowCount;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Filigrane : identité du demandeur et horodatage.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String watermark;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentDigest;

    @Schema(description = "Fichier exporté : identifiant dans MS-DOCUMENT.")
    private UUID documentRef;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getIndicatorCodes() {
        return indicatorCodes;
    }

    public void setIndicatorCodes(String indicatorCodes) {
        this.indicatorCodes = indicatorCodes;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getDimensionFilter() {
        return dimensionFilter;
    }

    public void setDimensionFilter(String dimensionFilter) {
        this.dimensionFilter = dimensionFilter;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public String getContentDigest() {
        return contentDigest;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataExportDTO)) {
            return false;
        }

        DataExportDTO dataExportDTO = (DataExportDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, dataExportDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataExportDTO{" +
            "id='" + getId() + "'" +
            ", requesterId='" + getRequesterId() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", indicatorCodes='" + getIndicatorCodes() + "'" +
            ", period='" + getPeriod() + "'" +
            ", dimensionFilter='" + getDimensionFilter() + "'" +
            ", rowCount=" + getRowCount() +
            ", watermark='" + getWatermark() + "'" +
            ", contentDigest='" + getContentDigest() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            "}";
    }
}
