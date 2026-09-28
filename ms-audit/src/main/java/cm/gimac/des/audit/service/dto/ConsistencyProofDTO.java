package cm.gimac.des.audit.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.audit.domain.ConsistencyProof} entity.
 */
@Schema(description = "Preuve de cohérence entre deux racines successives, calculée au\nscellement et conservée (algorithme 6.14).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConsistencyProofDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 0L)
    private Long firstSize;

    @NotNull
    @Min(value = 0L)
    private Long secondSize;

    @Schema(description = "Liste JSON des empreintes de la preuve, en hexadécimal.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Lob
    private String proofHashes;

    @NotNull
    private Instant computedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getFirstSize() {
        return firstSize;
    }

    public void setFirstSize(Long firstSize) {
        this.firstSize = firstSize;
    }

    public Long getSecondSize() {
        return secondSize;
    }

    public void setSecondSize(Long secondSize) {
        this.secondSize = secondSize;
    }

    public String getProofHashes() {
        return proofHashes;
    }

    public void setProofHashes(String proofHashes) {
        this.proofHashes = proofHashes;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConsistencyProofDTO)) {
            return false;
        }

        ConsistencyProofDTO consistencyProofDTO = (ConsistencyProofDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, consistencyProofDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConsistencyProofDTO{" +
            "id='" + getId() + "'" +
            ", firstSize=" + getFirstSize() +
            ", secondSize=" + getSecondSize() +
            ", proofHashes='" + getProofHashes() + "'" +
            ", computedAt='" + getComputedAt() + "'" +
            "}";
    }
}
