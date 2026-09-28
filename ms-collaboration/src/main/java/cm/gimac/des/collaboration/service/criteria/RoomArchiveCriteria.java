package cm.gimac.des.collaboration.service.criteria;

import cm.gimac.des.collaboration.domain.enumeration.ArchiveState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.collaboration.domain.RoomArchive} entity. This class is used
 * in {@link cm.gimac.des.collaboration.web.rest.RoomArchiveResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /room-archives?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomArchiveCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ArchiveState
     */
    public static class ArchiveStateFilter extends Filter<ArchiveState> {

        public ArchiveStateFilter() {}

        public ArchiveStateFilter(ArchiveStateFilter filter) {
            super(filter);
        }

        @Override
        public ArchiveStateFilter copy() {
            return new ArchiveStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter crisisCellReference;

    private StringFilter ticketReference;

    private IntegerFilter messageCount;

    private IntegerFilter undecryptableCount;

    private IntegerFilter maskedSequences;

    private StringFilter chainHead;

    private StringFilter contentDigest;

    private UUIDFilter documentRef;

    private IntegerFilter retentionYears;

    private ArchiveStateFilter state;

    private InstantFilter startedAt;

    private InstantFilter sealedAt;

    private UUIDFilter roomId;

    private Boolean distinct;

    public RoomArchiveCriteria() {}

    public RoomArchiveCriteria(RoomArchiveCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.crisisCellReference = other.optionalCrisisCellReference().map(StringFilter::copy).orElse(null);
        this.ticketReference = other.optionalTicketReference().map(StringFilter::copy).orElse(null);
        this.messageCount = other.optionalMessageCount().map(IntegerFilter::copy).orElse(null);
        this.undecryptableCount = other.optionalUndecryptableCount().map(IntegerFilter::copy).orElse(null);
        this.maskedSequences = other.optionalMaskedSequences().map(IntegerFilter::copy).orElse(null);
        this.chainHead = other.optionalChainHead().map(StringFilter::copy).orElse(null);
        this.contentDigest = other.optionalContentDigest().map(StringFilter::copy).orElse(null);
        this.documentRef = other.optionalDocumentRef().map(UUIDFilter::copy).orElse(null);
        this.retentionYears = other.optionalRetentionYears().map(IntegerFilter::copy).orElse(null);
        this.state = other.optionalState().map(ArchiveStateFilter::copy).orElse(null);
        this.startedAt = other.optionalStartedAt().map(InstantFilter::copy).orElse(null);
        this.sealedAt = other.optionalSealedAt().map(InstantFilter::copy).orElse(null);
        this.roomId = other.optionalRoomId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public RoomArchiveCriteria copy() {
        return new RoomArchiveCriteria(this);
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

    public StringFilter getCrisisCellReference() {
        return crisisCellReference;
    }

    public Optional<StringFilter> optionalCrisisCellReference() {
        return Optional.ofNullable(crisisCellReference);
    }

    public StringFilter crisisCellReference() {
        if (crisisCellReference == null) {
            setCrisisCellReference(new StringFilter());
        }
        return crisisCellReference;
    }

    public void setCrisisCellReference(StringFilter crisisCellReference) {
        this.crisisCellReference = crisisCellReference;
    }

    public StringFilter getTicketReference() {
        return ticketReference;
    }

    public Optional<StringFilter> optionalTicketReference() {
        return Optional.ofNullable(ticketReference);
    }

    public StringFilter ticketReference() {
        if (ticketReference == null) {
            setTicketReference(new StringFilter());
        }
        return ticketReference;
    }

    public void setTicketReference(StringFilter ticketReference) {
        this.ticketReference = ticketReference;
    }

    public IntegerFilter getMessageCount() {
        return messageCount;
    }

    public Optional<IntegerFilter> optionalMessageCount() {
        return Optional.ofNullable(messageCount);
    }

    public IntegerFilter messageCount() {
        if (messageCount == null) {
            setMessageCount(new IntegerFilter());
        }
        return messageCount;
    }

    public void setMessageCount(IntegerFilter messageCount) {
        this.messageCount = messageCount;
    }

    public IntegerFilter getUndecryptableCount() {
        return undecryptableCount;
    }

    public Optional<IntegerFilter> optionalUndecryptableCount() {
        return Optional.ofNullable(undecryptableCount);
    }

    public IntegerFilter undecryptableCount() {
        if (undecryptableCount == null) {
            setUndecryptableCount(new IntegerFilter());
        }
        return undecryptableCount;
    }

    public void setUndecryptableCount(IntegerFilter undecryptableCount) {
        this.undecryptableCount = undecryptableCount;
    }

    public IntegerFilter getMaskedSequences() {
        return maskedSequences;
    }

    public Optional<IntegerFilter> optionalMaskedSequences() {
        return Optional.ofNullable(maskedSequences);
    }

    public IntegerFilter maskedSequences() {
        if (maskedSequences == null) {
            setMaskedSequences(new IntegerFilter());
        }
        return maskedSequences;
    }

    public void setMaskedSequences(IntegerFilter maskedSequences) {
        this.maskedSequences = maskedSequences;
    }

    public StringFilter getChainHead() {
        return chainHead;
    }

    public Optional<StringFilter> optionalChainHead() {
        return Optional.ofNullable(chainHead);
    }

    public StringFilter chainHead() {
        if (chainHead == null) {
            setChainHead(new StringFilter());
        }
        return chainHead;
    }

    public void setChainHead(StringFilter chainHead) {
        this.chainHead = chainHead;
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

    public IntegerFilter getRetentionYears() {
        return retentionYears;
    }

    public Optional<IntegerFilter> optionalRetentionYears() {
        return Optional.ofNullable(retentionYears);
    }

    public IntegerFilter retentionYears() {
        if (retentionYears == null) {
            setRetentionYears(new IntegerFilter());
        }
        return retentionYears;
    }

    public void setRetentionYears(IntegerFilter retentionYears) {
        this.retentionYears = retentionYears;
    }

    public ArchiveStateFilter getState() {
        return state;
    }

    public Optional<ArchiveStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public ArchiveStateFilter state() {
        if (state == null) {
            setState(new ArchiveStateFilter());
        }
        return state;
    }

    public void setState(ArchiveStateFilter state) {
        this.state = state;
    }

    public InstantFilter getStartedAt() {
        return startedAt;
    }

    public Optional<InstantFilter> optionalStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public InstantFilter startedAt() {
        if (startedAt == null) {
            setStartedAt(new InstantFilter());
        }
        return startedAt;
    }

    public void setStartedAt(InstantFilter startedAt) {
        this.startedAt = startedAt;
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

    public UUIDFilter getRoomId() {
        return roomId;
    }

    public Optional<UUIDFilter> optionalRoomId() {
        return Optional.ofNullable(roomId);
    }

    public UUIDFilter roomId() {
        if (roomId == null) {
            setRoomId(new UUIDFilter());
        }
        return roomId;
    }

    public void setRoomId(UUIDFilter roomId) {
        this.roomId = roomId;
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
        final RoomArchiveCriteria that = (RoomArchiveCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(crisisCellReference, that.crisisCellReference) &&
            Objects.equals(ticketReference, that.ticketReference) &&
            Objects.equals(messageCount, that.messageCount) &&
            Objects.equals(undecryptableCount, that.undecryptableCount) &&
            Objects.equals(maskedSequences, that.maskedSequences) &&
            Objects.equals(chainHead, that.chainHead) &&
            Objects.equals(contentDigest, that.contentDigest) &&
            Objects.equals(documentRef, that.documentRef) &&
            Objects.equals(retentionYears, that.retentionYears) &&
            Objects.equals(state, that.state) &&
            Objects.equals(startedAt, that.startedAt) &&
            Objects.equals(sealedAt, that.sealedAt) &&
            Objects.equals(roomId, that.roomId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            crisisCellReference,
            ticketReference,
            messageCount,
            undecryptableCount,
            maskedSequences,
            chainHead,
            contentDigest,
            documentRef,
            retentionYears,
            state,
            startedAt,
            sealedAt,
            roomId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomArchiveCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalCrisisCellReference().map(f -> "crisisCellReference=" + f + ", ").orElse("") +
            optionalTicketReference().map(f -> "ticketReference=" + f + ", ").orElse("") +
            optionalMessageCount().map(f -> "messageCount=" + f + ", ").orElse("") +
            optionalUndecryptableCount().map(f -> "undecryptableCount=" + f + ", ").orElse("") +
            optionalMaskedSequences().map(f -> "maskedSequences=" + f + ", ").orElse("") +
            optionalChainHead().map(f -> "chainHead=" + f + ", ").orElse("") +
            optionalContentDigest().map(f -> "contentDigest=" + f + ", ").orElse("") +
            optionalDocumentRef().map(f -> "documentRef=" + f + ", ").orElse("") +
            optionalRetentionYears().map(f -> "retentionYears=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalStartedAt().map(f -> "startedAt=" + f + ", ").orElse("") +
            optionalSealedAt().map(f -> "sealedAt=" + f + ", ").orElse("") +
            optionalRoomId().map(f -> "roomId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
