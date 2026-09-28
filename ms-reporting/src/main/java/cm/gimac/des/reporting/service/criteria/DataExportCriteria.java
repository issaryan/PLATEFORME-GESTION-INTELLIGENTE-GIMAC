package cm.gimac.des.reporting.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.reporting.domain.DataExport} entity. This class is used
 * in {@link cm.gimac.des.reporting.web.rest.DataExportResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /data-exports?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataExportCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter requesterId;

    private InstantFilter requestedAt;

    private StringFilter indicatorCodes;

    private StringFilter period;

    private IntegerFilter rowCount;

    private StringFilter watermark;

    private StringFilter contentDigest;

    private UUIDFilter documentRef;

    private Boolean distinct;

    public DataExportCriteria() {}

    public DataExportCriteria(DataExportCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.requesterId = other.optionalRequesterId().map(UUIDFilter::copy).orElse(null);
        this.requestedAt = other.optionalRequestedAt().map(InstantFilter::copy).orElse(null);
        this.indicatorCodes = other.optionalIndicatorCodes().map(StringFilter::copy).orElse(null);
        this.period = other.optionalPeriod().map(StringFilter::copy).orElse(null);
        this.rowCount = other.optionalRowCount().map(IntegerFilter::copy).orElse(null);
        this.watermark = other.optionalWatermark().map(StringFilter::copy).orElse(null);
        this.contentDigest = other.optionalContentDigest().map(StringFilter::copy).orElse(null);
        this.documentRef = other.optionalDocumentRef().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DataExportCriteria copy() {
        return new DataExportCriteria(this);
    }

    public UUIDFilter getId() {
        return id;
    }

    public Optional<UUIDFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public UUIDFilter id() {
        if (id == null) {
            setId(new UUIDFilter());
        }
        return id;
    }

    public void setId(UUIDFilter id) {
        this.id = id;
    }

    public UUIDFilter getRequesterId() {
        return requesterId;
    }

    public Optional<UUIDFilter> optionalRequesterId() {
        return Optional.ofNullable(requesterId);
    }

    public UUIDFilter requesterId() {
        if (requesterId == null) {
            setRequesterId(new UUIDFilter());
        }
        return requesterId;
    }

    public void setRequesterId(UUIDFilter requesterId) {
        this.requesterId = requesterId;
    }

    public InstantFilter getRequestedAt() {
        return requestedAt;
    }

    public Optional<InstantFilter> optionalRequestedAt() {
        return Optional.ofNullable(requestedAt);
    }

    public InstantFilter requestedAt() {
        if (requestedAt == null) {
            setRequestedAt(new InstantFilter());
        }
        return requestedAt;
    }

    public void setRequestedAt(InstantFilter requestedAt) {
        this.requestedAt = requestedAt;
    }

    public StringFilter getIndicatorCodes() {
        return indicatorCodes;
    }

    public Optional<StringFilter> optionalIndicatorCodes() {
        return Optional.ofNullable(indicatorCodes);
    }

    public StringFilter indicatorCodes() {
        if (indicatorCodes == null) {
            setIndicatorCodes(new StringFilter());
        }
        return indicatorCodes;
    }

    public void setIndicatorCodes(StringFilter indicatorCodes) {
        this.indicatorCodes = indicatorCodes;
    }

    public StringFilter getPeriod() {
        return period;
    }

    public Optional<StringFilter> optionalPeriod() {
        return Optional.ofNullable(period);
    }

    public StringFilter period() {
        if (period == null) {
            setPeriod(new StringFilter());
        }
        return period;
    }

    public void setPeriod(StringFilter period) {
        this.period = period;
    }

    public IntegerFilter getRowCount() {
        return rowCount;
    }

    public Optional<IntegerFilter> optionalRowCount() {
        return Optional.ofNullable(rowCount);
    }

    public IntegerFilter rowCount() {
        if (rowCount == null) {
            setRowCount(new IntegerFilter());
        }
        return rowCount;
    }

    public void setRowCount(IntegerFilter rowCount) {
        this.rowCount = rowCount;
    }

    public StringFilter getWatermark() {
        return watermark;
    }

    public Optional<StringFilter> optionalWatermark() {
        return Optional.ofNullable(watermark);
    }

    public StringFilter watermark() {
        if (watermark == null) {
            setWatermark(new StringFilter());
        }
        return watermark;
    }

    public void setWatermark(StringFilter watermark) {
        this.watermark = watermark;
    }

    public StringFilter getContentDigest() {
        return contentDigest;
    }

    public Optional<StringFilter> optionalContentDigest() {
        return Optional.ofNullable(contentDigest);
    }

    public StringFilter contentDigest() {
        if (contentDigest == null) {
            setContentDigest(new StringFilter());
        }
        return contentDigest;
    }

    public void setContentDigest(StringFilter contentDigest) {
        this.contentDigest = contentDigest;
    }

    public UUIDFilter getDocumentRef() {
        return documentRef;
    }

    public Optional<UUIDFilter> optionalDocumentRef() {
        return Optional.ofNullable(documentRef);
    }

    public UUIDFilter documentRef() {
        if (documentRef == null) {
            setDocumentRef(new UUIDFilter());
        }
        return documentRef;
    }

    public void setDocumentRef(UUIDFilter documentRef) {
        this.documentRef = documentRef;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DataExportCriteria that = (DataExportCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(requesterId, that.requesterId) &&
            Objects.equals(requestedAt, that.requestedAt) &&
            Objects.equals(indicatorCodes, that.indicatorCodes) &&
            Objects.equals(period, that.period) &&
            Objects.equals(rowCount, that.rowCount) &&
            Objects.equals(watermark, that.watermark) &&
            Objects.equals(contentDigest, that.contentDigest) &&
            Objects.equals(documentRef, that.documentRef) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            requesterId,
            requestedAt,
            indicatorCodes,
            period,
            rowCount,
            watermark,
            contentDigest,
            documentRef,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataExportCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalRequesterId().map(f -> "requesterId=" + f + ", ").orElse("") +
            optionalRequestedAt().map(f -> "requestedAt=" + f + ", ").orElse("") +
            optionalIndicatorCodes().map(f -> "indicatorCodes=" + f + ", ").orElse("") +
            optionalPeriod().map(f -> "period=" + f + ", ").orElse("") +
            optionalRowCount().map(f -> "rowCount=" + f + ", ").orElse("") +
            optionalWatermark().map(f -> "watermark=" + f + ", ").orElse("") +
            optionalContentDigest().map(f -> "contentDigest=" + f + ", ").orElse("") +
            optionalDocumentRef().map(f -> "documentRef=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
