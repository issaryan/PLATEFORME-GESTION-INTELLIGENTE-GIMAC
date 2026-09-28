package cm.gimac.des.reporting.service.dto;

import cm.gimac.des.reporting.domain.enumeration.DimensionKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.Dimension} entity.
 */
@Schema(description = "Membre d'une dimension conforme, par exemple une institution rattachée à son pays.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DimensionDTO implements Serializable {

    private UUID id;

    @NotNull
    private DimensionKind kind;

    @NotNull
    @Size(max = 100)
    private String key;

    @NotNull
    @Size(max = 255)
    private String label;

    @Size(max = 100)
    private String parentKey;

    @NotNull
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DimensionKind getKind() {
        return kind;
    }

    public void setKind(DimensionKind kind) {
        this.kind = kind;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getParentKey() {
        return parentKey;
    }

    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DimensionDTO)) {
            return false;
        }

        DimensionDTO dimensionDTO = (DimensionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, dimensionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DimensionDTO{" +
            "id='" + getId() + "'" +
            ", kind='" + getKind() + "'" +
            ", key='" + getKey() + "'" +
            ", label='" + getLabel() + "'" +
            ", parentKey='" + getParentKey() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
