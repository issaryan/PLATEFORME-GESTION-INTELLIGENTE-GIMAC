package cm.gimac.des.incident.service.dto;

import cm.gimac.des.incident.domain.enumeration.EscalationScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.Escalation} entity.
 */
@Schema(
    description = "Escalade déclenchée (algorithme 6.41) : un palier n'est franchi qu'une fois,\nunicité (ticket, portée, niveau) posée en base."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EscalationDTO implements Serializable {

    private UUID id;

    @NotNull
    private EscalationScope scope;

    @NotNull
    @Min(value = 1)
    @Max(value = 4)
    @Schema(description = "Niveau N1 à N4.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer level;

    @Schema(description = "Destinataire interne (MS-IAM), après substitution du délégué actif.")
    private UUID targetUserId;

    @Schema(description = "Contact d'escalade de l'institution (MS-PARTICIPANT).")
    private UUID targetContactId;

    @NotNull
    private Instant triggeredAt;

    @NotNull
    @Size(max = 500)
    private String reason;

    @Schema(description = "Délai imputable au moment du franchissement.")
    private Duration imputableDelay;

    @NotNull
    private TicketDTO ticket;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EscalationScope getScope() {
        return scope;
    }

    public void setScope(EscalationScope scope) {
        this.scope = scope;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public UUID getTargetContactId() {
        return targetContactId;
    }

    public void setTargetContactId(UUID targetContactId) {
        this.targetContactId = targetContactId;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Duration getImputableDelay() {
        return imputableDelay;
    }

    public void setImputableDelay(Duration imputableDelay) {
        this.imputableDelay = imputableDelay;
    }

    public TicketDTO getTicket() {
        return ticket;
    }

    public void setTicket(TicketDTO ticket) {
        this.ticket = ticket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EscalationDTO)) {
            return false;
        }

        EscalationDTO escalationDTO = (EscalationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, escalationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EscalationDTO{" +
            "id='" + getId() + "'" +
            ", scope='" + getScope() + "'" +
            ", level=" + getLevel() +
            ", targetUserId='" + getTargetUserId() + "'" +
            ", targetContactId='" + getTargetContactId() + "'" +
            ", triggeredAt='" + getTriggeredAt() + "'" +
            ", reason='" + getReason() + "'" +
            ", imputableDelay='" + getImputableDelay() + "'" +
            ", ticket=" + getTicket() +
            "}";
    }
}
