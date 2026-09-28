package cm.gimac.des.audit.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Preuve d'inclusion d'une feuille dans un arbre de taille donnée (algorithme 6.13, RG-09).
 */
@Entity
@Table(name = "inclusion_proof")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class InclusionProof implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Min(value = 0L)
    @Column(name = "event_index", nullable = false)
    private Long eventIndex;

    @NotNull
    @Min(value = 1L)
    @Column(name = "tree_size", nullable = false)
    private Long treeSize;

    /**
     * Chemin d'audit : liste JSON des empreintes sœurs, du bas vers le haut.
     */
    @Lob
    @Column(name = "audit_path", nullable = false)
    private String auditPath;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "root_hash", length = 64, nullable = false)
    private String rootHash;

    @NotNull
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "previousRoot", "consistencyWithPrevious", "periodRoot" }, allowSetters = true)
    private PeriodRoot periodRoot;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public InclusionProof id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getEventIndex() {
        return this.eventIndex;
    }

    public InclusionProof eventIndex(Long eventIndex) {
        this.setEventIndex(eventIndex);
        return this;
    }

    public void setEventIndex(Long eventIndex) {
        this.eventIndex = eventIndex;
    }

    public Long getTreeSize() {
        return this.treeSize;
    }

    public InclusionProof treeSize(Long treeSize) {
        this.setTreeSize(treeSize);
        return this;
    }

    public void setTreeSize(Long treeSize) {
        this.treeSize = treeSize;
    }

    public String getAuditPath() {
        return this.auditPath;
    }

    public InclusionProof auditPath(String auditPath) {
        this.setAuditPath(auditPath);
        return this;
    }

    public void setAuditPath(String auditPath) {
        this.auditPath = auditPath;
    }

    public String getRootHash() {
        return this.rootHash;
    }

    public InclusionProof rootHash(String rootHash) {
        this.setRootHash(rootHash);
        return this;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public Instant getGeneratedAt() {
        return this.generatedAt;
    }

    public InclusionProof generatedAt(Instant generatedAt) {
        this.setGeneratedAt(generatedAt);
        return this;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public PeriodRoot getPeriodRoot() {
        return this.periodRoot;
    }

    public void setPeriodRoot(PeriodRoot periodRoot) {
        this.periodRoot = periodRoot;
    }

    public InclusionProof periodRoot(PeriodRoot periodRoot) {
        this.setPeriodRoot(periodRoot);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InclusionProof)) {
            return false;
        }
        return getId() != null && getId().equals(((InclusionProof) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "InclusionProof{" +
            "id=" + getId() +
            ", eventIndex=" + getEventIndex() +
            ", treeSize=" + getTreeSize() +
            ", auditPath='" + getAuditPath() + "'" +
            ", rootHash='" + getRootHash() + "'" +
            ", generatedAt='" + getGeneratedAt() + "'" +
            "}";
    }
}
