package cm.gimac.des.certification.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Entrée de déduplication des événements consommés (RG-15, RG-35).
 */
@Entity
@Table(name = "deduplication_entry")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeduplicationEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @NotNull
    @Size(max = 150)
    @Column(name = "subject", length = 150, nullable = false)
    private String subject;

    @NotNull
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

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

    public String getSubject() {
        return this.subject;
    }

    public DeduplicationEntry subject(String subject) {
        this.setSubject(subject);
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Instant getProcessedAt() {
        return this.processedAt;
    }

    public DeduplicationEntry processedAt(Instant processedAt) {
        this.setProcessedAt(processedAt);
        return this;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
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
            ", subject='" + getSubject() + "'" +
            ", processedAt='" + getProcessedAt() + "'" +
            "}";
    }
}
