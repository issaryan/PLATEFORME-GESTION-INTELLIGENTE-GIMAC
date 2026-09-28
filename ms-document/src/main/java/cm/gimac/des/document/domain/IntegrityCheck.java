package cm.gimac.des.document.domain;

import cm.gimac.des.document.domain.enumeration.IntegrityResult;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Contrôle d'intégrité nocturne par échantillonnage (algorithme 6.20, DOC-05).
 */
@Entity
@Table(name = "integrity_check")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IntegrityCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    /**
     * Taille d'échantillon : 200 par défaut.
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;

    @NotNull
    @Min(value = 0)
    @Column(name = "mismatches", nullable = false)
    private Integer mismatches;

    @NotNull
    @Min(value = 0)
    @Column(name = "unreadable", nullable = false)
    private Integer unreadable;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private IntegrityResult result;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public IntegrityCheck id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getStartedAt() {
        return this.startedAt;
    }

    public IntegrityCheck startedAt(Instant startedAt) {
        this.setStartedAt(startedAt);
        return this;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return this.finishedAt;
    }

    public IntegrityCheck finishedAt(Instant finishedAt) {
        this.setFinishedAt(finishedAt);
        return this;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getSampleSize() {
        return this.sampleSize;
    }

    public IntegrityCheck sampleSize(Integer sampleSize) {
        this.setSampleSize(sampleSize);
        return this;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Integer getMismatches() {
        return this.mismatches;
    }

    public IntegrityCheck mismatches(Integer mismatches) {
        this.setMismatches(mismatches);
        return this;
    }

    public void setMismatches(Integer mismatches) {
        this.mismatches = mismatches;
    }

    public Integer getUnreadable() {
        return this.unreadable;
    }

    public IntegrityCheck unreadable(Integer unreadable) {
        this.setUnreadable(unreadable);
        return this;
    }

    public void setUnreadable(Integer unreadable) {
        this.unreadable = unreadable;
    }

    public IntegrityResult getResult() {
        return this.result;
    }

    public IntegrityCheck result(IntegrityResult result) {
        this.setResult(result);
        return this;
    }

    public void setResult(IntegrityResult result) {
        this.result = result;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegrityCheck)) {
            return false;
        }
        return getId() != null && getId().equals(((IntegrityCheck) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IntegrityCheck{" +
            "id=" + getId() +
            ", startedAt='" + getStartedAt() + "'" +
            ", finishedAt='" + getFinishedAt() + "'" +
            ", sampleSize=" + getSampleSize() +
            ", mismatches=" + getMismatches() +
            ", unreadable=" + getUnreadable() +
            ", result='" + getResult() + "'" +
            "}";
    }
}
