package cm.gimac.des.notification.domain;

import cm.gimac.des.notification.domain.enumeration.Channel;
import cm.gimac.des.notification.domain.enumeration.DeliveryState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Trace de remise par couple (destinataire, canal) (RG-62, algorithme 6.22).
 */
@Entity
@Table(name = "delivery_trace")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeliveryTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private DeliveryState state;

    @NotNull
    @Min(value = 0)
    @Max(value = 6)
    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Size(max = 500)
    @Column(name = "last_error", length = 500)
    private String lastError;

    /**
     * Identifiant rendu par le canal (message, publication).
     */
    @Size(max = 255)
    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "consolidationWindow", "summarizedWindow" }, allowSetters = true)
    private Notification notification;

    /**
     * Gabarit utilisé pour la remise.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private MessageTemplate template;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public DeliveryTrace id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public DeliveryTrace userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public DeliveryTrace channel(Channel channel) {
        this.setChannel(channel);
        return this;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public DeliveryState getState() {
        return this.state;
    }

    public DeliveryTrace state(DeliveryState state) {
        this.setState(state);
        return this;
    }

    public void setState(DeliveryState state) {
        this.state = state;
    }

    public Integer getAttempts() {
        return this.attempts;
    }

    public DeliveryTrace attempts(Integer attempts) {
        this.setAttempts(attempts);
        return this;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Instant getNextAttemptAt() {
        return this.nextAttemptAt;
    }

    public DeliveryTrace nextAttemptAt(Instant nextAttemptAt) {
        this.setNextAttemptAt(nextAttemptAt);
        return this;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getLastError() {
        return this.lastError;
    }

    public DeliveryTrace lastError(String lastError) {
        this.setLastError(lastError);
        return this;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getProviderMessageId() {
        return this.providerMessageId;
    }

    public DeliveryTrace providerMessageId(String providerMessageId) {
        this.setProviderMessageId(providerMessageId);
        return this;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public Instant getDeliveredAt() {
        return this.deliveredAt;
    }

    public DeliveryTrace deliveredAt(Instant deliveredAt) {
        this.setDeliveredAt(deliveredAt);
        return this;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getAcknowledgedAt() {
        return this.acknowledgedAt;
    }

    public DeliveryTrace acknowledgedAt(Instant acknowledgedAt) {
        this.setAcknowledgedAt(acknowledgedAt);
        return this;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Notification getNotification() {
        return this.notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public DeliveryTrace notification(Notification notification) {
        this.setNotification(notification);
        return this;
    }

    public MessageTemplate getTemplate() {
        return this.template;
    }

    public void setTemplate(MessageTemplate messageTemplate) {
        this.template = messageTemplate;
    }

    public DeliveryTrace template(MessageTemplate messageTemplate) {
        this.setTemplate(messageTemplate);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeliveryTrace)) {
            return false;
        }
        return getId() != null && getId().equals(((DeliveryTrace) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DeliveryTrace{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", channel='" + getChannel() + "'" +
            ", state='" + getState() + "'" +
            ", attempts=" + getAttempts() +
            ", nextAttemptAt='" + getNextAttemptAt() + "'" +
            ", lastError='" + getLastError() + "'" +
            ", providerMessageId='" + getProviderMessageId() + "'" +
            ", deliveredAt='" + getDeliveredAt() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            "}";
    }
}
