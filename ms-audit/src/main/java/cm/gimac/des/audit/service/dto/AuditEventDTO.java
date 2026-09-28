package cm.gimac.des.audit.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.audit.domain.AuditEvent} entity.
 */
@Schema(
    description = "Événement d'audit : feuille de l'arbre de Merkle (CC §6.1.3.3).\nL'index est strictement croissant et sans trou ; il est attribué dans la\ntransaction d'inscription, et non par une séquence de la base."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AuditEventDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 0L)
    @Schema(description = "Index de la feuille dans l'arbre, à partir de 0.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long logIndex;

    @NotNull
    @Schema(description = "Identifiant CloudEvents de l'enveloppe reçue.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID eventId;

    @NotNull
    @Schema(description = "Horodatage d'occurrence, fourni par le producteur.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant occurredAt;

    @NotNull
    @Schema(description = "Horodatage d'inscription dans le registre.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant recordedAt;

    @NotNull
    @Size(max = 100)
    private String actorId;

    @NotNull
    @Size(max = 100)
    private String actionType;

    @NotNull
    @Size(max = 50)
    @Schema(description = "Domaine producteur, par exemple homologation.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String domain;

    @NotNull
    @Size(max = 150)
    @Schema(description = "Sujet du bus, par exemple homologation.operation-closed.v1.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    private UUID correlationId;

    @Schema(
        description = "Enveloppe CloudEvents canonique (clés triées, RFC 8259), sans numéro de carte en clair.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Lob
    private String payload;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "SHA256(0x00 || forme canonique).", requiredMode = Schema.RequiredMode.REQUIRED)
    private String leafHash;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getLogIndex() {
        return logIndex;
    }

    public void setLogIndex(Long logIndex) {
        this.logIndex = logIndex;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getLeafHash() {
        return leafHash;
    }

    public void setLeafHash(String leafHash) {
        this.leafHash = leafHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditEventDTO)) {
            return false;
        }

        AuditEventDTO auditEventDTO = (AuditEventDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, auditEventDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AuditEventDTO{" +
            "id='" + getId() + "'" +
            ", logIndex=" + getLogIndex() +
            ", eventId='" + getEventId() + "'" +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", recordedAt='" + getRecordedAt() + "'" +
            ", actorId='" + getActorId() + "'" +
            ", actionType='" + getActionType() + "'" +
            ", domain='" + getDomain() + "'" +
            ", subject='" + getSubject() + "'" +
            ", correlationId='" + getCorrelationId() + "'" +
            ", payload='" + getPayload() + "'" +
            ", leafHash='" + getLeafHash() + "'" +
            "}";
    }
}
