package cm.gimac.des.monitoring.service.criteria;

import cm.gimac.des.monitoring.domain.enumeration.ReportState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.monitoring.domain.ShiftReport} entity. This class is used
 * in {@link cm.gimac.des.monitoring.web.rest.ShiftReportResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /shift-reports?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShiftReportCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ReportState
     */
    public static class ReportStateFilter extends Filter<ReportState> {

        public ReportStateFilter() {}

        public ReportStateFilter(ReportStateFilter filter) {
            super(filter);
        }

        @Override
        public ReportStateFilter copy() {
            return new ReportStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter reference;

    private ReportStateFilter state;

    private StringFilter logHeadHash;

    private StringFilter contentHash;

    private StringFilter digest;

    private StringFilter documentDigest;

    private StringFilter timestampSerialNumber;

    private StringFilter documentRef;

    private InstantFilter responsibilityTransferredAt;

    private InstantFilter sealedAt;

    private UUIDFilter shiftId;

    private UUIDFilter supersedesId;

    private UUIDFilter shiftReportId;

    private Boolean distinct;

    public ShiftReportCriteria() {}

    public ShiftReportCriteria(ShiftReportCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.state = other.optionalState().map(ReportStateFilter::copy).orElse(null);
        this.logHeadHash = other.optionalLogHeadHash().map(StringFilter::copy).orElse(null);
        this.contentHash = other.optionalContentHash().map(StringFilter::copy).orElse(null);
        this.digest = other.optionalDigest().map(StringFilter::copy).orElse(null);
        this.documentDigest = other.optionalDocumentDigest().map(StringFilter::copy).orElse(null);
        this.timestampSerialNumber = other.optionalTimestampSerialNumber().map(StringFilter::copy).orElse(null);
        this.documentRef = other.optionalDocumentRef().map(StringFilter::copy).orElse(null);
        this.responsibilityTransferredAt = other.optionalResponsibilityTransferredAt().map(InstantFilter::copy).orElse(null);
        this.sealedAt = other.optionalSealedAt().map(InstantFilter::copy).orElse(null);
        this.shiftId = other.optionalShiftId().map(UUIDFilter::copy).orElse(null);
        this.supersedesId = other.optionalSupersedesId().map(UUIDFilter::copy).orElse(null);
        this.shiftReportId = other.optionalShiftReportId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ShiftReportCriteria copy() {
        return new ShiftReportCriteria(this);
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

    public ReportStateFilter getState() {
        return state;
    }

    public Optional<ReportStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public ReportStateFilter state() {
        if (state == null) {
            setState(new ReportStateFilter());
        }
        return state;
    }

    public void setState(ReportStateFilter state) {
        this.state = state;
    }

    public StringFilter getLogHeadHash() {
        return logHeadHash;
    }

    public Optional<StringFilter> optionalLogHeadHash() {
        return Optional.ofNullable(logHeadHash);
    }

    public StringFilter logHeadHash() {
        if (logHeadHash == null) {
            setLogHeadHash(new StringFilter());
        }
        return logHeadHash;
    }

    public void setLogHeadHash(StringFilter logHeadHash) {
        this.logHeadHash = logHeadHash;
    }

    public StringFilter getContentHash() {
        return contentHash;
    }

    public Optional<StringFilter> optionalContentHash() {
        return Optional.ofNullable(contentHash);
    }

    public StringFilter contentHash() {
        if (contentHash == null) {
            setContentHash(new StringFilter());
        }
        return contentHash;
    }

    public void setContentHash(StringFilter contentHash) {
        this.contentHash = contentHash;
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

    public StringFilter getDocumentDigest() {
        return documentDigest;
    }

    public Optional<StringFilter> optionalDocumentDigest() {
        return Optional.ofNullable(documentDigest);
    }

    public StringFilter documentDigest() {
        if (documentDigest == null) {
            setDocumentDigest(new StringFilter());
        }
        return documentDigest;
    }

    public void setDocumentDigest(StringFilter documentDigest) {
        this.documentDigest = documentDigest;
    }

    public StringFilter getTimestampSerialNumber() {
        return timestampSerialNumber;
    }

    public Optional<StringFilter> optionalTimestampSerialNumber() {
        return Optional.ofNullable(timestampSerialNumber);
    }

    public StringFilter timestampSerialNumber() {
        if (timestampSerialNumber == null) {
            setTimestampSerialNumber(new StringFilter());
        }
        return timestampSerialNumber;
    }

    public void setTimestampSerialNumber(StringFilter timestampSerialNumber) {
        this.timestampSerialNumber = timestampSerialNumber;
    }

    public StringFilter getDocumentRef() {
        return documentRef;
    }

    public Optional<StringFilter> optionalDocumentRef() {
        return Optional.ofNullable(documentRef);
    }

    public StringFilter documentRef() {
        if (documentRef == null) {
            setDocumentRef(new StringFilter());
        }
        return documentRef;
    }

    public void setDocumentRef(StringFilter documentRef) {
        this.documentRef = documentRef;
    }

    public InstantFilter getResponsibilityTransferredAt() {
        return responsibilityTransferredAt;
    }

    public Optional<InstantFilter> optionalResponsibilityTransferredAt() {
        return Optional.ofNullable(responsibilityTransferredAt);
    }

    public InstantFilter responsibilityTransferredAt() {
        if (responsibilityTransferredAt == null) {
            setResponsibilityTransferredAt(new InstantFilter());
        }
        return responsibilityTransferredAt;
    }

    public void setResponsibilityTransferredAt(InstantFilter responsibilityTransferredAt) {
        this.responsibilityTransferredAt = responsibilityTransferredAt;
    }

    public InstantFilter getSealedAt() {
        return sealedAt;
    }

    public Optional<InstantFilter> optionalSealedAt() {
        return Optional.ofNullable(sealedAt);
    }

    public InstantFilter sealedAt() {
        if (sealedAt == null) {
            setSealedAt(new InstantFilter());
        }
        return sealedAt;
    }

    public void setSealedAt(InstantFilter sealedAt) {
        this.sealedAt = sealedAt;
    }

    public UUIDFilter getShiftId() {
        return shiftId;
    }

    public Optional<UUIDFilter> optionalShiftId() {
        return Optional.ofNullable(shiftId);
    }

    public UUIDFilter shiftId() {
        if (shiftId == null) {
            setShiftId(new UUIDFilter());
        }
        return shiftId;
    }

    public void setShiftId(UUIDFilter shiftId) {
        this.shiftId = shiftId;
    }

    public UUIDFilter getSupersedesId() {
        return supersedesId;
    }

    public Optional<UUIDFilter> optionalSupersedesId() {
        return Optional.ofNullable(supersedesId);
    }

    public UUIDFilter supersedesId() {
        if (supersedesId == null) {
            setSupersedesId(new UUIDFilter());
        }
        return supersedesId;
    }

    public void setSupersedesId(UUIDFilter supersedesId) {
        this.supersedesId = supersedesId;
    }

    public UUIDFilter getShiftReportId() {
        return shiftReportId;
    }

    public Optional<UUIDFilter> optionalShiftReportId() {
        return Optional.ofNullable(shiftReportId);
    }

    public UUIDFilter shiftReportId() {
        if (shiftReportId == null) {
            setShiftReportId(new UUIDFilter());
        }
        return shiftReportId;
    }

    public void setShiftReportId(UUIDFilter shiftReportId) {
        this.shiftReportId = shiftReportId;
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
        final ShiftReportCriteria that = (ShiftReportCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(state, that.state) &&
            Objects.equals(logHeadHash, that.logHeadHash) &&
            Objects.equals(contentHash, that.contentHash) &&
            Objects.equals(digest, that.digest) &&
            Objects.equals(documentDigest, that.documentDigest) &&
            Objects.equals(timestampSerialNumber, that.timestampSerialNumber) &&
            Objects.equals(documentRef, that.documentRef) &&
            Objects.equals(responsibilityTransferredAt, that.responsibilityTransferredAt) &&
            Objects.equals(sealedAt, that.sealedAt) &&
            Objects.equals(shiftId, that.shiftId) &&
            Objects.equals(supersedesId, that.supersedesId) &&
            Objects.equals(shiftReportId, that.shiftReportId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            reference,
            state,
            logHeadHash,
            contentHash,
            digest,
            documentDigest,
            timestampSerialNumber,
            documentRef,
            responsibilityTransferredAt,
            sealedAt,
            shiftId,
            supersedesId,
            shiftReportId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShiftReportCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalLogHeadHash().map(f -> "logHeadHash=" + f + ", ").orElse("") +
            optionalContentHash().map(f -> "contentHash=" + f + ", ").orElse("") +
            optionalDigest().map(f -> "digest=" + f + ", ").orElse("") +
            optionalDocumentDigest().map(f -> "documentDigest=" + f + ", ").orElse("") +
            optionalTimestampSerialNumber().map(f -> "timestampSerialNumber=" + f + ", ").orElse("") +
            optionalDocumentRef().map(f -> "documentRef=" + f + ", ").orElse("") +
            optionalResponsibilityTransferredAt().map(f -> "responsibilityTransferredAt=" + f + ", ").orElse("") +
            optionalSealedAt().map(f -> "sealedAt=" + f + ", ").orElse("") +
            optionalShiftId().map(f -> "shiftId=" + f + ", ").orElse("") +
            optionalSupersedesId().map(f -> "supersedesId=" + f + ", ").orElse("") +
            optionalShiftReportId().map(f -> "shiftReportId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
