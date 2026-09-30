package cm.gimac.des.collaboration.domain;

import cm.gimac.des.collaboration.domain.enumeration.MemberRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Membre d'un salon, tiré du rattachement et non d'une liste libre. Le suivi
 * d'acquittement de l'invitation trace l'escalade vers le suppléant après
 * 10 min (RG-75) ; la temporisation elle-même relève de MS-NOTIFICATION (D-14).
 */
@Entity
@Table(name = "room_member")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Collaborateur (MS-IAM), ou compte technique du terminal d'archivage.
     */
    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MemberRole role;

    @NotNull
    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    /**
     * Membre suppléé : titulaire non acquitté remplacé par ce membre (RG-75).
     */
    @Column(name = "replaces_user_id")
    private UUID replacesUserId;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "roomArchive" }, allowSetters = true)
    private Room room;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RoomMember id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public RoomMember userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public MemberRole getRole() {
        return this.role;
    }

    public RoomMember role(MemberRole role) {
        this.setRole(role);
        return this;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public Instant getInvitedAt() {
        return this.invitedAt;
    }

    public RoomMember invitedAt(Instant invitedAt) {
        this.setInvitedAt(invitedAt);
        return this;
    }

    public void setInvitedAt(Instant invitedAt) {
        this.invitedAt = invitedAt;
    }

    public Instant getAcknowledgedAt() {
        return this.acknowledgedAt;
    }

    public RoomMember acknowledgedAt(Instant acknowledgedAt) {
        this.setAcknowledgedAt(acknowledgedAt);
        return this;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public UUID getReplacesUserId() {
        return this.replacesUserId;
    }

    public RoomMember replacesUserId(UUID replacesUserId) {
        this.setReplacesUserId(replacesUserId);
        return this;
    }

    public void setReplacesUserId(UUID replacesUserId) {
        this.replacesUserId = replacesUserId;
    }

    public Instant getJoinedAt() {
        return this.joinedAt;
    }

    public RoomMember joinedAt(Instant joinedAt) {
        this.setJoinedAt(joinedAt);
        return this;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLeftAt() {
        return this.leftAt;
    }

    public RoomMember leftAt(Instant leftAt) {
        this.setLeftAt(leftAt);
        return this;
    }

    public void setLeftAt(Instant leftAt) {
        this.leftAt = leftAt;
    }

    public Room getRoom() {
        return this.room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public RoomMember room(Room room) {
        this.setRoom(room);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomMember)) {
            return false;
        }
        return getId() != null && getId().equals(((RoomMember) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomMember{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", role='" + getRole() + "'" +
            ", invitedAt='" + getInvitedAt() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            ", replacesUserId='" + getReplacesUserId() + "'" +
            ", joinedAt='" + getJoinedAt() + "'" +
            ", leftAt='" + getLeftAt() + "'" +
            "}";
    }
}
