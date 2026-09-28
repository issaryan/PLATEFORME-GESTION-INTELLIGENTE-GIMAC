package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.ShiftLogEntry} entity.
 */
@Schema(
    description = "Entrée de main courante de quart : ajout seul, chaînée par empreinte ;\nune correction est une nouvelle entrée qui cite la précédente."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShiftLogEntryDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 1L)
    private Long chainIndex;

    @NotNull
    private Instant occurredAt;

    @NotNull
    private UUID authorId;

    @NotNull
    private ShiftLogNature nature;

    @NotNull
    @Size(max = 4000)
    private String text;

    @Size(max = 100)
    @Schema(description = "Référence d'alerte, de ticket ou de liaison.")
    private String reference;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String previousHash;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String hash;

    @NotNull
    private ShiftDTO shift;

    @Schema(description = "Entrée corrigée, citée par une entrée de correction.")
    private ShiftLogEntryDTO corrects;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getChainIndex() {
        return chainIndex;
    }

    public void setChainIndex(Long chainIndex) {
        this.chainIndex = chainIndex;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public ShiftLogNature getNature() {
        return nature;
    }

    public void setNature(ShiftLogNature nature) {
        this.nature = nature;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public ShiftDTO getShift() {
        return shift;
    }

    public void setShift(ShiftDTO shift) {
        this.shift = shift;
    }

    public ShiftLogEntryDTO getCorrects() {
        return corrects;
    }

    public void setCorrects(ShiftLogEntryDTO corrects) {
        this.corrects = corrects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShiftLogEntryDTO)) {
            return false;
        }

        ShiftLogEntryDTO shiftLogEntryDTO = (ShiftLogEntryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, shiftLogEntryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShiftLogEntryDTO{" +
            "id='" + getId() + "'" +
            ", chainIndex=" + getChainIndex() +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", authorId='" + getAuthorId() + "'" +
            ", nature='" + getNature() + "'" +
            ", text='" + getText() + "'" +
            ", reference='" + getReference() + "'" +
            ", previousHash='" + getPreviousHash() + "'" +
            ", hash='" + getHash() + "'" +
            ", shift=" + getShift() +
            ", corrects=" + getCorrects() +
            "}";
    }
}
