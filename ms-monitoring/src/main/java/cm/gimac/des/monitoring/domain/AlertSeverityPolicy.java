package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.NotificationLevel;
import cm.gimac.des.monitoring.domain.enumeration.TicketPolicy;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.UUID;

/**
 * Temporisations et exigences par gravité (tableau 6.48 ; D-14 : acquittement
 * en 5, 15 ou 60 min par défaut, paramétrable).
 */
@Entity
@Table(name = "alert_severity_policy")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AlertSeverityPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, unique = true)
    private AlertSeverity severity;

    @NotNull
    @Column(name = "ack_delay", nullable = false)
    private Duration ackDelay;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_level", nullable = false)
    private NotificationLevel notificationLevel;

    /**
     * Vrai : port synchrone de MS-NOTIFICATION (gravité critique) ; faux : événement.
     */
    @NotNull
    @Column(name = "urgent_channel", nullable = false)
    private Boolean urgentChannel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_policy", nullable = false)
    private TicketPolicy ticketPolicy;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AlertSeverityPolicy id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AlertSeverity getSeverity() {
        return this.severity;
    }

    public AlertSeverityPolicy severity(AlertSeverity severity) {
        this.setSeverity(severity);
        return this;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public Duration getAckDelay() {
        return this.ackDelay;
    }

    public AlertSeverityPolicy ackDelay(Duration ackDelay) {
        this.setAckDelay(ackDelay);
        return this;
    }

    public void setAckDelay(Duration ackDelay) {
        this.ackDelay = ackDelay;
    }

    public NotificationLevel getNotificationLevel() {
        return this.notificationLevel;
    }

    public AlertSeverityPolicy notificationLevel(NotificationLevel notificationLevel) {
        this.setNotificationLevel(notificationLevel);
        return this;
    }

    public void setNotificationLevel(NotificationLevel notificationLevel) {
        this.notificationLevel = notificationLevel;
    }

    public Boolean getUrgentChannel() {
        return this.urgentChannel;
    }

    public AlertSeverityPolicy urgentChannel(Boolean urgentChannel) {
        this.setUrgentChannel(urgentChannel);
        return this;
    }

    public void setUrgentChannel(Boolean urgentChannel) {
        this.urgentChannel = urgentChannel;
    }

    public TicketPolicy getTicketPolicy() {
        return this.ticketPolicy;
    }

    public AlertSeverityPolicy ticketPolicy(TicketPolicy ticketPolicy) {
        this.setTicketPolicy(ticketPolicy);
        return this;
    }

    public void setTicketPolicy(TicketPolicy ticketPolicy) {
        this.ticketPolicy = ticketPolicy;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AlertSeverityPolicy)) {
            return false;
        }
        return getId() != null && getId().equals(((AlertSeverityPolicy) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AlertSeverityPolicy{" +
            "id=" + getId() +
            ", severity='" + getSeverity() + "'" +
            ", ackDelay='" + getAckDelay() + "'" +
            ", notificationLevel='" + getNotificationLevel() + "'" +
            ", urgentChannel='" + getUrgentChannel() + "'" +
            ", ticketPolicy='" + getTicketPolicy() + "'" +
            "}";
    }
}
