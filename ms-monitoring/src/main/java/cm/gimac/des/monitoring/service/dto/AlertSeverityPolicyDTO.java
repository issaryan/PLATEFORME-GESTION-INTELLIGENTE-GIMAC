package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.NotificationLevel;
import cm.gimac.des.monitoring.domain.enumeration.TicketPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.AlertSeverityPolicy} entity.
 */
@Schema(
    description = "Temporisations et exigences par gravité (tableau 6.48 ; D-14 : acquittement\nen 5, 15 ou 60 min par défaut, paramétrable)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AlertSeverityPolicyDTO implements Serializable {

    private UUID id;

    @NotNull
    private AlertSeverity severity;

    @NotNull
    private Duration ackDelay;

    @NotNull
    private NotificationLevel notificationLevel;

    @NotNull
    @Schema(
        description = "Vrai : port synchrone de MS-NOTIFICATION (gravité critique) ; faux : événement.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean urgentChannel;

    @NotNull
    private TicketPolicy ticketPolicy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public Duration getAckDelay() {
        return ackDelay;
    }

    public void setAckDelay(Duration ackDelay) {
        this.ackDelay = ackDelay;
    }

    public NotificationLevel getNotificationLevel() {
        return notificationLevel;
    }

    public void setNotificationLevel(NotificationLevel notificationLevel) {
        this.notificationLevel = notificationLevel;
    }

    public Boolean getUrgentChannel() {
        return urgentChannel;
    }

    public void setUrgentChannel(Boolean urgentChannel) {
        this.urgentChannel = urgentChannel;
    }

    public TicketPolicy getTicketPolicy() {
        return ticketPolicy;
    }

    public void setTicketPolicy(TicketPolicy ticketPolicy) {
        this.ticketPolicy = ticketPolicy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AlertSeverityPolicyDTO)) {
            return false;
        }

        AlertSeverityPolicyDTO alertSeverityPolicyDTO = (AlertSeverityPolicyDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, alertSeverityPolicyDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AlertSeverityPolicyDTO{" +
            "id='" + getId() + "'" +
            ", severity='" + getSeverity() + "'" +
            ", ackDelay='" + getAckDelay() + "'" +
            ", notificationLevel='" + getNotificationLevel() + "'" +
            ", urgentChannel='" + getUrgentChannel() + "'" +
            ", ticketPolicy='" + getTicketPolicy() + "'" +
            "}";
    }
}
