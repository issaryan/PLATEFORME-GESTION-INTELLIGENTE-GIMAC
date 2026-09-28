package cm.gimac.des.document.service.dto;

import cm.gimac.des.document.domain.enumeration.IntegrityResult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.document.domain.IntegrityCheck} entity.
 */
@Schema(description = "Contrôle d'intégrité nocturne par échantillonnage (algorithme 6.20, DOC-05).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IntegrityCheckDTO implements Serializable {

    private UUID id;

    @NotNull
    private Instant startedAt;

    private Instant finishedAt;

    @NotNull
    @Min(value = 1)
    @Schema(description = "Taille d'échantillon : 200 par défaut.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sampleSize;

    @NotNull
    @Min(value = 0)
    private Integer mismatches;

    @NotNull
    @Min(value = 0)
    private Integer unreadable;

    @NotNull
    private IntegrityResult result;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Integer getMismatches() {
        return mismatches;
    }

    public void setMismatches(Integer mismatches) {
        this.mismatches = mismatches;
    }

    public Integer getUnreadable() {
        return unreadable;
    }

    public void setUnreadable(Integer unreadable) {
        this.unreadable = unreadable;
    }

    public IntegrityResult getResult() {
        return result;
    }

    public void setResult(IntegrityResult result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegrityCheckDTO)) {
            return false;
        }

        IntegrityCheckDTO integrityCheckDTO = (IntegrityCheckDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, integrityCheckDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IntegrityCheckDTO{" +
            "id='" + getId() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", finishedAt='" + getFinishedAt() + "'" +
            ", sampleSize=" + getSampleSize() +
            ", mismatches=" + getMismatches() +
            ", unreadable=" + getUnreadable() +
            ", result='" + getResult() + "'" +
            "}";
    }
}
