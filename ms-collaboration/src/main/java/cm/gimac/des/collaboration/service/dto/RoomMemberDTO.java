package cm.gimac.des.collaboration.service.dto;

import cm.gimac.des.collaboration.domain.enumeration.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.RoomMember} entity.
 */
@Schema(
    description = "Membre d'un salon, tiré du rattachement et non d'une liste libre. Le suivi\nd'acquittement de l'invitation trace l'escalade vers le suppléant après\n10 min (RG-75) ; la temporisation elle-même relève de MS-NOTIFICATION (D-14)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomMemberDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(
        description = "Collaborateur (MS-IAM), ou compte technique du terminal d'archivage.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID userId;

    @NotNull
    private MemberRole role;

    @NotNull
    private Instant invitedAt;

    private Instant acknowledgedAt;

    @Schema(description = "Membre suppléé : titulaire non acquitté remplacé par ce membre (RG-75).")
    private UUID replacesUserId;

    private Instant joinedAt;

    private Instant leftAt;

    @NotNull
    private RoomDTO room;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Instant invitedAt) {
        this.invitedAt = invitedAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public UUID getReplacesUserId() {
        return replacesUserId;
    }

    public void setReplacesUserId(UUID replacesUserId) {
        this.replacesUserId = replacesUserId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Instant leftAt) {
        this.leftAt = leftAt;
    }

    public RoomDTO getRoom() {
        return room;
    }

    public void setRoom(RoomDTO room) {
        this.room = room;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomMemberDTO)) {
            return false;
        }

        RoomMemberDTO roomMemberDTO = (RoomMemberDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roomMemberDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomMemberDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", role='" + getRole() + "'" +
            ", invitedAt='" + getInvitedAt() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            ", replacesUserId='" + getReplacesUserId() + "'" +
            ", joinedAt='" + getJoinedAt() + "'" +
            ", leftAt='" + getLeftAt() + "'" +
            ", room=" + getRoom() +
            "}";
    }
}
