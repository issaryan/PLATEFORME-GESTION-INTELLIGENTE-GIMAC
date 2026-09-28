package cm.gimac.des.rapprochement.service.criteria;

import cm.gimac.des.rapprochement.domain.enumeration.StatementFormat;
import cm.gimac.des.rapprochement.domain.enumeration.StatementSource;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.rapprochement.domain.ImportedStatement} entity. This class is used
 * in {@link cm.gimac.des.rapprochement.web.rest.ImportedStatementResource} to receive all the possible filtering options from
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

    /**
     * Class for filtering StatementFormat
     */
    public static class StatementFormatFilter extends Filter<StatementFormat> {

        public StatementFormatFilter() {}

        public StatementFormatFilter(StatementFormatFilter filter) {
            super(filter);
        }

        @Override
        public StatementFormatFilter copy() {
            return new StatementFormatFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter reference;

    private StatementSourceFilter source;

    private UUIDFilter institutionId;

    private LocalDateFilter businessDate;

    private InstantFilter importedAt;

    private UUIDFilter importerId;

    private StatementFormatFilter format;

    private StringFilter digest;

    private BooleanFilter maskingPassed;

    private IntegerFilter maskingRejectedLines;

    private InstantFilter maskingCheckedAt;

    private IntegerFilter lineCount;

    private UUIDFilter documentRef;

    private LocalDateFilter linesRetainedUntil;

    private Boolean distinct;

    public ImportedStatementCriteria() {}

    public ImportedStatementCriteria(ImportedStatementCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.source = other.optionalSource().map(StatementSourceFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.businessDate = other.optionalBusinessDate().map(LocalDateFilter::copy).orElse(null);
        this.importedAt = other.optionalImportedAt().map(InstantFilter::copy).orElse(null);
        this.importerId = other.optionalImporterId().map(UUIDFilter::copy).orElse(null);
        this.format = other.optionalFormat().map(StatementFormatFilter::copy).orElse(null);
        this.digest = other.optionalDigest().map(StringFilter::copy).orElse(null);
        this.maskingPassed = other.optionalMaskingPassed().map(BooleanFilter::copy).orElse(null);
        this.maskingRejectedLines = other.optionalMaskingRejectedLines().map(IntegerFilter::copy).orElse(null);
        this.maskingCheckedAt = other.optionalMaskingCheckedAt().map(InstantFilter::copy).orElse(null);
        this.lineCount = other.optionalLineCount().map(IntegerFilter::copy).orElse(null);
        this.documentRef = other.optionalDocumentRef().map(UUIDFilter::copy).orElse(null);
        this.linesRetainedUntil = other.optionalLinesRetainedUntil().map(LocalDateFilter::copy).orElse(null);
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

    public StringFilter getReference() {
        return reference;
    }

    public Optional<StringFilter> optionalReference() {
        return Optional.ofNullable(reference);
    }

    public StringFilter reference() {
        if (reference == null) {
            setReference(new StringFilter());
        }
        return reference;
    }

    public void setReference(StringFilter reference) {
        this.reference = reference;
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

    public LocalDateFilter getBusinessDate() {
        return businessDate;
    }

    public Optional<LocalDateFilter> optionalBusinessDate() {
        return Optional.ofNullable(businessDate);
    }

    public LocalDateFilter businessDate() {
        if (businessDate == null) {
            setBusinessDate(new LocalDateFilter());
        }
        return businessDate;
    }

    public void setBusinessDate(LocalDateFilter businessDate) {
        this.businessDate = businessDate;
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

    public StatementFormatFilter getFormat() {
        return format;
    }

    public Optional<StatementFormatFilter> optionalFormat() {
        return Optional.ofNullable(format);
    }

    public StatementFormatFilter format() {
        if (format == null) {
            setFormat(new StatementFormatFilter());
        }
        return format;
    }

    public void setFormat(StatementFormatFilter format) {
        this.format = format;
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

    public IntegerFilter getMaskingRejectedLines() {
        return maskingRejectedLines;
    }

    public Optional<IntegerFilter> optionalMaskingRejectedLines() {
        return Optional.ofNullable(maskingRejectedLines);
    }

    public IntegerFilter maskingRejectedLines() {
        if (maskingRejectedLines == null) {
            setMaskingRejectedLines(new IntegerFilter());
        }
        return maskingRejectedLines;
    }

    public void setMaskingRejectedLines(IntegerFilter maskingRejectedLines) {
        this.maskingRejectedLines = maskingRejectedLines;
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

    public IntegerFilter getLineCount() {
        return lineCount;
    }

    public Optional<IntegerFilter> optionalLineCount() {
        return Optional.ofNullable(lineCount);
    }

    public IntegerFilter lineCount() {
        if (lineCount == null) {
            setLineCount(new IntegerFilter());
        }
        return lineCount;
    }

    public void setLineCount(IntegerFilter lineCount) {
        this.lineCount = lineCount;
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

    public LocalDateFilter getLinesRetainedUntil() {
        return linesRetainedUntil;
    }

    public Optional<LocalDateFilter> optionalLinesRetainedUntil() {
        return Optional.ofNullable(linesRetainedUntil);
    }

    public LocalDateFilter linesRetainedUntil() {
        if (linesRetainedUntil == null) {
            setLinesRetainedUntil(new LocalDateFilter());
        }
        return linesRetainedUntil;
    }

    public void setLinesRetainedUntil(LocalDateFilter linesRetainedUntil) {
        this.linesRetainedUntil = linesRetainedUntil;
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
            Objects.equals(reference, that.reference) &&
            Objects.equals(source, that.source) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(businessDate, that.businessDate) &&
            Objects.equals(importedAt, that.importedAt) &&
            Objects.equals(importerId, that.importerId) &&
            Objects.equals(format, that.format) &&
            Objects.equals(digest, that.digest) &&
            Objects.equals(maskingPassed, that.maskingPassed) &&
            Objects.equals(maskingRejectedLines, that.maskingRejectedLines) &&
            Objects.equals(maskingCheckedAt, that.maskingCheckedAt) &&
            Objects.equals(lineCount, that.lineCount) &&
            Objects.equals(documentRef, that.documentRef) &&
            Objects.equals(linesRetainedUntil, that.linesRetainedUntil) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            reference,
            source,
            institutionId,
            businessDate,
            importedAt,
            importerId,
            format,
            digest,
            maskingPassed,
            maskingRejectedLines,
            maskingCheckedAt,
            lineCount,
            documentRef,
            linesRetainedUntil,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ImportedStatementCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalSource().map(f -> "source=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalBusinessDate().map(f -> "businessDate=" + f + ", ").orElse("") +
            optionalImportedAt().map(f -> "importedAt=" + f + ", ").orElse("") +
            optionalImporterId().map(f -> "importerId=" + f + ", ").orElse("") +
            optionalFormat().map(f -> "format=" + f + ", ").orElse("") +
            optionalDigest().map(f -> "digest=" + f + ", ").orElse("") +
            optionalMaskingPassed().map(f -> "maskingPassed=" + f + ", ").orElse("") +
            optionalMaskingRejectedLines().map(f -> "maskingRejectedLines=" + f + ", ").orElse("") +
            optionalMaskingCheckedAt().map(f -> "maskingCheckedAt=" + f + ", ").orElse("") +
            optionalLineCount().map(f -> "lineCount=" + f + ", ").orElse("") +
            optionalDocumentRef().map(f -> "documentRef=" + f + ", ").orElse("") +
            optionalLinesRetainedUntil().map(f -> "linesRetainedUntil=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
