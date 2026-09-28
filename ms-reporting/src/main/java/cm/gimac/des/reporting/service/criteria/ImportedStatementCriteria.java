package cm.gimac.des.reporting.service.criteria;

import cm.gimac.des.reporting.domain.enumeration.StatementSource;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.reporting.domain.ImportedStatement} entity. This class is used
 * in {@link cm.gimac.des.reporting.web.rest.ImportedStatementResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /imported-statements?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ImportedStatementCriteria implements Serializable, Criteria {

    /**
     * Class for filtering StatementSource
     */
    public static class StatementSourceFilter extends Filter<StatementSource> {

        public StatementSourceFilter() {}

        public StatementSourceFilter(StatementSourceFilter filter) {
            super(filter);
        }

        @Override
        public StatementSourceFilter copy() {
            return new StatementSourceFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StatementSourceFilter source;

    private UUIDFilter institutionId;

    private StringFilter period;

    private InstantFilter importedAt;

    private UUIDFilter importerId;

    private StringFilter digest;

    private BooleanFilter maskingPassed;

    private IntegerFilter rejectedRows;

    private InstantFilter maskingCheckedAt;

    private IntegerFilter rowCount;

    private UUIDFilter documentRef;

    private BooleanFilter currentVersion;

    private UUIDFilter supersededById;

    private UUIDFilter importedStatementId;

    private Boolean distinct;

    public ImportedStatementCriteria() {}

    public ImportedStatementCriteria(ImportedStatementCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.source = other.optionalSource().map(StatementSourceFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.period = other.optionalPeriod().map(StringFilter::copy).orElse(null);
        this.importedAt = other.optionalImportedAt().map(InstantFilter::copy).orElse(null);
        this.importerId = other.optionalImporterId().map(UUIDFilter::copy).orElse(null);
        this.digest = other.optionalDigest().map(StringFilter::copy).orElse(null);
        this.maskingPassed = other.optionalMaskingPassed().map(BooleanFilter::copy).orElse(null);
        this.rejectedRows = other.optionalRejectedRows().map(IntegerFilter::copy).orElse(null);
        this.maskingCheckedAt = other.optionalMaskingCheckedAt().map(InstantFilter::copy).orElse(null);
        this.rowCount = other.optionalRowCount().map(IntegerFilter::copy).orElse(null);
        this.documentRef = other.optionalDocumentRef().map(UUIDFilter::copy).orElse(null);
        this.currentVersion = other.optionalCurrentVersion().map(BooleanFilter::copy).orElse(null);
        this.supersededById = other.optionalSupersededById().map(UUIDFilter::copy).orElse(null);
        this.importedStatementId = other.optionalImportedStatementId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ImportedStatementCriteria copy() {
        return new ImportedStatementCriteria(this);
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

    public StatementSourceFilter getSource() {
        return source;
    }

    public Optional<StatementSourceFilter> optionalSource() {
        return Optional.ofNullable(source);
    }

    public StatementSourceFilter source() {
        if (source == null) {
            setSource(new StatementSourceFilter());
        }
        return source;
    }

    public void setSource(StatementSourceFilter source) {
        this.source = source;
    }

    public UUIDFilter getInstitutionId() {
        return institutionId;
    }

    public Optional<UUIDFilter> optionalInstitutionId() {
        return Optional.ofNullable(institutionId);
    }

    public UUIDFilter institutionId() {
        if (institutionId == null) {
            setInstitutionId(new UUIDFilter());
        }
        return institutionId;
    }

    public void setInstitutionId(UUIDFilter institutionId) {
        this.institutionId = institutionId;
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

    public InstantFilter getImportedAt() {
        return importedAt;
    }

    public Optional<InstantFilter> optionalImportedAt() {
        return Optional.ofNullable(importedAt);
    }

    public InstantFilter importedAt() {
        if (importedAt == null) {
            setImportedAt(new InstantFilter());
        }
        return importedAt;
    }

    public void setImportedAt(InstantFilter importedAt) {
        this.importedAt = importedAt;
    }

    public UUIDFilter getImporterId() {
        return importerId;
    }

    public Optional<UUIDFilter> optionalImporterId() {
        return Optional.ofNullable(importerId);
    }

    public UUIDFilter importerId() {
        if (importerId == null) {
            setImporterId(new UUIDFilter());
        }
        return importerId;
    }

    public void setImporterId(UUIDFilter importerId) {
        this.importerId = importerId;
    }

    public StringFilter getDigest() {
        return digest;
    }

    public Optional<StringFilter> optionalDigest() {
        return Optional.ofNullable(digest);
    }

    public StringFilter digest() {
        if (digest == null) {
            setDigest(new StringFilter());
        }
        return digest;
    }

    public void setDigest(StringFilter digest) {
        this.digest = digest;
    }

    public BooleanFilter getMaskingPassed() {
        return maskingPassed;
    }

    public Optional<BooleanFilter> optionalMaskingPassed() {
        return Optional.ofNullable(maskingPassed);
    }

    public BooleanFilter maskingPassed() {
        if (maskingPassed == null) {
            setMaskingPassed(new BooleanFilter());
        }
        return maskingPassed;
    }

    public void setMaskingPassed(BooleanFilter maskingPassed) {
        this.maskingPassed = maskingPassed;
    }

    public IntegerFilter getRejectedRows() {
        return rejectedRows;
    }

    public Optional<IntegerFilter> optionalRejectedRows() {
        return Optional.ofNullable(rejectedRows);
    }

    public IntegerFilter rejectedRows() {
        if (rejectedRows == null) {
            setRejectedRows(new IntegerFilter());
        }
        return rejectedRows;
    }

    public void setRejectedRows(IntegerFilter rejectedRows) {
        this.rejectedRows = rejectedRows;
    }

    public InstantFilter getMaskingCheckedAt() {
        return maskingCheckedAt;
    }

    public Optional<InstantFilter> optionalMaskingCheckedAt() {
        return Optional.ofNullable(maskingCheckedAt);
    }

    public InstantFilter maskingCheckedAt() {
        if (maskingCheckedAt == null) {
            setMaskingCheckedAt(new InstantFilter());
        }
        return maskingCheckedAt;
    }

    public void setMaskingCheckedAt(InstantFilter maskingCheckedAt) {
        this.maskingCheckedAt = maskingCheckedAt;
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

    public BooleanFilter getCurrentVersion() {
        return currentVersion;
    }

    public Optional<BooleanFilter> optionalCurrentVersion() {
        return Optional.ofNullable(currentVersion);
    }

    public BooleanFilter currentVersion() {
        if (currentVersion == null) {
            setCurrentVersion(new BooleanFilter());
        }
        return currentVersion;
    }

    public void setCurrentVersion(BooleanFilter currentVersion) {
        this.currentVersion = currentVersion;
    }

    public UUIDFilter getSupersededById() {
        return supersededById;
    }

    public Optional<UUIDFilter> optionalSupersededById() {
        return Optional.ofNullable(supersededById);
    }

    public UUIDFilter supersededById() {
        if (supersededById == null) {
            setSupersededById(new UUIDFilter());
        }
        return supersededById;
    }

    public void setSupersededById(UUIDFilter supersededById) {
        this.supersededById = supersededById;
    }

    public UUIDFilter getImportedStatementId() {
        return importedStatementId;
    }

    public Optional<UUIDFilter> optionalImportedStatementId() {
        return Optional.ofNullable(importedStatementId);
    }

    public UUIDFilter importedStatementId() {
        if (importedStatementId == null) {
            setImportedStatementId(new UUIDFilter());
        }
        return importedStatementId;
    }

    public void setImportedStatementId(UUIDFilter importedStatementId) {
        this.importedStatementId = importedStatementId;
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
        final ImportedStatementCriteria that = (ImportedStatementCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(source, that.source) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(period, that.period) &&
            Objects.equals(importedAt, that.importedAt) &&
            Objects.equals(importerId, that.importerId) &&
            Objects.equals(digest, that.digest) &&
            Objects.equals(maskingPassed, that.maskingPassed) &&
            Objects.equals(rejectedRows, that.rejectedRows) &&
            Objects.equals(maskingCheckedAt, that.maskingCheckedAt) &&
            Objects.equals(rowCount, that.rowCount) &&
            Objects.equals(documentRef, that.documentRef) &&
            Objects.equals(currentVersion, that.currentVersion) &&
            Objects.equals(supersededById, that.supersededById) &&
            Objects.equals(importedStatementId, that.importedStatementId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            source,
            institutionId,
            period,
            importedAt,
            importerId,
            digest,
            maskingPassed,
            rejectedRows,
            maskingCheckedAt,
            rowCount,
            documentRef,
            currentVersion,
            supersededById,
            importedStatementId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ImportedStatementCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalSource().map(f -> "source=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalPeriod().map(f -> "period=" + f + ", ").orElse("") +
            optionalImportedAt().map(f -> "importedAt=" + f + ", ").orElse("") +
            optionalImporterId().map(f -> "importerId=" + f + ", ").orElse("") +
            optionalDigest().map(f -> "digest=" + f + ", ").orElse("") +
            optionalMaskingPassed().map(f -> "maskingPassed=" + f + ", ").orElse("") +
            optionalRejectedRows().map(f -> "rejectedRows=" + f + ", ").orElse("") +
            optionalMaskingCheckedAt().map(f -> "maskingCheckedAt=" + f + ", ").orElse("") +
            optionalRowCount().map(f -> "rowCount=" + f + ", ").orElse("") +
            optionalDocumentRef().map(f -> "documentRef=" + f + ", ").orElse("") +
            optionalCurrentVersion().map(f -> "currentVersion=" + f + ", ").orElse("") +
            optionalSupersededById().map(f -> "supersededById=" + f + ", ").orElse("") +
            optionalImportedStatementId().map(f -> "importedStatementId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
