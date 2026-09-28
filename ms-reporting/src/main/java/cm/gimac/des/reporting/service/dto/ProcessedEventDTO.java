package cm.gimac.des.reporting.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.ProcessedEvent} entity.
 */
@Schema(description = "Événement traité : clé unique = identifiant CloudEvents (algorithme 6.56, RG-15, RG-35).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProcessedEventDTO implements Serializable {

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
        if (!(o instanceof ProcessedEventDTO)) {
            return false;
        }

        ProcessedEventDTO processedEventDTO = (ProcessedEventDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, processedEventDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProcessedEventDTO{" +
            "id='" + getId() + "'" +
            ", eventId='" + getEventId() + "'" +
            ", subject='" + getSubject() + "'" +
            ", processedAt='" + getProcessedAt() + "'" +
            "}";
    }
}
