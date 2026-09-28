package cm.gimac.des.iam.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.iam.domain.Permission} entity.
 */
@Schema(description = "Permission unitaire, ventilée par microservice (matrice des permissions, UC-SOC-05).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PermissionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    private String code;

    @NotNull
    @Size(max = 100)
    private String resource;

    @NotNull
    @Size(max = 50)
    private String action;

    @NotNull
    @Size(max = 100)
    @Schema(description = "Microservice ou domaine cible, par exemple ms-monitoring.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetDomain;

    private Set<RoleDTO> roleses = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetDomain() {
        return targetDomain;
    }

    public void setTargetDomain(String targetDomain) {
        this.targetDomain = targetDomain;
    }

    public Set<RoleDTO> getRoleses() {
        return roleses;
    }

    public void setRoleses(Set<RoleDTO> roleses) {
        this.roleses = roleses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PermissionDTO)) {
            return false;
        }

        PermissionDTO permissionDTO = (PermissionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, permissionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PermissionDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", resource='" + getResource() + "'" +
            ", action='" + getAction() + "'" +
            ", targetDomain='" + getTargetDomain() + "'" +
            ", roleses=" + getRoleses() +
            "}";
    }
}
