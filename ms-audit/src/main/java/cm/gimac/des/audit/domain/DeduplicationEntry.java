package cm.gimac.des.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Entrée de déduplication : clé unique = identifiant CloudEvents (RG-15, RG-35).
 */
@Entity
@Table(name = "deduplication_entry")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeduplicationEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @NotNull
    @Min(value = 0L)
    @Column(name = "audit_index", nullable = false)
    private Long auditIndex;

    @NotNull
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public DeduplicationEntry id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return this.eventId;
    }

    public DeduplicationEntry eventId(UUID eventId) {
        this.setEventId(eventId);
        return this;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Long getAuditIndex() {
        return this.auditIndex;
    }

    public DeduplicationEntry auditIndex(Long auditIndex) {
        this.setAuditIndex(auditIndex);
        return this;
    }

    public void setAuditIndex(Long auditIndex) {
        this.auditIndex = auditIndex;
    }

    public Instant getRecordedAt() {
        return this.recordedAt;
    }

    public DeduplicationEntry recordedAt(Instant recordedAt) {
        this.setRecordedAt(recordedAt);
        return this;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeduplicationEntry)) {
            return false;
        }
        return getId() != null && getId().equals(((DeduplicationEntry) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DeduplicationEntry{" +
            "id=" + getId() +
            ", eventId='" + getEventId() + "'" +
            ", auditIndex=" + getAuditIndex() +
            ", recordedAt='" + getRecordedAt() + "'" +
            "}";
    }
}
