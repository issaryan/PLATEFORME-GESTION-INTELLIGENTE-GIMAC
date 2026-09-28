package cm.gimac.des.rapprochement.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.ConciliationEntry} entity.
 */
@Schema(description = "Entrée de conciliation entre le groupement et le participant (objet-valeur du CC, persisté).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConciliationEntryDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID authorId;

    @NotNull
    @Schema(description = "Horodatage (at du CC, renommé : mot réservé SQL).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant recordedAt;

    @NotNull
    @Size(max = 4000)
    private String text;

    @Schema(description = "Pièce éventuelle déposée dans MS-DOCUMENT.")
    private UUID documentRef;

    @NotNull
    private SuspensDTO suspens;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public UUID getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(UUID documentRef) {
        this.documentRef = documentRef;
    }

    public SuspensDTO getSuspens() {
        return suspens;
    }

    public void setSuspens(SuspensDTO suspens) {
        this.suspens = suspens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConciliationEntryDTO)) {
            return false;
        }

        ConciliationEntryDTO conciliationEntryDTO = (ConciliationEntryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, conciliationEntryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConciliationEntryDTO{" +
            "id='" + getId() + "'" +
            ", authorId='" + getAuthorId() + "'" +
            ", recordedAt='" + getRecordedAt() + "'" +
            ", text='" + getText() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", suspens=" + getSuspens() +
            "}";
    }
}
