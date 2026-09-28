package cm.gimac.des.collaboration.domain;

import cm.gimac.des.collaboration.domain.enumeration.AttachmentKind;
import cm.gimac.des.collaboration.domain.enumeration.RoomState;
import cm.gimac.des.collaboration.domain.enumeration.RoomType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Salon chiffré de bout en bout, obligatoirement (RG-22). Le rattachement
 * (AttachmentRef du CC) est aplati ; il détermine les membres.
 */
@Entity
@Table(name = "room")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Code lisible, par exemple SAL-CRISE-2026-009 (D-30).
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RoomType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_kind", nullable = false)
    private AttachmentKind attachmentKind;

    /**
     * Référence du rattachement : vacation, pôle, ticket ou cellule de crise.
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "attachment_reference", length = 100, nullable = false)
    private String attachmentReference;

    /**
     * Service source du rattachement, par exemple MS-INCIDENT.
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "attachment_service", length = 50, nullable = false)
    private String attachmentService;

    /**
     * Ticket lié (MS-INCIDENT), pour un salon de crise ou d'incident, par exemple TCK-000318.
     */
    @Size(max = 50)
    @Column(name = "ticket_reference", length = 50)
    private String ticketReference;

    /**
     * Identifiant du salon dans Matrix Synapse.
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "matrix_room_id", length = 255, nullable = false, unique = true)
    private String matrixRoomId;

    /**
     * Politique de chiffrement, toujours « bout en bout obligatoire ».
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "encryption", length = 50, nullable = false)
    private String encryption;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private RoomState state;

    /**
     * Rétention copiée de la politique du type à la création.
     */
    @NotNull
    @Column(name = "retention", nullable = false)
    private Duration retention;

    /**
     * Époque de la session de groupe, incrémentée à chaque rotation (algorithme 6.60).
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "group_session_epoch", nullable = false)
    private Integer groupSessionEpoch;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    /**
     * Échéance de purge des messages chiffrés, calculée selon la politique.
     */
    @Column(name = "purge_after")
    private Instant purgeAfter;

    @JsonIgnoreProperties(value = { "room" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "room")
    private RoomArchive roomArchive;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Room id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public Room code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public RoomType getType() {
        return this.type;
    }

    public Room type(RoomType type) {
        this.setType(type);
        return this;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public AttachmentKind getAttachmentKind() {
        return this.attachmentKind;
    }

    public Room attachmentKind(AttachmentKind attachmentKind) {
        this.setAttachmentKind(attachmentKind);
        return this;
    }

    public void setAttachmentKind(AttachmentKind attachmentKind) {
        this.attachmentKind = attachmentKind;
    }

    public String getAttachmentReference() {
        return this.attachmentReference;
    }

    public Room attachmentReference(String attachmentReference) {
        this.setAttachmentReference(attachmentReference);
        return this;
    }

    public void setAttachmentReference(String attachmentReference) {
        this.attachmentReference = attachmentReference;
    }

    public String getAttachmentService() {
        return this.attachmentService;
    }

    public Room attachmentService(String attachmentService) {
        this.setAttachmentService(attachmentService);
        return this;
    }

    public void setAttachmentService(String attachmentService) {
        this.attachmentService = attachmentService;
    }

    public String getTicketReference() {
        return this.ticketReference;
    }

    public Room ticketReference(String ticketReference) {
        this.setTicketReference(ticketReference);
        return this;
    }

    public void setTicketReference(String ticketReference) {
        this.ticketReference = ticketReference;
    }

    public String getMatrixRoomId() {
        return this.matrixRoomId;
    }

    public Room matrixRoomId(String matrixRoomId) {
        this.setMatrixRoomId(matrixRoomId);
        return this;
    }

    public void setMatrixRoomId(String matrixRoomId) {
        this.matrixRoomId = matrixRoomId;
    }

    public String getEncryption() {
        return this.encryption;
    }

    public Room encryption(String encryption) {
        this.setEncryption(encryption);
        return this;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Room createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public RoomState getState() {
        return this.state;
    }

    public Room state(RoomState state) {
        this.setState(state);
        return this;
    }

    public void setState(RoomState state) {
        this.state = state;
    }

    public Duration getRetention() {
        return this.retention;
    }

    public Room retention(Duration retention) {
        this.setRetention(retention);
        return this;
    }

    public void setRetention(Duration retention) {
        this.retention = retention;
    }

    public Integer getGroupSessionEpoch() {
        return this.groupSessionEpoch;
    }

    public Room groupSessionEpoch(Integer groupSessionEpoch) {
        this.setGroupSessionEpoch(groupSessionEpoch);
        return this;
    }

    public void setGroupSessionEpoch(Integer groupSessionEpoch) {
        this.groupSessionEpoch = groupSessionEpoch;
    }

    public Instant getFrozenAt() {
        return this.frozenAt;
    }

    public Room frozenAt(Instant frozenAt) {
        this.setFrozenAt(frozenAt);
        return this;
    }

    public void setFrozenAt(Instant frozenAt) {
        this.frozenAt = frozenAt;
    }

    public Instant getArchivedAt() {
        return this.archivedAt;
    }

    public Room archivedAt(Instant archivedAt) {
        this.setArchivedAt(archivedAt);
        return this;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Instant getPurgeAfter() {
        return this.purgeAfter;
    }

    public Room purgeAfter(Instant purgeAfter) {
        this.setPurgeAfter(purgeAfter);
        return this;
    }

    public void setPurgeAfter(Instant purgeAfter) {
        this.purgeAfter = purgeAfter;
    }

    public RoomArchive getRoomArchive() {
        return this.roomArchive;
    }

    public void setRoomArchive(RoomArchive roomArchive) {
        if (this.roomArchive != null) {
            this.roomArchive.setRoom(null);
        }
        if (roomArchive != null) {
            roomArchive.setRoom(this);
        }
        this.roomArchive = roomArchive;
    }

    public Room roomArchive(RoomArchive roomArchive) {
        this.setRoomArchive(roomArchive);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Room)) {
            return false;
        }
        return getId() != null && getId().equals(((Room) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Room{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", type='" + getType() + "'" +
            ", attachmentKind='" + getAttachmentKind() + "'" +
            ", attachmentReference='" + getAttachmentReference() + "'" +
            ", attachmentService='" + getAttachmentService() + "'" +
            ", ticketReference='" + getTicketReference() + "'" +
            ", matrixRoomId='" + getMatrixRoomId() + "'" +
            ", encryption='" + getEncryption() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", state='" + getState() + "'" +
            ", retention='" + getRetention() + "'" +
            ", groupSessionEpoch=" + getGroupSessionEpoch() +
            ", frozenAt='" + getFrozenAt() + "'" +
            ", archivedAt='" + getArchivedAt() + "'" +
            ", purgeAfter='" + getPurgeAfter() + "'" +
            "}";
    }
}
