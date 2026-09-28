package cm.gimac.des.collaboration.service.criteria;

import cm.gimac.des.collaboration.domain.enumeration.AttachmentKind;
import cm.gimac.des.collaboration.domain.enumeration.RoomState;
import cm.gimac.des.collaboration.domain.enumeration.RoomType;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.collaboration.domain.Room} entity. This class is used
 * in {@link cm.gimac.des.collaboration.web.rest.RoomResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /rooms?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomCriteria implements Serializable, Criteria {

    /**
     * Class for filtering RoomType
     */
    public static class RoomTypeFilter extends Filter<RoomType> {

        public RoomTypeFilter() {}

        public RoomTypeFilter(RoomTypeFilter filter) {
            super(filter);
        }

        @Override
        public RoomTypeFilter copy() {
            return new RoomTypeFilter(this);
        }
    }

    /**
     * Class for filtering AttachmentKind
     */
    public static class AttachmentKindFilter extends Filter<AttachmentKind> {

        public AttachmentKindFilter() {}

        public AttachmentKindFilter(AttachmentKindFilter filter) {
            super(filter);
        }

        @Override
        public AttachmentKindFilter copy() {
            return new AttachmentKindFilter(this);
        }
    }

    /**
     * Class for filtering RoomState
     */
    public static class RoomStateFilter extends Filter<RoomState> {

        public RoomStateFilter() {}

        public RoomStateFilter(RoomStateFilter filter) {
            super(filter);
        }

        @Override
        public RoomStateFilter copy() {
            return new RoomStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter code;

    private RoomTypeFilter type;

    private AttachmentKindFilter attachmentKind;

    private StringFilter attachmentReference;

    private StringFilter attachmentService;

    private StringFilter ticketReference;

    private StringFilter matrixRoomId;

    private StringFilter encryption;

    private InstantFilter createdAt;

    private RoomStateFilter state;

    private DurationFilter retention;

    private IntegerFilter groupSessionEpoch;

    private InstantFilter frozenAt;

    private InstantFilter archivedAt;

    private InstantFilter purgeAfter;

    private UUIDFilter roomArchiveId;

    private Boolean distinct;

    public RoomCriteria() {}

    public RoomCriteria(RoomCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.code = other.optionalCode().map(StringFilter::copy).orElse(null);
        this.type = other.optionalType().map(RoomTypeFilter::copy).orElse(null);
        this.attachmentKind = other.optionalAttachmentKind().map(AttachmentKindFilter::copy).orElse(null);
        this.attachmentReference = other.optionalAttachmentReference().map(StringFilter::copy).orElse(null);
        this.attachmentService = other.optionalAttachmentService().map(StringFilter::copy).orElse(null);
        this.ticketReference = other.optionalTicketReference().map(StringFilter::copy).orElse(null);
        this.matrixRoomId = other.optionalMatrixRoomId().map(StringFilter::copy).orElse(null);
        this.encryption = other.optionalEncryption().map(StringFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.state = other.optionalState().map(RoomStateFilter::copy).orElse(null);
        this.retention = other.optionalRetention().map(DurationFilter::copy).orElse(null);
        this.groupSessionEpoch = other.optionalGroupSessionEpoch().map(IntegerFilter::copy).orElse(null);
        this.frozenAt = other.optionalFrozenAt().map(InstantFilter::copy).orElse(null);
        this.archivedAt = other.optionalArchivedAt().map(InstantFilter::copy).orElse(null);
        this.purgeAfter = other.optionalPurgeAfter().map(InstantFilter::copy).orElse(null);
        this.roomArchiveId = other.optionalRoomArchiveId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public RoomCriteria copy() {
        return new RoomCriteria(this);
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

    public StringFilter getCode() {
        return code;
    }

    public Optional<StringFilter> optionalCode() {
        return Optional.ofNullable(code);
    }

    public StringFilter code() {
        if (code == null) {
            setCode(new StringFilter());
        }
        return code;
    }

    public void setCode(StringFilter code) {
        this.code = code;
    }

    public RoomTypeFilter getType() {
        return type;
    }

    public Optional<RoomTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public RoomTypeFilter type() {
        if (type == null) {
            setType(new RoomTypeFilter());
        }
        return type;
    }

    public void setType(RoomTypeFilter type) {
        this.type = type;
    }

    public AttachmentKindFilter getAttachmentKind() {
        return attachmentKind;
    }

    public Optional<AttachmentKindFilter> optionalAttachmentKind() {
        return Optional.ofNullable(attachmentKind);
    }

    public AttachmentKindFilter attachmentKind() {
        if (attachmentKind == null) {
            setAttachmentKind(new AttachmentKindFilter());
        }
        return attachmentKind;
    }

    public void setAttachmentKind(AttachmentKindFilter attachmentKind) {
        this.attachmentKind = attachmentKind;
    }

    public StringFilter getAttachmentReference() {
        return attachmentReference;
    }

    public Optional<StringFilter> optionalAttachmentReference() {
        return Optional.ofNullable(attachmentReference);
    }

    public StringFilter attachmentReference() {
        if (attachmentReference == null) {
            setAttachmentReference(new StringFilter());
        }
        return attachmentReference;
    }

    public void setAttachmentReference(StringFilter attachmentReference) {
        this.attachmentReference = attachmentReference;
    }

    public StringFilter getAttachmentService() {
        return attachmentService;
    }

    public Optional<StringFilter> optionalAttachmentService() {
        return Optional.ofNullable(attachmentService);
    }

    public StringFilter attachmentService() {
        if (attachmentService == null) {
            setAttachmentService(new StringFilter());
        }
        return attachmentService;
    }

    public void setAttachmentService(StringFilter attachmentService) {
        this.attachmentService = attachmentService;
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

    public StringFilter getMatrixRoomId() {
        return matrixRoomId;
    }

    public Optional<StringFilter> optionalMatrixRoomId() {
        return Optional.ofNullable(matrixRoomId);
    }

    public StringFilter matrixRoomId() {
        if (matrixRoomId == null) {
            setMatrixRoomId(new StringFilter());
        }
        return matrixRoomId;
    }

    public void setMatrixRoomId(StringFilter matrixRoomId) {
        this.matrixRoomId = matrixRoomId;
    }

    public StringFilter getEncryption() {
        return encryption;
    }

    public Optional<StringFilter> optionalEncryption() {
        return Optional.ofNullable(encryption);
    }

    public StringFilter encryption() {
        if (encryption == null) {
            setEncryption(new StringFilter());
        }
        return encryption;
    }

    public void setEncryption(StringFilter encryption) {
        this.encryption = encryption;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public RoomStateFilter getState() {
        return state;
    }

    public Optional<RoomStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public RoomStateFilter state() {
        if (state == null) {
            setState(new RoomStateFilter());
        }
        return state;
    }

    public void setState(RoomStateFilter state) {
        this.state = state;
    }

    public DurationFilter getRetention() {
        return retention;
    }

    public Optional<DurationFilter> optionalRetention() {
        return Optional.ofNullable(retention);
    }

    public DurationFilter retention() {
        if (retention == null) {
            setRetention(new DurationFilter());
        }
        return retention;
    }

    public void setRetention(DurationFilter retention) {
        this.retention = retention;
    }

    public IntegerFilter getGroupSessionEpoch() {
        return groupSessionEpoch;
    }

    public Optional<IntegerFilter> optionalGroupSessionEpoch() {
        return Optional.ofNullable(groupSessionEpoch);
    }

    public IntegerFilter groupSessionEpoch() {
        if (groupSessionEpoch == null) {
            setGroupSessionEpoch(new IntegerFilter());
        }
        return groupSessionEpoch;
    }

    public void setGroupSessionEpoch(IntegerFilter groupSessionEpoch) {
        this.groupSessionEpoch = groupSessionEpoch;
    }

    public InstantFilter getFrozenAt() {
        return frozenAt;
    }

    public Optional<InstantFilter> optionalFrozenAt() {
        return Optional.ofNullable(frozenAt);
    }

    public InstantFilter frozenAt() {
        if (frozenAt == null) {
            setFrozenAt(new InstantFilter());
        }
        return frozenAt;
    }

    public void setFrozenAt(InstantFilter frozenAt) {
        this.frozenAt = frozenAt;
    }

    public InstantFilter getArchivedAt() {
        return archivedAt;
    }

    public Optional<InstantFilter> optionalArchivedAt() {
        return Optional.ofNullable(archivedAt);
    }

    public InstantFilter archivedAt() {
        if (archivedAt == null) {
            setArchivedAt(new InstantFilter());
        }
        return archivedAt;
    }

    public void setArchivedAt(InstantFilter archivedAt) {
        this.archivedAt = archivedAt;
    }

    public InstantFilter getPurgeAfter() {
        return purgeAfter;
    }

    public Optional<InstantFilter> optionalPurgeAfter() {
        return Optional.ofNullable(purgeAfter);
    }

    public InstantFilter purgeAfter() {
        if (purgeAfter == null) {
            setPurgeAfter(new InstantFilter());
        }
        return purgeAfter;
    }

    public void setPurgeAfter(InstantFilter purgeAfter) {
        this.purgeAfter = purgeAfter;
    }

    public UUIDFilter getRoomArchiveId() {
        return roomArchiveId;
    }

    public Optional<UUIDFilter> optionalRoomArchiveId() {
        return Optional.ofNullable(roomArchiveId);
    }

    public UUIDFilter roomArchiveId() {
        if (roomArchiveId == null) {
            setRoomArchiveId(new UUIDFilter());
        }
        return roomArchiveId;
    }

    public void setRoomArchiveId(UUIDFilter roomArchiveId) {
        this.roomArchiveId = roomArchiveId;
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
        final RoomCriteria that = (RoomCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(code, that.code) &&
            Objects.equals(type, that.type) &&
            Objects.equals(attachmentKind, that.attachmentKind) &&
            Objects.equals(attachmentReference, that.attachmentReference) &&
            Objects.equals(attachmentService, that.attachmentService) &&
            Objects.equals(ticketReference, that.ticketReference) &&
            Objects.equals(matrixRoomId, that.matrixRoomId) &&
            Objects.equals(encryption, that.encryption) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(state, that.state) &&
            Objects.equals(retention, that.retention) &&
            Objects.equals(groupSessionEpoch, that.groupSessionEpoch) &&
            Objects.equals(frozenAt, that.frozenAt) &&
            Objects.equals(archivedAt, that.archivedAt) &&
            Objects.equals(purgeAfter, that.purgeAfter) &&
            Objects.equals(roomArchiveId, that.roomArchiveId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            code,
            type,
            attachmentKind,
            attachmentReference,
            attachmentService,
            ticketReference,
            matrixRoomId,
            encryption,
            createdAt,
            state,
            retention,
            groupSessionEpoch,
            frozenAt,
            archivedAt,
            purgeAfter,
            roomArchiveId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalCode().map(f -> "code=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalAttachmentKind().map(f -> "attachmentKind=" + f + ", ").orElse("") +
            optionalAttachmentReference().map(f -> "attachmentReference=" + f + ", ").orElse("") +
            optionalAttachmentService().map(f -> "attachmentService=" + f + ", ").orElse("") +
            optionalTicketReference().map(f -> "ticketReference=" + f + ", ").orElse("") +
            optionalMatrixRoomId().map(f -> "matrixRoomId=" + f + ", ").orElse("") +
            optionalEncryption().map(f -> "encryption=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalRetention().map(f -> "retention=" + f + ", ").orElse("") +
            optionalGroupSessionEpoch().map(f -> "groupSessionEpoch=" + f + ", ").orElse("") +
            optionalFrozenAt().map(f -> "frozenAt=" + f + ", ").orElse("") +
            optionalArchivedAt().map(f -> "archivedAt=" + f + ", ").orElse("") +
            optionalPurgeAfter().map(f -> "purgeAfter=" + f + ", ").orElse("") +
            optionalRoomArchiveId().map(f -> "roomArchiveId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
