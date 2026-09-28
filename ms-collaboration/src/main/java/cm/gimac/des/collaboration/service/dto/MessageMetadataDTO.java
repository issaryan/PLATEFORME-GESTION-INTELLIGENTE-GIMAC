package cm.gimac.des.collaboration.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.MessageMetadata} entity.
 */
@Schema(
    description = "Métadonnées d'un message chiffré. Écart CC corrigé : l'entité Message du CC\nporte le chiffré ; conformément à D-28, le chiffré reste dans Matrix Synapse\net seule la référence de l'événement est conservée (renommage en\nMessageMetadata, qui évite aussi toute confusion avec Message de Spring)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageMetadataDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Identifiant de l'événement dans Matrix Synapse.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String matrixEventId;

    @NotNull
    @Schema(description = "Auteur (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID authorId;

    @NotNull
    @Size(max = 100)
    private String terminalId;

    @NotNull
    private Instant sentAt;

    @Size(max = 255)
    @Schema(description = "Identifiant de la session de groupe utilisée.")
    private String sessionId;

    @Min(value = 0L)
    @Schema(description = "Indice de cliquet du message dans la chaîne.")
    private Long ratchetIndex;

    @Min(value = 0)
    @Schema(description = "Taille du chiffré, en octets.")
    private Integer ciphertextSize;

    @Size(max = 2000)
    @Schema(description = "Pièces jointes : identifiants de documents (MS-DOCUMENT), séparés par des virgules.")
    private String attachmentRefs;

    @NotNull
    private RoomDTO room;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMatrixEventId() {
        return matrixEventId;
    }

    public void setMatrixEventId(String matrixEventId) {
        this.matrixEventId = matrixEventId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getRatchetIndex() {
        return ratchetIndex;
    }

    public void setRatchetIndex(Long ratchetIndex) {
        this.ratchetIndex = ratchetIndex;
    }

    public Integer getCiphertextSize() {
        return ciphertextSize;
    }

    public void setCiphertextSize(Integer ciphertextSize) {
        this.ciphertextSize = ciphertextSize;
    }

    public String getAttachmentRefs() {
        return attachmentRefs;
    }

    public void setAttachmentRefs(String attachmentRefs) {
        this.attachmentRefs = attachmentRefs;
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
        if (!(o instanceof MessageMetadataDTO)) {
            return false;
        }

        MessageMetadataDTO messageMetadataDTO = (MessageMetadataDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, messageMetadataDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageMetadataDTO{" +
            "id='" + getId() + "'" +
            ", matrixEventId='" + getMatrixEventId() + "'" +
            ", authorId='" + getAuthorId() + "'" +
            ", terminalId='" + getTerminalId() + "'" +
            ", sentAt='" + getSentAt() + "'" +
            ", sessionId='" + getSessionId() + "'" +
            ", ratchetIndex=" + getRatchetIndex() +
            ", ciphertextSize=" + getCiphertextSize() +
            ", attachmentRefs='" + getAttachmentRefs() + "'" +
            ", room=" + getRoom() +
            "}";
    }
}
