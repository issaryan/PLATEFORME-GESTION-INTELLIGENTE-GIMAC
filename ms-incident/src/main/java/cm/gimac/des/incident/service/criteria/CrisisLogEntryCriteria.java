package cm.gimac.des.incident.service.criteria;

import cm.gimac.des.incident.domain.enumeration.CrisisLogNature;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.incident.domain.CrisisLogEntry} entity. This class is used
 * in {@link cm.gimac.des.incident.web.rest.CrisisLogEntryResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /crisis-log-entries?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CrisisLogEntryCriteria implements Serializable, Criteria {

    /**
     * Class for filtering CrisisLogNature
     */
    public static class CrisisLogNatureFilter extends Filter<CrisisLogNature> {

        public CrisisLogNatureFilter() {}

        public CrisisLogNatureFilter(CrisisLogNatureFilter filter) {
            super(filter);
        }

        @Override
        public CrisisLogNatureFilter copy() {
            return new CrisisLogNatureFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private LongFilter chainIndex;

    private InstantFilter occurredAt;

    private UUIDFilter authorId;

    private CrisisLogNatureFilter nature;

    private StringFilter text;

    private StringFilter reference;

    private StringFilter previousHash;

    private StringFilter hash;

    private UUIDFilter cellId;

    private Boolean distinct;

    public CrisisLogEntryCriteria() {}

    public CrisisLogEntryCriteria(CrisisLogEntryCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.chainIndex = other.optionalChainIndex().map(LongFilter::copy).orElse(null);
        this.occurredAt = other.optionalOccurredAt().map(InstantFilter::copy).orElse(null);
        this.authorId = other.optionalAuthorId().map(UUIDFilter::copy).orElse(null);
        this.nature = other.optionalNature().map(CrisisLogNatureFilter::copy).orElse(null);
        this.text = other.optionalText().map(StringFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.previousHash = other.optionalPreviousHash().map(StringFilter::copy).orElse(null);
        this.hash = other.optionalHash().map(StringFilter::copy).orElse(null);
        this.cellId = other.optionalCellId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CrisisLogEntryCriteria copy() {
        return new CrisisLogEntryCriteria(this);
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

    public LongFilter getChainIndex() {
        return chainIndex;
    }

    public Optional<LongFilter> optionalChainIndex() {
        return Optional.ofNullable(chainIndex);
    }

    public LongFilter chainIndex() {
        if (chainIndex == null) {
            setChainIndex(new LongFilter());
        }
        return chainIndex;
    }

    public void setChainIndex(LongFilter chainIndex) {
        this.chainIndex = chainIndex;
    }

    public InstantFilter getOccurredAt() {
        return occurredAt;
    }

    public Optional<InstantFilter> optionalOccurredAt() {
        return Optional.ofNullable(occurredAt);
    }

    public InstantFilter occurredAt() {
        if (occurredAt == null) {
            setOccurredAt(new InstantFilter());
        }
        return occurredAt;
    }

    public void setOccurredAt(InstantFilter occurredAt) {
        this.occurredAt = occurredAt;
    }

    public UUIDFilter getAuthorId() {
        return authorId;
    }

    public Optional<UUIDFilter> optionalAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public UUIDFilter authorId() {
        if (authorId == null) {
            setAuthorId(new UUIDFilter());
        }
        return authorId;
    }

    public void setAuthorId(UUIDFilter authorId) {
        this.authorId = authorId;
    }

    public CrisisLogNatureFilter getNature() {
        return nature;
    }

    public Optional<CrisisLogNatureFilter> optionalNature() {
        return Optional.ofNullable(nature);
    }

    public CrisisLogNatureFilter nature() {
        if (nature == null) {
            setNature(new CrisisLogNatureFilter());
        }
        return nature;
    }

    public void setNature(CrisisLogNatureFilter nature) {
        this.nature = nature;
    }

    public StringFilter getText() {
        return text;
    }

    public Optional<StringFilter> optionalText() {
        return Optional.ofNullable(text);
    }

    public StringFilter text() {
        if (text == null) {
            setText(new StringFilter());
        }
        return text;
    }

    public void setText(StringFilter text) {
        this.text = text;
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

    public StringFilter getPreviousHash() {
        return previousHash;
    }

    public Optional<StringFilter> optionalPreviousHash() {
        return Optional.ofNullable(previousHash);
    }

    public StringFilter previousHash() {
        if (previousHash == null) {
            setPreviousHash(new StringFilter());
        }
        return previousHash;
    }

    public void setPreviousHash(StringFilter previousHash) {
        this.previousHash = previousHash;
    }

    public StringFilter getHash() {
        return hash;
    }

    public Optional<StringFilter> optionalHash() {
        return Optional.ofNullable(hash);
    }

    public StringFilter hash() {
        if (hash == null) {
            setHash(new StringFilter());
        }
        return hash;
    }

    public void setHash(StringFilter hash) {
        this.hash = hash;
    }

    public UUIDFilter getCellId() {
        return cellId;
    }

    public Optional<UUIDFilter> optionalCellId() {
        return Optional.ofNullable(cellId);
    }

    public UUIDFilter cellId() {
        if (cellId == null) {
            setCellId(new UUIDFilter());
        }
        return cellId;
    }

    public void setCellId(UUIDFilter cellId) {
        this.cellId = cellId;
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
        final CrisisLogEntryCriteria that = (CrisisLogEntryCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(chainIndex, that.chainIndex) &&
            Objects.equals(occurredAt, that.occurredAt) &&
            Objects.equals(authorId, that.authorId) &&
            Objects.equals(nature, that.nature) &&
            Objects.equals(text, that.text) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(previousHash, that.previousHash) &&
            Objects.equals(hash, that.hash) &&
            Objects.equals(cellId, that.cellId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chainIndex, occurredAt, authorId, nature, text, reference, previousHash, hash, cellId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CrisisLogEntryCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalChainIndex().map(f -> "chainIndex=" + f + ", ").orElse("") +
            optionalOccurredAt().map(f -> "occurredAt=" + f + ", ").orElse("") +
            optionalAuthorId().map(f -> "authorId=" + f + ", ").orElse("") +
            optionalNature().map(f -> "nature=" + f + ", ").orElse("") +
            optionalText().map(f -> "text=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalPreviousHash().map(f -> "previousHash=" + f + ", ").orElse("") +
            optionalHash().map(f -> "hash=" + f + ", ").orElse("") +
            optionalCellId().map(f -> "cellId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
