package cm.gimac.des.collaboration.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.collaboration.domain.Conference} entity. This class is used
 * in {@link cm.gimac.des.collaboration.web.rest.ConferenceResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /conferences?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConferenceCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter sfuRoom;

    private InstantFilter startedAt;

    private InstantFilter endedAt;

    private BooleanFilter sframeRequired;

    private BooleanFilter screenShare;

    private BooleanFilter recording;

    private IntegerFilter keyEpoch;

    private UUIDFilter roomId;

    private Boolean distinct;

    public ConferenceCriteria() {}

    public ConferenceCriteria(ConferenceCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.sfuRoom = other.optionalSfuRoom().map(StringFilter::copy).orElse(null);
        this.startedAt = other.optionalStartedAt().map(InstantFilter::copy).orElse(null);
        this.endedAt = other.optionalEndedAt().map(InstantFilter::copy).orElse(null);
        this.sframeRequired = other.optionalSframeRequired().map(BooleanFilter::copy).orElse(null);
        this.screenShare = other.optionalScreenShare().map(BooleanFilter::copy).orElse(null);
        this.recording = other.optionalRecording().map(BooleanFilter::copy).orElse(null);
        this.keyEpoch = other.optionalKeyEpoch().map(IntegerFilter::copy).orElse(null);
        this.roomId = other.optionalRoomId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ConferenceCriteria copy() {
        return new ConferenceCriteria(this);
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

    public StringFilter getSfuRoom() {
        return sfuRoom;
    }

    public Optional<StringFilter> optionalSfuRoom() {
        return Optional.ofNullable(sfuRoom);
    }

    public StringFilter sfuRoom() {
        if (sfuRoom == null) {
            setSfuRoom(new StringFilter());
        }
        return sfuRoom;
    }

    public void setSfuRoom(StringFilter sfuRoom) {
        this.sfuRoom = sfuRoom;
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

    public InstantFilter getEndedAt() {
        return endedAt;
    }

    public Optional<InstantFilter> optionalEndedAt() {
        return Optional.ofNullable(endedAt);
    }

    public InstantFilter endedAt() {
        if (endedAt == null) {
            setEndedAt(new InstantFilter());
        }
        return endedAt;
    }

    public void setEndedAt(InstantFilter endedAt) {
        this.endedAt = endedAt;
    }

    public BooleanFilter getSframeRequired() {
        return sframeRequired;
    }

    public Optional<BooleanFilter> optionalSframeRequired() {
        return Optional.ofNullable(sframeRequired);
    }

    public BooleanFilter sframeRequired() {
        if (sframeRequired == null) {
            setSframeRequired(new BooleanFilter());
        }
        return sframeRequired;
    }

    public void setSframeRequired(BooleanFilter sframeRequired) {
        this.sframeRequired = sframeRequired;
    }

    public BooleanFilter getScreenShare() {
        return screenShare;
    }

    public Optional<BooleanFilter> optionalScreenShare() {
        return Optional.ofNullable(screenShare);
    }

    public BooleanFilter screenShare() {
        if (screenShare == null) {
            setScreenShare(new BooleanFilter());
        }
        return screenShare;
    }

    public void setScreenShare(BooleanFilter screenShare) {
        this.screenShare = screenShare;
    }

    public BooleanFilter getRecording() {
        return recording;
    }

    public Optional<BooleanFilter> optionalRecording() {
        return Optional.ofNullable(recording);
    }

    public BooleanFilter recording() {
        if (recording == null) {
            setRecording(new BooleanFilter());
        }
        return recording;
    }

    public void setRecording(BooleanFilter recording) {
        this.recording = recording;
    }

    public IntegerFilter getKeyEpoch() {
        return keyEpoch;
    }

    public Optional<IntegerFilter> optionalKeyEpoch() {
        return Optional.ofNullable(keyEpoch);
    }

    public IntegerFilter keyEpoch() {
        if (keyEpoch == null) {
            setKeyEpoch(new IntegerFilter());
        }
        return keyEpoch;
    }

    public void setKeyEpoch(IntegerFilter keyEpoch) {
        this.keyEpoch = keyEpoch;
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
        final ConferenceCriteria that = (ConferenceCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(sfuRoom, that.sfuRoom) &&
            Objects.equals(startedAt, that.startedAt) &&
            Objects.equals(endedAt, that.endedAt) &&
            Objects.equals(sframeRequired, that.sframeRequired) &&
            Objects.equals(screenShare, that.screenShare) &&
            Objects.equals(recording, that.recording) &&
            Objects.equals(keyEpoch, that.keyEpoch) &&
            Objects.equals(roomId, that.roomId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sfuRoom, startedAt, endedAt, sframeRequired, screenShare, recording, keyEpoch, roomId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConferenceCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalSfuRoom().map(f -> "sfuRoom=" + f + ", ").orElse("") +
            optionalStartedAt().map(f -> "startedAt=" + f + ", ").orElse("") +
            optionalEndedAt().map(f -> "endedAt=" + f + ", ").orElse("") +
            optionalSframeRequired().map(f -> "sframeRequired=" + f + ", ").orElse("") +
            optionalScreenShare().map(f -> "screenShare=" + f + ", ").orElse("") +
            optionalRecording().map(f -> "recording=" + f + ", ").orElse("") +
            optionalKeyEpoch().map(f -> "keyEpoch=" + f + ", ").orElse("") +
            optionalRoomId().map(f -> "roomId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
