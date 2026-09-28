package cm.gimac.des.pilotage.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.Board} entity.
 */
@Schema(description = "Tableau Kanban d'un service ou pôle du DES (exploitation, supervision,\nhomologation, conformité).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BoardDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    @Schema(description = "Code du service ou pôle propriétaire, par exemple HOMOLOGATION.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceCode;

    @NotNull
    @Size(max = 150)
    private String name;

    @NotNull
    @Min(value = 0L)
    @Schema(
        description = "Version incrémentée à chaque déplacement (algorithme 6.54, étape 5).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BoardDTO)) {
            return false;
        }

        BoardDTO boardDTO = (BoardDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, boardDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BoardDTO{" +
            "id='" + getId() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", name='" + getName() + "'" +
            ", version=" + getVersion() +
            "}";
    }
}
