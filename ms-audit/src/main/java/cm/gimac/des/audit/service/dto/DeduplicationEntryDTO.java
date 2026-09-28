package cm.gimac.des.audit.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.audit.domain.DeduplicationEntry} entity.
 */
@Schema(description = "Entrée de déduplication : clé unique = identifiant CloudEvents (RG-15, RG-35).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeduplicationEntryDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID eventId;

    @NotNull
    @Min(value = 0L)
    private Long auditIndex;

    @NotNull
    private Instant recordedAt;

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

    public Long getAuditIndex() {
        return auditIndex;
    }

    public void setAuditIndex(Long auditIndex) {
        this.auditIndex = auditIndex;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
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
            ", auditIndex=" + getAuditIndex() +
            ", recordedAt='" + getRecordedAt() + "'" +
            "}";
    }
}
