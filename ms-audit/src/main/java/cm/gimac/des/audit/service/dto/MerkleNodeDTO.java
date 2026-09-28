package cm.gimac.des.audit.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.audit.domain.MerkleNode} entity.
 */
@Schema(
    description = "Nœud de l'arbre, persisté par niveau et position (niveau 0 = feuille).\nNœud interne : SHA256(0x01 || gauche || droite)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MerkleNodeDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 0)
    @Max(value = 64)
    private Integer level;

    @NotNull
    @Min(value = 0L)
    private Long position;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String hash;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MerkleNodeDTO)) {
            return false;
        }

        MerkleNodeDTO merkleNodeDTO = (MerkleNodeDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, merkleNodeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MerkleNodeDTO{" +
            "id='" + getId() + "'" +
            ", level=" + getLevel() +
            ", position=" + getPosition() +
            ", hash='" + getHash() + "'" +
            "}";
    }
}
