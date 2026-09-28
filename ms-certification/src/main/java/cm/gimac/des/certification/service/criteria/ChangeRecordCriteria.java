package cm.gimac.des.certification.service.criteria;

import cm.gimac.des.certification.domain.enumeration.ChangeApproval;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.certification.domain.ChangeRecord} entity. This class is used
 * in {@link cm.gimac.des.certification.web.rest.ChangeRecordResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /change-records?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChangeRecordCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ChangeApproval
     */
    public static class ChangeApprovalFilter extends Filter<ChangeApproval> {

        public ChangeApprovalFilter() {}

        public ChangeApprovalFilter(ChangeApprovalFilter filter) {
            super(filter);
        }

        @Override
        public ChangeApprovalFilter copy() {
            return new ChangeApprovalFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter externalRef;

    private ChangeApprovalFilter approval;

    private InstantFilter approvedAt;

    private InstantFilter windowStart;

    private InstantFilter windowEnd;

    private InstantFilter readAt;

    private StringFilter contentDigest;

    private UUIDFilter referencedBy;

    private UUIDFilter commissioningFileId;

    private Boolean distinct;

    public ChangeRecordCriteria() {}

    public ChangeRecordCriteria(ChangeRecordCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.externalRef = other.optionalExternalRef().map(StringFilter::copy).orElse(null);
        this.approval = other.optionalApproval().map(ChangeApprovalFilter::copy).orElse(null);
        this.approvedAt = other.optionalApprovedAt().map(InstantFilter::copy).orElse(null);
        this.windowStart = other.optionalWindowStart().map(InstantFilter::copy).orElse(null);
        this.windowEnd = other.optionalWindowEnd().map(InstantFilter::copy).orElse(null);
        this.readAt = other.optionalReadAt().map(InstantFilter::copy).orElse(null);
        this.contentDigest = other.optionalContentDigest().map(StringFilter::copy).orElse(null);
        this.referencedBy = other.optionalReferencedBy().map(UUIDFilter::copy).orElse(null);
        this.commissioningFileId = other.optionalCommissioningFileId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ChangeRecordCriteria copy() {
        return new ChangeRecordCriteria(this);
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

    public StringFilter getExternalRef() {
        return externalRef;
    }

    public Optional<StringFilter> optionalExternalRef() {
        return Optional.ofNullable(externalRef);
    }

    public StringFilter externalRef() {
        if (externalRef == null) {
            setExternalRef(new StringFilter());
        }
        return externalRef;
    }

    public void setExternalRef(StringFilter externalRef) {
        this.externalRef = externalRef;
    }

    public ChangeApprovalFilter getApproval() {
        return approval;
    }

    public Optional<ChangeApprovalFilter> optionalApproval() {
        return Optional.ofNullable(approval);
    }

    public ChangeApprovalFilter approval() {
        if (approval == null) {
            setApproval(new ChangeApprovalFilter());
        }
        return approval;
    }

    public void setApproval(ChangeApprovalFilter approval) {
        this.approval = approval;
    }

    public InstantFilter getApprovedAt() {
        return approvedAt;
    }

    public Optional<InstantFilter> optionalApprovedAt() {
        return Optional.ofNullable(approvedAt);
    }

    public InstantFilter approvedAt() {
        if (approvedAt == null) {
            setApprovedAt(new InstantFilter());
        }
        return approvedAt;
    }

    public void setApprovedAt(InstantFilter approvedAt) {
        this.approvedAt = approvedAt;
    }

    public InstantFilter getWindowStart() {
        return windowStart;
    }

    public Optional<InstantFilter> optionalWindowStart() {
        return Optional.ofNullable(windowStart);
    }

    public InstantFilter windowStart() {
        if (windowStart == null) {
            setWindowStart(new InstantFilter());
        }
        return windowStart;
    }

    public void setWindowStart(InstantFilter windowStart) {
        this.windowStart = windowStart;
    }

    public InstantFilter getWindowEnd() {
        return windowEnd;
    }

    public Optional<InstantFilter> optionalWindowEnd() {
        return Optional.ofNullable(windowEnd);
    }

    public InstantFilter windowEnd() {
        if (windowEnd == null) {
            setWindowEnd(new InstantFilter());
        }
        return windowEnd;
    }

    public void setWindowEnd(InstantFilter windowEnd) {
        this.windowEnd = windowEnd;
    }

    public InstantFilter getReadAt() {
        return readAt;
    }

    public Optional<InstantFilter> optionalReadAt() {
        return Optional.ofNullable(readAt);
    }

    public InstantFilter readAt() {
        if (readAt == null) {
            setReadAt(new InstantFilter());
        }
        return readAt;
    }

    public void setReadAt(InstantFilter readAt) {
        this.readAt = readAt;
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

    public UUIDFilter getReferencedBy() {
        return referencedBy;
    }

    public Optional<UUIDFilter> optionalReferencedBy() {
        return Optional.ofNullable(referencedBy);
    }

    public UUIDFilter referencedBy() {
        if (referencedBy == null) {
            setReferencedBy(new UUIDFilter());
        }
        return referencedBy;
    }

    public void setReferencedBy(UUIDFilter referencedBy) {
        this.referencedBy = referencedBy;
    }

    public UUIDFilter getCommissioningFileId() {
        return commissioningFileId;
    }

    public Optional<UUIDFilter> optionalCommissioningFileId() {
        return Optional.ofNullable(commissioningFileId);
    }

    public UUIDFilter commissioningFileId() {
        if (commissioningFileId == null) {
            setCommissioningFileId(new UUIDFilter());
        }
        return commissioningFileId;
    }

    public void setCommissioningFileId(UUIDFilter commissioningFileId) {
        this.commissioningFileId = commissioningFileId;
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
        final ChangeRecordCriteria that = (ChangeRecordCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(externalRef, that.externalRef) &&
            Objects.equals(approval, that.approval) &&
            Objects.equals(approvedAt, that.approvedAt) &&
            Objects.equals(windowStart, that.windowStart) &&
            Objects.equals(windowEnd, that.windowEnd) &&
            Objects.equals(readAt, that.readAt) &&
            Objects.equals(contentDigest, that.contentDigest) &&
            Objects.equals(referencedBy, that.referencedBy) &&
            Objects.equals(commissioningFileId, that.commissioningFileId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            externalRef,
            approval,
            approvedAt,
            windowStart,
            windowEnd,
            readAt,
            contentDigest,
            referencedBy,
            commissioningFileId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChangeRecordCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalExternalRef().map(f -> "externalRef=" + f + ", ").orElse("") +
            optionalApproval().map(f -> "approval=" + f + ", ").orElse("") +
            optionalApprovedAt().map(f -> "approvedAt=" + f + ", ").orElse("") +
            optionalWindowStart().map(f -> "windowStart=" + f + ", ").orElse("") +
            optionalWindowEnd().map(f -> "windowEnd=" + f + ", ").orElse("") +
            optionalReadAt().map(f -> "readAt=" + f + ", ").orElse("") +
            optionalContentDigest().map(f -> "contentDigest=" + f + ", ").orElse("") +
            optionalReferencedBy().map(f -> "referencedBy=" + f + ", ").orElse("") +
            optionalCommissioningFileId().map(f -> "commissioningFileId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
