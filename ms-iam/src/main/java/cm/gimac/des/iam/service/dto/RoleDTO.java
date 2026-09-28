package cm.gimac.des.iam.service.dto;

import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import cm.gimac.des.iam.domain.enumeration.RoleFamily;
import cm.gimac.des.iam.domain.enumeration.VisibilityScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.iam.domain.Role} entity.
 */
@Schema(description = "Rôle applicatif, composé de permissions (CC §6.1.2.1).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoleDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    private String code;

    @NotNull
    @Size(max = 255)
    private String label;

    @NotNull
    private HabilitationLevel level;

    @NotNull
    private RoleFamily family;

    @NotNull
    private VisibilityScope visibilityScope;

    @Size(max = 255)
    @Schema(description = "Groupe de l'annuaire traduit en ce rôle, s'il existe.")
    private String directoryGroup;

    private Set<PermissionDTO> permissionses = new HashSet<>();

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public HabilitationLevel getLevel() {
        return level;
    }

    public void setLevel(HabilitationLevel level) {
        this.level = level;
    }

    public RoleFamily getFamily() {
        return family;
    }

    public void setFamily(RoleFamily family) {
        this.family = family;
    }

    public VisibilityScope getVisibilityScope() {
        return visibilityScope;
    }

    public void setVisibilityScope(VisibilityScope visibilityScope) {
        this.visibilityScope = visibilityScope;
    }

    public String getDirectoryGroup() {
        return directoryGroup;
    }

    public void setDirectoryGroup(String directoryGroup) {
        this.directoryGroup = directoryGroup;
    }

    public Set<PermissionDTO> getPermissionses() {
        return permissionses;
    }

    public void setPermissionses(Set<PermissionDTO> permissionses) {
        this.permissionses = permissionses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleDTO)) {
            return false;
        }

        RoleDTO roleDTO = (RoleDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roleDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoleDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", level='" + getLevel() + "'" +
            ", family='" + getFamily() + "'" +
            ", visibilityScope='" + getVisibilityScope() + "'" +
            ", directoryGroup='" + getDirectoryGroup() + "'" +
            ", permissionses=" + getPermissionses() +
            "}";
    }
}
