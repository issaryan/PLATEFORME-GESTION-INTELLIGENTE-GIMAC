package cm.gimac.des.collaboration.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.collaboration.domain.MessageMetadata} entity. This class is used
 * in {@link cm.gimac.des.collaboration.web.rest.MessageMetadataResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /message-metadata?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageMetadataCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter matrixEventId;

    private UUIDFilter authorId;

    private StringFilter terminalId;

    private InstantFilter sentAt;

    private StringFilter sessionId;

    private LongFilter ratchetIndex;

    private IntegerFilter ciphertextSize;

    private StringFilter attachmentRefs;

    private UUIDFilter roomId;

    private Boolean distinct;

    public MessageMetadataCriteria() {}

    public MessageMetadataCriteria(MessageMetadataCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.matrixEventId = other.optionalMatrixEventId().map(StringFilter::copy).orElse(null);
        this.authorId = other.optionalAuthorId().map(UUIDFilter::copy).orElse(null);
        this.terminalId = other.optionalTerminalId().map(StringFilter::copy).orElse(null);
        this.sentAt = other.optionalSentAt().map(InstantFilter::copy).orElse(null);
        this.sessionId = other.optionalSessionId().map(StringFilter::copy).orElse(null);
        this.ratchetIndex = other.optionalRatchetIndex().map(LongFilter::copy).orElse(null);
        this.ciphertextSize = other.optionalCiphertextSize().map(IntegerFilter::copy).orElse(null);
        this.attachmentRefs = other.optionalAttachmentRefs().map(StringFilter::copy).orElse(null);
        this.roomId = other.optionalRoomId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public MessageMetadataCriteria copy() {
        return new MessageMetadataCriteria(this);
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

    public StringFilter getMatrixEventId() {
        return matrixEventId;
    }

    public Optional<StringFilter> optionalMatrixEventId() {
        return Optional.ofNullable(matrixEventId);
    }

    public StringFilter matrixEventId() {
        if (matrixEventId == null) {
            setMatrixEventId(new StringFilter());
        }
        return matrixEventId;
    }

    public void setMatrixEventId(StringFilter matrixEventId) {
        this.matrixEventId = matrixEventId;
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

    public StringFilter getTerminalId() {
        return terminalId;
    }

    public Optional<StringFilter> optionalTerminalId() {
        return Optional.ofNullable(terminalId);
    }

    public StringFilter terminalId() {
        if (terminalId == null) {
            setTerminalId(new StringFilter());
        }
        return terminalId;
    }

    public void setTerminalId(StringFilter terminalId) {
        this.terminalId = terminalId;
    }

    public InstantFilter getSentAt() {
        return sentAt;
    }

    public Optional<InstantFilter> optionalSentAt() {
        return Optional.ofNullable(sentAt);
    }

    public InstantFilter sentAt() {
        if (sentAt == null) {
            setSentAt(new InstantFilter());
        }
        return sentAt;
    }

    public void setSentAt(InstantFilter sentAt) {
        this.sentAt = sentAt;
    }

    public StringFilter getSessionId() {
        return sessionId;
    }

    public Optional<StringFilter> optionalSessionId() {
        return Optional.ofNullable(sessionId);
    }

    public StringFilter sessionId() {
        if (sessionId == null) {
            setSessionId(new StringFilter());
        }
        return sessionId;
    }

    public void setSessionId(StringFilter sessionId) {
        this.sessionId = sessionId;
    }

    public LongFilter getRatchetIndex() {
        return ratchetIndex;
    }

    public Optional<LongFilter> optionalRatchetIndex() {
        return Optional.ofNullable(ratchetIndex);
    }

    public LongFilter ratchetIndex() {
        if (ratchetIndex == null) {
            setRatchetIndex(new LongFilter());
        }
        return ratchetIndex;
    }

    public void setRatchetIndex(LongFilter ratchetIndex) {
        this.ratchetIndex = ratchetIndex;
    }

    public IntegerFilter getCiphertextSize() {
        return ciphertextSize;
    }

    public Optional<IntegerFilter> optionalCiphertextSize() {
        return Optional.ofNullable(ciphertextSize);
    }

    public IntegerFilter ciphertextSize() {
        if (ciphertextSize == null) {
            setCiphertextSize(new IntegerFilter());
        }
        return ciphertextSize;
    }

    public void setCiphertextSize(IntegerFilter ciphertextSize) {
        this.ciphertextSize = ciphertextSize;
    }

    public StringFilter getAttachmentRefs() {
        return attachmentRefs;
    }

    public Optional<StringFilter> optionalAttachmentRefs() {
        return Optional.ofNullable(attachmentRefs);
    }

    public StringFilter attachmentRefs() {
        if (attachmentRefs == null) {
            setAttachmentRefs(new StringFilter());
        }
        return attachmentRefs;
    }

    public void setAttachmentRefs(StringFilter attachmentRefs) {
        this.attachmentRefs = attachmentRefs;
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
        final MessageMetadataCriteria that = (MessageMetadataCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(matrixEventId, that.matrixEventId) &&
            Objects.equals(authorId, that.authorId) &&
            Objects.equals(terminalId, that.terminalId) &&
            Objects.equals(sentAt, that.sentAt) &&
            Objects.equals(sessionId, that.sessionId) &&
            Objects.equals(ratchetIndex, that.ratchetIndex) &&
            Objects.equals(ciphertextSize, that.ciphertextSize) &&
            Objects.equals(attachmentRefs, that.attachmentRefs) &&
            Objects.equals(roomId, that.roomId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            matrixEventId,
            authorId,
            terminalId,
            sentAt,
            sessionId,
            ratchetIndex,
            ciphertextSize,
            attachmentRefs,
            roomId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageMetadataCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalMatrixEventId().map(f -> "matrixEventId=" + f + ", ").orElse("") +
            optionalAuthorId().map(f -> "authorId=" + f + ", ").orElse("") +
            optionalTerminalId().map(f -> "terminalId=" + f + ", ").orElse("") +
            optionalSentAt().map(f -> "sentAt=" + f + ", ").orElse("") +
            optionalSessionId().map(f -> "sessionId=" + f + ", ").orElse("") +
            optionalRatchetIndex().map(f -> "ratchetIndex=" + f + ", ").orElse("") +
            optionalCiphertextSize().map(f -> "ciphertextSize=" + f + ", ").orElse("") +
            optionalAttachmentRefs().map(f -> "attachmentRefs=" + f + ", ").orElse("") +
            optionalRoomId().map(f -> "roomId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
