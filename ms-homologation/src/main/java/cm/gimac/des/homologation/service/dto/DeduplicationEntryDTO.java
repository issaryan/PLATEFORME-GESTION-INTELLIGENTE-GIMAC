package cm.gimac.des.homologation.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.DeduplicationEntry} entity.
 */
@Schema(description = "Entrée de déduplication des événements consommés (RG-15, RG-35).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeduplicationEntryDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID eventId;

    @NotNull
    @Size(max = 150)
    private String subject;

    @NotNull
    private Instant processedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeduplicationEntryDTO)) {
            return false;
        }

        DeduplicationEntryDTO deduplicationEntryDTO = (DeduplicationEntryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, deduplicationEntryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DeduplicationEntryDTO{" +
            "id='" + getId() + "'" +
            ", eventId='" + getEventId() + "'" +
            ", subject='" + getSubject() + "'" +
            ", processedAt='" + getProcessedAt() + "'" +
            "}";
    }
}
