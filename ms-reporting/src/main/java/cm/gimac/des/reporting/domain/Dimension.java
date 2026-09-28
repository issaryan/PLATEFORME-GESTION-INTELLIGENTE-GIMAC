package cm.gimac.des.reporting.domain;

import cm.gimac.des.reporting.domain.enumeration.DimensionKind;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Membre d'une dimension conforme, par exemple une institution rattachée à son pays.
 */
@Entity
@Table(name = "dimension")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Dimension implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private DimensionKind kind;

    @NotNull
    @Size(max = 100)
    @Column(name = "key", length = 100, nullable = false)
    private String key;

    @NotNull
    @Size(max = 255)
    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @Size(max = 100)
    @Column(name = "parent_key", length = 100)
    private String parentKey;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Dimension id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DimensionKind getKind() {
        return this.kind;
    }

    public Dimension kind(DimensionKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(DimensionKind kind) {
        this.kind = kind;
    }

    public String getKey() {
        return this.key;
    }

    public Dimension key(String key) {
        this.setKey(key);
        return this;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return this.label;
    }

    public Dimension label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getParentKey() {
        return this.parentKey;
    }

    public Dimension parentKey(String parentKey) {
        this.setParentKey(parentKey);
        return this;
    }

    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Dimension updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dimension)) {
            return false;
        }
        return getId() != null && getId().equals(((Dimension) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Dimension{" +
            "id=" + getId() +
            ", kind='" + getKind() + "'" +
            ", key='" + getKey() + "'" +
            ", label='" + getLabel() + "'" +
            ", parentKey='" + getParentKey() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
