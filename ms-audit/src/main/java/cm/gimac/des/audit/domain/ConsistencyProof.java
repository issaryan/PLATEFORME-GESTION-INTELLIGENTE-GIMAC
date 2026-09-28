package cm.gimac.des.audit.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Preuve de cohérence entre deux racines successives, calculée au
 * scellement et conservée (algorithme 6.14).
 */
@Entity
@Table(name = "consistency_proof")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConsistencyProof implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Min(value = 0L)
    @Column(name = "first_size", nullable = false)
    private Long firstSize;

    @NotNull
    @Min(value = 0L)
    @Column(name = "second_size", nullable = false)
    private Long secondSize;

    /**
     * Liste JSON des empreintes de la preuve, en hexadécimal.
     */
    @Lob
    @Column(name = "proof_hashes", nullable = false)
    private String proofHashes;

    @NotNull
    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @JsonIgnoreProperties(value = { "previousRoot", "consistencyWithPrevious", "periodRoot" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "consistencyWithPrevious")
    private PeriodRoot periodRoot;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ConsistencyProof id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getFirstSize() {
        return this.firstSize;
    }

    public ConsistencyProof firstSize(Long firstSize) {
        this.setFirstSize(firstSize);
        return this;
    }

    public void setFirstSize(Long firstSize) {
        this.firstSize = firstSize;
    }

    public Long getSecondSize() {
        return this.secondSize;
    }

    public ConsistencyProof secondSize(Long secondSize) {
        this.setSecondSize(secondSize);
        return this;
    }

    public void setSecondSize(Long secondSize) {
        this.secondSize = secondSize;
    }

    public String getProofHashes() {
        return this.proofHashes;
    }

    public ConsistencyProof proofHashes(String proofHashes) {
        this.setProofHashes(proofHashes);
        return this;
    }

    public void setProofHashes(String proofHashes) {
        this.proofHashes = proofHashes;
    }

    public Instant getComputedAt() {
        return this.computedAt;
    }

    public ConsistencyProof computedAt(Instant computedAt) {
        this.setComputedAt(computedAt);
        return this;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public PeriodRoot getPeriodRoot() {
        return this.periodRoot;
    }

    public void setPeriodRoot(PeriodRoot periodRoot) {
        if (this.periodRoot != null) {
            this.periodRoot.setConsistencyWithPrevious(null);
        }
        if (periodRoot != null) {
            periodRoot.setConsistencyWithPrevious(this);
        }
        this.periodRoot = periodRoot;
    }

    public ConsistencyProof periodRoot(PeriodRoot periodRoot) {
        this.setPeriodRoot(periodRoot);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConsistencyProof)) {
            return false;
        }
        return getId() != null && getId().equals(((ConsistencyProof) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConsistencyProof{" +
            "id=" + getId() +
            ", firstSize=" + getFirstSize() +
            ", secondSize=" + getSecondSize() +
            ", proofHashes='" + getProofHashes() + "'" +
            ", computedAt='" + getComputedAt() + "'" +
            "}";
    }
}
