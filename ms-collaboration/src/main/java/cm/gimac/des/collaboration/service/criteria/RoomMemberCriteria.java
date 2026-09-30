package cm.gimac.des.collaboration.service.criteria;

import cm.gimac.des.collaboration.domain.enumeration.MemberRole;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.collaboration.domain.RoomMember} entity. This class is used
 * in {@link cm.gimac.des.collaboration.web.rest.RoomMemberResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /room-members?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomMemberCriteria implements Serializable, Criteria {

    /**
     * Class for filtering MemberRole
     */
    public static class MemberRoleFilter extends Filter<MemberRole> {

        public MemberRoleFilter() {}

        public MemberRoleFilter(MemberRoleFilter filter) {
            super(filter);
        }

        @Override
        public MemberRoleFilter copy() {
            return new MemberRoleFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter userId;

    private MemberRoleFilter role;

    private InstantFilter invitedAt;

    private InstantFilter acknowledgedAt;

    private UUIDFilter replacesUserId;

    private InstantFilter joinedAt;

    private InstantFilter leftAt;

    private UUIDFilter roomId;

    private Boolean distinct;

    public RoomMemberCriteria() {}

    public RoomMemberCriteria(RoomMemberCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.userId = other.optionalUserId().map(UUIDFilter::copy).orElse(null);
        this.role = other.optionalRole().map(MemberRoleFilter::copy).orElse(null);
        this.invitedAt = other.optionalInvitedAt().map(InstantFilter::copy).orElse(null);
        this.acknowledgedAt = other.optionalAcknowledgedAt().map(InstantFilter::copy).orElse(null);
        this.replacesUserId = other.optionalReplacesUserId().map(UUIDFilter::copy).orElse(null);
        this.joinedAt = other.optionalJoinedAt().map(InstantFilter::copy).orElse(null);
        this.leftAt = other.optionalLeftAt().map(InstantFilter::copy).orElse(null);
        this.roomId = other.optionalRoomId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public RoomMemberCriteria copy() {
        return new RoomMemberCriteria(this);
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

    public UUIDFilter getUserId() {
        return userId;
    }

    public Optional<UUIDFilter> optionalUserId() {
        return Optional.ofNullable(userId);
    }

    public UUIDFilter userId() {
        if (userId == null) {
            setUserId(new UUIDFilter());
        }
        return userId;
    }

    public void setUserId(UUIDFilter userId) {
        this.userId = userId;
    }

    public MemberRoleFilter getRole() {
        return role;
    }

    public Optional<MemberRoleFilter> optionalRole() {
        return Optional.ofNullable(role);
    }

    public MemberRoleFilter role() {
        if (role == null) {
            setRole(new MemberRoleFilter());
        }
        return role;
    }

    public void setRole(MemberRoleFilter role) {
        this.role = role;
    }

    public InstantFilter getInvitedAt() {
        return invitedAt;
    }

    public Optional<InstantFilter> optionalInvitedAt() {
        return Optional.ofNullable(invitedAt);
    }

    public InstantFilter invitedAt() {
        if (invitedAt == null) {
            setInvitedAt(new InstantFilter());
        }
        return invitedAt;
    }

    public void setInvitedAt(InstantFilter invitedAt) {
        this.invitedAt = invitedAt;
    }

    public InstantFilter getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Optional<InstantFilter> optionalAcknowledgedAt() {
        return Optional.ofNullable(acknowledgedAt);
    }

    public InstantFilter acknowledgedAt() {
        if (acknowledgedAt == null) {
            setAcknowledgedAt(new InstantFilter());
        }
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(InstantFilter acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public UUIDFilter getReplacesUserId() {
        return replacesUserId;
    }

    public Optional<UUIDFilter> optionalReplacesUserId() {
        return Optional.ofNullable(replacesUserId);
    }

    public UUIDFilter replacesUserId() {
        if (replacesUserId == null) {
            setReplacesUserId(new UUIDFilter());
        }
        return replacesUserId;
    }

    public void setReplacesUserId(UUIDFilter replacesUserId) {
        this.replacesUserId = replacesUserId;
    }

    public InstantFilter getJoinedAt() {
        return joinedAt;
    }

    public Optional<InstantFilter> optionalJoinedAt() {
        return Optional.ofNullable(joinedAt);
    }

    public InstantFilter joinedAt() {
        if (joinedAt == null) {
            setJoinedAt(new InstantFilter());
        }
        return joinedAt;
    }

    public void setJoinedAt(InstantFilter joinedAt) {
        this.joinedAt = joinedAt;
    }

    public InstantFilter getLeftAt() {
        return leftAt;
    }

    public Optional<InstantFilter> optionalLeftAt() {
        return Optional.ofNullable(leftAt);
    }

    public InstantFilter leftAt() {
        if (leftAt == null) {
            setLeftAt(new InstantFilter());
        }
        return leftAt;
    }

    public void setLeftAt(InstantFilter leftAt) {
        this.leftAt = leftAt;
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
        final RoomMemberCriteria that = (RoomMemberCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(role, that.role) &&
            Objects.equals(invitedAt, that.invitedAt) &&
            Objects.equals(acknowledgedAt, that.acknowledgedAt) &&
            Objects.equals(replacesUserId, that.replacesUserId) &&
            Objects.equals(joinedAt, that.joinedAt) &&
            Objects.equals(leftAt, that.leftAt) &&
            Objects.equals(roomId, that.roomId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, role, invitedAt, acknowledgedAt, replacesUserId, joinedAt, leftAt, roomId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomMemberCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalUserId().map(f -> "userId=" + f + ", ").orElse("") +
            optionalRole().map(f -> "role=" + f + ", ").orElse("") +
            optionalInvitedAt().map(f -> "invitedAt=" + f + ", ").orElse("") +
            optionalAcknowledgedAt().map(f -> "acknowledgedAt=" + f + ", ").orElse("") +
            optionalReplacesUserId().map(f -> "replacesUserId=" + f + ", ").orElse("") +
            optionalJoinedAt().map(f -> "joinedAt=" + f + ", ").orElse("") +
            optionalLeftAt().map(f -> "leftAt=" + f + ", ").orElse("") +
            optionalRoomId().map(f -> "roomId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
