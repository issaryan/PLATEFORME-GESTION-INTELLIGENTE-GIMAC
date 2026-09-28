package cm.gimac.des.pilotage.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Tableau Kanban d'un service ou pôle du DES (exploitation, supervision,
 * homologation, conformité).
 */
@Entity
@Table(name = "board")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Board implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Code du service ou pôle propriétaire, par exemple HOMOLOGATION.
     */
    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    @Column(name = "service_code", length = 30, nullable = false)
    private String serviceCode;

    @NotNull
    @Size(max = 150)
    @Column(name = "name", length = 150, nullable = false)
    private String name;

    /**
     * Version incrémentée à chaque déplacement (algorithme 6.54, étape 5).
     */
    @NotNull
    @Min(value = 0L)
    @Column(name = "version", nullable = false)
    private Long version;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Board id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public Board serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getName() {
        return this.name;
    }

    public Board name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getVersion() {
        return this.version;
    }

    public Board version(Long version) {
        this.setVersion(version);
        return this;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Board)) {
            return false;
        }
        return getId() != null && getId().equals(((Board) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Board{" +
            "id=" + getId() +
            ", serviceCode='" + getServiceCode() + "'" +
            ", name='" + getName() + "'" +
            ", version=" + getVersion() +
            "}";
    }
}
