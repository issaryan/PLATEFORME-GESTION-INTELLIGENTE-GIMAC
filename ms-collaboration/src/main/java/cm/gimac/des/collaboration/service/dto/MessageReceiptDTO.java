package cm.gimac.des.collaboration.service.dto;

import cm.gimac.des.collaboration.domain.enumeration.ReceiptKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.MessageReceipt} entity.
 */
@Schema(description = "Accusé de réception (remis, lu), métadonnée visible du serveur.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageReceiptDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Membre destinataire (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID userId;

    @NotNull
    private ReceiptKind kind;

    @NotNull
    private Instant receivedAt;

    @NotNull
    private MessageMetadataDTO message;

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

    public ReceiptKind getKind() {
        return kind;
    }

    public void setKind(ReceiptKind kind) {
        this.kind = kind;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public MessageMetadataDTO getMessage() {
        return message;
    }

    public void setMessage(MessageMetadataDTO message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageReceiptDTO)) {
            return false;
        }

        MessageReceiptDTO messageReceiptDTO = (MessageReceiptDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, messageReceiptDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageReceiptDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", kind='" + getKind() + "'" +
            ", receivedAt='" + getReceivedAt() + "'" +
            ", message=" + getMessage() +
            "}";
    }
}
