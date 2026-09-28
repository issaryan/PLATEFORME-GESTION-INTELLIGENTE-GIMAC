package cm.gimac.des.collaboration.service.dto;

import cm.gimac.des.collaboration.domain.enumeration.ArchiveState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.RoomArchive} entity.
 */
@Schema(
    description = "Archive scellée d'un salon (algorithme 6.63) : PDF/A-3 horodaté RFC 3161,\ndéposé en WORM 10 ans dans MS-DOCUMENT (RG-53, D-11). Écart CC corrigé :\nCrisisArchive devient RoomArchive, D-28 étendant l'archivage aux salons\nliés à un ticket."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomArchiveDTO implements Serializable {

    private UUID id;

    @Size(max = 50)
    @Schema(description = "Cellule de crise (MS-INCIDENT), par exemple CRI-2026-009 ; vide pour un salon d'incident.")
    private String crisisCellReference;

    @Size(max = 50)
    @Schema(description = "Ticket lié (MS-INCIDENT).")
    private String ticketReference;

    @NotNull
    @Min(value = 0)
    private Integer messageCount;

    @NotNull
    @Min(value = 0)
    @Schema(
        description = "Messages que le terminal d'archivage n'a pu déchiffrer, consignés comme tels.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer undecryptableCount;

    @NotNull
    @Min(value = 0)
    @Schema(description = "Suites de chiffres masquées en 6+4 (RG-52).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer maskedSequences;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Tête de chaîne hM des entrées, portée au manifeste (formule 6.78).")
    private String chainHead;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "H = SHA256(document).")
    private String contentDigest;

    @Schema(description = "Jeton RFC 3161 encodé en DER.")
    @Lob
    private byte[] timestampToken;

    private String timestampTokenContentType;

    @Schema(description = "Archive déposée : identifiant du document dans MS-DOCUMENT.")
    private UUID documentRef;

    @NotNull
    @Min(value = 1)
    @Max(value = 99)
    @Schema(description = "Rétention de l'archive, en années ; 10 pour une archive de crise.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer retentionYears;

    @NotNull
    private ArchiveState state;

    @NotNull
    private Instant startedAt;

    private Instant sealedAt;

    @NotNull
    @Schema(description = "Une archive au plus par salon (idempotence de l'algorithme 6.63).")
    private RoomDTO room;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCrisisCellReference() {
        return crisisCellReference;
    }

    public void setCrisisCellReference(String crisisCellReference) {
        this.crisisCellReference = crisisCellReference;
    }

    public String getTicketReference() {
        return ticketReference;
    }

    public void setTicketReference(String ticketReference) {
        this.ticketReference = ticketReference;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public Integer getUndecryptableCount() {
        return undecryptableCount;
    }

    public void setUndecryptableCount(Integer undecryptableCount) {
        this.undecryptableCount = undecryptableCount;
    }

    public Integer getMaskedSequences() {
        return maskedSequences;
    }

    public void setMaskedSequences(Integer maskedSequences) {
        this.maskedSequences = maskedSequences;
    }

    public String getChainHead() {
        return chainHead;
    }

    public void setChainHead(String chainHead) {
        this.chainHead = chainHead;
    }

    public String getContentDigest() {
        return contentDigest;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public byte[] getTimestampToken() {
        return timestampToken;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public String getTimestampTokenContentType() {
        return timestampTokenContentType;
    }

    public void setTimestampTokenContentType(String timestampTokenContentType) {
        this.timestampTokenContentType = timestampTokenContentType;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public Integer getRetentionYears() {
        return retentionYears;
    }

    public void setRetentionYears(Integer retentionYears) {
        this.retentionYears = retentionYears;
    }

    public ArchiveState getState() {
        return state;
    }

    public void setState(ArchiveState state) {
        this.state = state;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(Instant sealedAt) {
        this.sealedAt = sealedAt;
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
        if (!(o instanceof RoomArchiveDTO)) {
            return false;
        }

        RoomArchiveDTO roomArchiveDTO = (RoomArchiveDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roomArchiveDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomArchiveDTO{" +
            "id='" + getId() + "'" +
            ", crisisCellReference='" + getCrisisCellReference() + "'" +
            ", ticketReference='" + getTicketReference() + "'" +
            ", messageCount=" + getMessageCount() +
            ", undecryptableCount=" + getUndecryptableCount() +
            ", maskedSequences=" + getMaskedSequences() +
            ", chainHead='" + getChainHead() + "'" +
            ", contentDigest='" + getContentDigest() + "'" +
            ", timestampToken='" + getTimestampToken() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", retentionYears=" + getRetentionYears() +
            ", state='" + getState() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", sealedAt='" + getSealedAt() + "'" +
            ", room=" + getRoom() +
            "}";
    }
}
