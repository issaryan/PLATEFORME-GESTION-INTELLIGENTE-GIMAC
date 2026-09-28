package cm.gimac.des.reporting.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Exportation contrôlée d'agrégats (reporting.export-requested.v1) : plafonnée,
 * filigranée, déposée en GED. Écart CC corrigé : la trace de l'exportation
 * n'est pas modélisée dans le CC.
 */
@Entity
@Table(name = "data_export")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataExport implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Demandeur, responsable ou auditeur (MS-IAM).
     */
    @NotNull
    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    /**
     * Codes des indicateurs exportés, séparés par des virgules.
     */
    @NotNull
    @Size(max = 2000)
    @Column(name = "indicator_codes", length = 2000, nullable = false)
    private String indicatorCodes;

    @NotNull
    @Size(max = 10)
    @Column(name = "period", length = 10, nullable = false)
    private String period;

    /**
     * Filtres de dimensions appliqués : JSON.
     */
    @Lob
    @Column(name = "dimension_filter")
    private String dimensionFilter;

    @NotNull
    @Min(value = 0)
    @Column(name = "row_count", nullable = false)
    private Integer rowCount;

    /**
     * Filigrane : identité du demandeur et horodatage.
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "watermark", length = 255, nullable = false)
    private String watermark;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "content_digest", length = 64)
    private String contentDigest;

    /**
     * Fichier exporté : identifiant dans MS-DOCUMENT.
     */
    @Column(name = "document_ref")
    private UUID documentRef;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public DataExport id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRequesterId() {
        return this.requesterId;
    }

    public DataExport requesterId(UUID requesterId) {
        this.setRequesterId(requesterId);
        return this;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public Instant getRequestedAt() {
        return this.requestedAt;
    }

    public DataExport requestedAt(Instant requestedAt) {
        this.setRequestedAt(requestedAt);
        return this;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getIndicatorCodes() {
        return this.indicatorCodes;
    }

    public DataExport indicatorCodes(String indicatorCodes) {
        this.setIndicatorCodes(indicatorCodes);
        return this;
    }

    public void setIndicatorCodes(String indicatorCodes) {
        this.indicatorCodes = indicatorCodes;
    }

    public String getPeriod() {
        return this.period;
    }

    public DataExport period(String period) {
        this.setPeriod(period);
        return this;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getDimensionFilter() {
        return this.dimensionFilter;
    }

    public DataExport dimensionFilter(String dimensionFilter) {
        this.setDimensionFilter(dimensionFilter);
        return this;
    }

    public void setDimensionFilter(String dimensionFilter) {
        this.dimensionFilter = dimensionFilter;
    }

    public Integer getRowCount() {
        return this.rowCount;
    }

    public DataExport rowCount(Integer rowCount) {
        this.setRowCount(rowCount);
        return this;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public String getWatermark() {
        return this.watermark;
    }

    public DataExport watermark(String watermark) {
        this.setWatermark(watermark);
        return this;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public String getContentDigest() {
        return this.contentDigest;
    }

    public DataExport contentDigest(String contentDigest) {
        this.setContentDigest(contentDigest);
        return this;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public UUID getDocumentRef() {
        return this.documentRef;
    }

    public DataExport documentRef(UUID documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataExport)) {
            return false;
        }
        return getId() != null && getId().equals(((DataExport) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataExport{" +
            "id=" + getId() +
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
