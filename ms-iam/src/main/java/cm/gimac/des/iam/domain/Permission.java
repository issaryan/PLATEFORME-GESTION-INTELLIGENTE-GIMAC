package cm.gimac.des.iam.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Permission unitaire, ventilée par microservice (matrice des permissions, UC-SOC-05).
 */
@Entity
@Table(name = "permission")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Permission implements Serializable {

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
    @Size(max = 100)
    @Column(name = "resource", length = 100, nullable = false)
    private String resource;

    @NotNull
    @Size(max = 50)
    @Column(name = "action", length = 50, nullable = false)
    private String action;

    /**
     * Microservice ou domaine cible, par exemple ms-monitoring.
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "target_domain", length = 100, nullable = false)
    private String targetDomain;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "permissionses")
    @JsonIgnoreProperties(value = { "permissionses" }, allowSetters = true)
    private Set<Role> roleses = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Permission id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public Permission code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getResource() {
        return this.resource;
    }

    public Permission resource(String resource) {
        this.setResource(resource);
        return this;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return this.action;
    }

    public Permission action(String action) {
        this.setAction(action);
        return this;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetDomain() {
        return this.targetDomain;
    }

    public Permission targetDomain(String targetDomain) {
        this.setTargetDomain(targetDomain);
        return this;
    }

    public void setTargetDomain(String targetDomain) {
        this.targetDomain = targetDomain;
    }

    public Set<Role> getRoleses() {
        return this.roleses;
    }

    public void setRoleses(Set<Role> roles) {
        if (this.roleses != null) {
            this.roleses.forEach(i -> i.removePermissions(this));
        }
        if (roles != null) {
            roles.forEach(i -> i.addPermissions(this));
        }
        this.roleses = roles;
    }

    public Permission roleses(Set<Role> roles) {
        this.setRoleses(roles);
        return this;
    }

    public Permission addRoles(Role role) {
        this.roleses.add(role);
        role.getPermissionses().add(this);
        return this;
    }

    public Permission removeRoles(Role role) {
        this.roleses.remove(role);
        role.getPermissionses().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Permission)) {
            return false;
        }
        return getId() != null && getId().equals(((Permission) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Permission{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", resource='" + getResource() + "'" +
            ", action='" + getAction() + "'" +
            ", targetDomain='" + getTargetDomain() + "'" +
            "}";
    }
}
