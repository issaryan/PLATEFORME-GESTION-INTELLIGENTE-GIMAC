package cm.gimac.des.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Nœud de l'arbre, persisté par niveau et position (niveau 0 = feuille).
 * Nœud interne : SHA256(0x01 || gauche || droite).
 */
@Entity
@Table(name = "merkle_node")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MerkleNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Min(value = 0)
    @Max(value = 64)
    @Column(name = "level", nullable = false)
    private Integer level;

    @NotNull
    @Min(value = 0L)
    @Column(name = "position", nullable = false)
    private Long position;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "hash", length = 64, nullable = false)
    private String hash;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public MerkleNode id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getLevel() {
        return this.level;
    }

    public MerkleNode level(Integer level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getPosition() {
        return this.position;
    }

    public MerkleNode position(Long position) {
        this.setPosition(position);
        return this;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public String getHash() {
        return this.hash;
    }

    public MerkleNode hash(String hash) {
        this.setHash(hash);
        return this;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MerkleNode)) {
            return false;
        }
        return getId() != null && getId().equals(((MerkleNode) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MerkleNode{" +
            "id=" + getId() +
            ", level=" + getLevel() +
            ", position=" + getPosition() +
            ", hash='" + getHash() + "'" +
            "}";
    }
}
