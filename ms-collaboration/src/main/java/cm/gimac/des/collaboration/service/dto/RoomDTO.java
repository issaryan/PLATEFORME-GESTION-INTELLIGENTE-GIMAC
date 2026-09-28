package cm.gimac.des.collaboration.service.dto;

import cm.gimac.des.collaboration.domain.enumeration.AttachmentKind;
import cm.gimac.des.collaboration.domain.enumeration.RoomState;
import cm.gimac.des.collaboration.domain.enumeration.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.Room} entity.
 */
@Schema(
    description = "Salon chiffré de bout en bout, obligatoirement (RG-22). Le rattachement\n(AttachmentRef du CC) est aplati ; il détermine les membres."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 50)
    @Schema(description = "Code lisible, par exemple SAL-CRISE-2026-009 (D-30).", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotNull
    private RoomType type;

    @NotNull
    private AttachmentKind attachmentKind;

    @NotNull
    @Size(max = 100)
    @Schema(
        description = "Référence du rattachement : vacation, pôle, ticket ou cellule de crise.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String attachmentReference;

    @NotNull
    @Size(max = 50)
    @Schema(description = "Service source du rattachement, par exemple MS-INCIDENT.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String attachmentService;

    @Size(max = 50)
    @Schema(description = "Ticket lié (MS-INCIDENT), pour un salon de crise ou d'incident, par exemple TCK-000318.")
    private String ticketReference;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Identifiant du salon dans Matrix Synapse.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String matrixRoomId;

    @NotNull
    @Size(max = 50)
    @Schema(description = "Politique de chiffrement, toujours « bout en bout obligatoire ».", requiredMode = Schema.RequiredMode.REQUIRED)
    private String encryption;

    @NotNull
    private Instant createdAt;

    @NotNull
    private RoomState state;

    @NotNull
    @Schema(description = "Rétention copiée de la politique du type à la création.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration retention;

    @NotNull
    @Min(value = 0)
    @Schema(
        description = "Époque de la session de groupe, incrémentée à chaque rotation (algorithme 6.60).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer groupSessionEpoch;

    private Instant frozenAt;

    private Instant archivedAt;

    @Schema(description = "Échéance de purge des messages chiffrés, calculée selon la politique.")
    private Instant purgeAfter;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public AttachmentKind getAttachmentKind() {
        return attachmentKind;
    }

    public void setAttachmentKind(AttachmentKind attachmentKind) {
        this.attachmentKind = attachmentKind;
    }

    public String getAttachmentReference() {
        return attachmentReference;
    }

    public void setAttachmentReference(String attachmentReference) {
        this.attachmentReference = attachmentReference;
    }

    public String getAttachmentService() {
        return attachmentService;
    }

    public void setAttachmentService(String attachmentService) {
        this.attachmentService = attachmentService;
    }

    public String getTicketReference() {
        return ticketReference;
    }

    public void setTicketReference(String ticketReference) {
        this.ticketReference = ticketReference;
    }

    public String getMatrixRoomId() {
        return matrixRoomId;
    }

    public void setMatrixRoomId(String matrixRoomId) {
        this.matrixRoomId = matrixRoomId;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public RoomState getState() {
        return state;
    }

    public void setState(RoomState state) {
        this.state = state;
    }

    public Duration getRetention() {
        return retention;
    }

    public void setRetention(Duration retention) {
        this.retention = retention;
    }

    public Integer getGroupSessionEpoch() {
        return groupSessionEpoch;
    }

    public void setGroupSessionEpoch(Integer groupSessionEpoch) {
        this.groupSessionEpoch = groupSessionEpoch;
    }

    public Instant getFrozenAt() {
        return frozenAt;
    }

    public void setFrozenAt(Instant frozenAt) {
        this.frozenAt = frozenAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Instant getPurgeAfter() {
        return purgeAfter;
    }

    public void setPurgeAfter(Instant purgeAfter) {
        this.purgeAfter = purgeAfter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomDTO)) {
            return false;
        }

        RoomDTO roomDTO = (RoomDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roomDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomDTO{" +
            "id='" + getId() + "'" +
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
