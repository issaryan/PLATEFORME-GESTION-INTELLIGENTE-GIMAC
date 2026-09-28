package cm.gimac.des.incident.service.dto;

import cm.gimac.des.incident.domain.enumeration.CrisisLogNature;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.CrisisLogEntry} entity.
 */
@Schema(description = "Entrée de main courante de crise : ajout seul, chaînage des empreintes (RG-41).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CrisisLogEntryDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 1L)
    private Long chainIndex;

    @NotNull
    private Instant occurredAt;

    @NotNull
    private UUID authorId;

    @NotNull
    private CrisisLogNature nature;

    @NotNull
    @Size(max = 4000)
    private String text;

    @Size(max = 100)
    @Schema(description = "Référence citée (décision, tâche, pièce, alerte).")
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
    private CrisisCellDTO cell;

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

    public CrisisLogNature getNature() {
        return nature;
    }

    public void setNature(CrisisLogNature nature) {
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

    public CrisisCellDTO getCell() {
        return cell;
    }

    public void setCell(CrisisCellDTO cell) {
        this.cell = cell;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrisisLogEntryDTO)) {
            return false;
        }

        CrisisLogEntryDTO crisisLogEntryDTO = (CrisisLogEntryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, crisisLogEntryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CrisisLogEntryDTO{" +
            "id='" + getId() + "'" +
            ", chainIndex=" + getChainIndex() +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", authorId='" + getAuthorId() + "'" +
            ", nature='" + getNature() + "'" +
            ", text='" + getText() + "'" +
            ", reference='" + getReference() + "'" +
            ", previousHash='" + getPreviousHash() + "'" +
            ", hash='" + getHash() + "'" +
            ", cell=" + getCell() +
            "}";
    }
}
