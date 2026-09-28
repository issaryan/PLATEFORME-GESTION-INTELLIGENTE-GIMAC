package cm.gimac.des.audit.service.criteria;

import cm.gimac.des.audit.domain.enumeration.PeriodRootState;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.audit.domain.PeriodRoot} entity. This class is used
 * in {@link cm.gimac.des.audit.web.rest.PeriodRootResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /period-roots?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PeriodRootCriteria implements Serializable, Criteria {

    /**
     * Class for filtering PeriodRootState
     */
    public static class PeriodRootStateFilter extends Filter<PeriodRootState> {

        public PeriodRootStateFilter() {}

        public PeriodRootStateFilter(PeriodRootStateFilter filter) {
            super(filter);
        }

        @Override
        public PeriodRootStateFilter copy() {
            return new PeriodRootStateFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter period;

    private LongFilter treeSize;

    private StringFilter rootHash;

    private InstantFilter sealedAt;

    private PeriodRootStateFilter state;

    private StringFilter timestampSerialNumber;

    private InstantFilter timestampGenTime;

    private StringFilter timestampImprint;

    private StringFilter publicationReference;

    private InstantFilter publishedAt;

    private UUIDFilter previousRootId;

    private UUIDFilter consistencyWithPreviousId;

    private UUIDFilter periodRootId;

    private Boolean distinct;

    public PeriodRootCriteria() {}

    public PeriodRootCriteria(PeriodRootCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.period = other.optionalPeriod().map(StringFilter::copy).orElse(null);
        this.treeSize = other.optionalTreeSize().map(LongFilter::copy).orElse(null);
        this.rootHash = other.optionalRootHash().map(StringFilter::copy).orElse(null);
        this.sealedAt = other.optionalSealedAt().map(InstantFilter::copy).orElse(null);
        this.state = other.optionalState().map(PeriodRootStateFilter::copy).orElse(null);
        this.timestampSerialNumber = other.optionalTimestampSerialNumber().map(StringFilter::copy).orElse(null);
        this.timestampGenTime = other.optionalTimestampGenTime().map(InstantFilter::copy).orElse(null);
        this.timestampImprint = other.optionalTimestampImprint().map(StringFilter::copy).orElse(null);
        this.publicationReference = other.optionalPublicationReference().map(StringFilter::copy).orElse(null);
        this.publishedAt = other.optionalPublishedAt().map(InstantFilter::copy).orElse(null);
        this.previousRootId = other.optionalPreviousRootId().map(UUIDFilter::copy).orElse(null);
        this.consistencyWithPreviousId = other.optionalConsistencyWithPreviousId().map(UUIDFilter::copy).orElse(null);
        this.periodRootId = other.optionalPeriodRootId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public PeriodRootCriteria copy() {
        return new PeriodRootCriteria(this);
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

    public LongFilter getTreeSize() {
        return treeSize;
    }

    public Optional<LongFilter> optionalTreeSize() {
        return Optional.ofNullable(treeSize);
    }

    public LongFilter treeSize() {
        if (treeSize == null) {
            setTreeSize(new LongFilter());
        }
        return treeSize;
    }

    public void setTreeSize(LongFilter treeSize) {
        this.treeSize = treeSize;
    }

    public StringFilter getRootHash() {
        return rootHash;
    }

    public Optional<StringFilter> optionalRootHash() {
        return Optional.ofNullable(rootHash);
    }

    public StringFilter rootHash() {
        if (rootHash == null) {
            setRootHash(new StringFilter());
        }
        return rootHash;
    }

    public void setRootHash(StringFilter rootHash) {
        this.rootHash = rootHash;
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

    public PeriodRootStateFilter getState() {
        return state;
    }

    public Optional<PeriodRootStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public PeriodRootStateFilter state() {
        if (state == null) {
            setState(new PeriodRootStateFilter());
        }
        return state;
    }

    public void setState(PeriodRootStateFilter state) {
        this.state = state;
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

    public InstantFilter getTimestampGenTime() {
        return timestampGenTime;
    }

    public Optional<InstantFilter> optionalTimestampGenTime() {
        return Optional.ofNullable(timestampGenTime);
    }

    public InstantFilter timestampGenTime() {
        if (timestampGenTime == null) {
            setTimestampGenTime(new InstantFilter());
        }
        return timestampGenTime;
    }

    public void setTimestampGenTime(InstantFilter timestampGenTime) {
        this.timestampGenTime = timestampGenTime;
    }

    public StringFilter getTimestampImprint() {
        return timestampImprint;
    }

    public Optional<StringFilter> optionalTimestampImprint() {
        return Optional.ofNullable(timestampImprint);
    }

    public StringFilter timestampImprint() {
        if (timestampImprint == null) {
            setTimestampImprint(new StringFilter());
        }
        return timestampImprint;
    }

    public void setTimestampImprint(StringFilter timestampImprint) {
        this.timestampImprint = timestampImprint;
    }

    public StringFilter getPublicationReference() {
        return publicationReference;
    }

    public Optional<StringFilter> optionalPublicationReference() {
        return Optional.ofNullable(publicationReference);
    }

    public StringFilter publicationReference() {
        if (publicationReference == null) {
            setPublicationReference(new StringFilter());
        }
        return publicationReference;
    }

    public void setPublicationReference(StringFilter publicationReference) {
        this.publicationReference = publicationReference;
    }

    public InstantFilter getPublishedAt() {
        return publishedAt;
    }

    public Optional<InstantFilter> optionalPublishedAt() {
        return Optional.ofNullable(publishedAt);
    }

    public InstantFilter publishedAt() {
        if (publishedAt == null) {
            setPublishedAt(new InstantFilter());
        }
        return publishedAt;
    }

    public void setPublishedAt(InstantFilter publishedAt) {
        this.publishedAt = publishedAt;
    }

    public UUIDFilter getPreviousRootId() {
        return previousRootId;
    }

    public Optional<UUIDFilter> optionalPreviousRootId() {
        return Optional.ofNullable(previousRootId);
    }

    public UUIDFilter previousRootId() {
        if (previousRootId == null) {
            setPreviousRootId(new UUIDFilter());
        }
        return previousRootId;
    }

    public void setPreviousRootId(UUIDFilter previousRootId) {
        this.previousRootId = previousRootId;
    }

    public UUIDFilter getConsistencyWithPreviousId() {
        return consistencyWithPreviousId;
    }

    public Optional<UUIDFilter> optionalConsistencyWithPreviousId() {
        return Optional.ofNullable(consistencyWithPreviousId);
    }

    public UUIDFilter consistencyWithPreviousId() {
        if (consistencyWithPreviousId == null) {
            setConsistencyWithPreviousId(new UUIDFilter());
        }
        return consistencyWithPreviousId;
    }

    public void setConsistencyWithPreviousId(UUIDFilter consistencyWithPreviousId) {
        this.consistencyWithPreviousId = consistencyWithPreviousId;
    }

    public UUIDFilter getPeriodRootId() {
        return periodRootId;
    }

    public Optional<UUIDFilter> optionalPeriodRootId() {
        return Optional.ofNullable(periodRootId);
    }

    public UUIDFilter periodRootId() {
        if (periodRootId == null) {
            setPeriodRootId(new UUIDFilter());
        }
        return periodRootId;
    }

    public void setPeriodRootId(UUIDFilter periodRootId) {
        this.periodRootId = periodRootId;
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
        final PeriodRootCriteria that = (PeriodRootCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(period, that.period) &&
            Objects.equals(treeSize, that.treeSize) &&
            Objects.equals(rootHash, that.rootHash) &&
            Objects.equals(sealedAt, that.sealedAt) &&
            Objects.equals(state, that.state) &&
            Objects.equals(timestampSerialNumber, that.timestampSerialNumber) &&
            Objects.equals(timestampGenTime, that.timestampGenTime) &&
            Objects.equals(timestampImprint, that.timestampImprint) &&
            Objects.equals(publicationReference, that.publicationReference) &&
            Objects.equals(publishedAt, that.publishedAt) &&
            Objects.equals(previousRootId, that.previousRootId) &&
            Objects.equals(consistencyWithPreviousId, that.consistencyWithPreviousId) &&
            Objects.equals(periodRootId, that.periodRootId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            period,
            treeSize,
            rootHash,
            sealedAt,
            state,
            timestampSerialNumber,
            timestampGenTime,
            timestampImprint,
            publicationReference,
            publishedAt,
            previousRootId,
            consistencyWithPreviousId,
            periodRootId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PeriodRootCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalPeriod().map(f -> "period=" + f + ", ").orElse("") +
            optionalTreeSize().map(f -> "treeSize=" + f + ", ").orElse("") +
            optionalRootHash().map(f -> "rootHash=" + f + ", ").orElse("") +
            optionalSealedAt().map(f -> "sealedAt=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalTimestampSerialNumber().map(f -> "timestampSerialNumber=" + f + ", ").orElse("") +
            optionalTimestampGenTime().map(f -> "timestampGenTime=" + f + ", ").orElse("") +
            optionalTimestampImprint().map(f -> "timestampImprint=" + f + ", ").orElse("") +
            optionalPublicationReference().map(f -> "publicationReference=" + f + ", ").orElse("") +
            optionalPublishedAt().map(f -> "publishedAt=" + f + ", ").orElse("") +
            optionalPreviousRootId().map(f -> "previousRootId=" + f + ", ").orElse("") +
            optionalConsistencyWithPreviousId().map(f -> "consistencyWithPreviousId=" + f + ", ").orElse("") +
            optionalPeriodRootId().map(f -> "periodRootId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
