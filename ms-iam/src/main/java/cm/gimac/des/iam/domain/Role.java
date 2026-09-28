package cm.gimac.des.iam.domain;

import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import cm.gimac.des.iam.domain.enumeration.RoleFamily;
import cm.gimac.des.iam.domain.enumeration.VisibilityScope;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Rôle applicatif, composé de permissions (CC §6.1.2.1).
 */
@Entity
@Table(name = "role")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 80)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Column(name = "code", length = 80, nullable = false, unique = true)
    private String code;

    @NotNull
    @Size(max = 255)
    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private HabilitationLevel level;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "family", nullable = false)
    private RoleFamily family;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", nullable = false)
    private VisibilityScope visibilityScope;

    /**
     * Groupe de l'annuaire traduit en ce rôle, s'il existe.
     */
    @Size(max = 255)
    @Column(name = "directory_group", length = 255)
    private String directoryGroup;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_role__permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permissions_id")
    )
    @JsonIgnoreProperties(value = { "roleses" }, allowSetters = true)
    private Set<Permission> permissionses = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Role id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public Role code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return this.label;
    }

    public Role label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public HabilitationLevel getLevel() {
        return this.level;
    }

    public Role level(HabilitationLevel level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(HabilitationLevel level) {
        this.level = level;
    }

    public RoleFamily getFamily() {
        return this.family;
    }

    public Role family(RoleFamily family) {
        this.setFamily(family);
        return this;
    }

    public void setFamily(RoleFamily family) {
        this.family = family;
    }

    public VisibilityScope getVisibilityScope() {
        return this.visibilityScope;
    }

    public Role visibilityScope(VisibilityScope visibilityScope) {
        this.setVisibilityScope(visibilityScope);
        return this;
    }

    public void setVisibilityScope(VisibilityScope visibilityScope) {
        this.visibilityScope = visibilityScope;
    }

    public String getDirectoryGroup() {
        return this.directoryGroup;
    }

    public Role directoryGroup(String directoryGroup) {
        this.setDirectoryGroup(directoryGroup);
        return this;
    }

    public void setDirectoryGroup(String directoryGroup) {
        this.directoryGroup = directoryGroup;
    }

    public Set<Permission> getPermissionses() {
        return this.permissionses;
    }

    public void setPermissionses(Set<Permission> permissions) {
        this.permissionses = permissions;
    }

    public Role permissionses(Set<Permission> permissions) {
        this.setPermissionses(permissions);
        return this;
    }

    public Role addPermissions(Permission permission) {
        this.permissionses.add(permission);
        return this;
    }

    public Role removePermissions(Permission permission) {
        this.permissionses.remove(permission);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Role)) {
            return false;
        }
        return getId() != null && getId().equals(((Role) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Role{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", level='" + getLevel() + "'" +
            ", family='" + getFamily() + "'" +
            ", visibilityScope='" + getVisibilityScope() + "'" +
            ", directoryGroup='" + getDirectoryGroup() + "'" +
            "}";
    }
}
