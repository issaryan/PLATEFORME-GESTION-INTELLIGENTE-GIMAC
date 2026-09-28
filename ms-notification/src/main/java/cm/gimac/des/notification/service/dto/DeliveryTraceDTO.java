package cm.gimac.des.notification.service.dto;

import cm.gimac.des.notification.domain.enumeration.Channel;
import cm.gimac.des.notification.domain.enumeration.DeliveryState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.notification.domain.DeliveryTrace} entity.
 */
@Schema(description = "Trace de remise par couple (destinataire, canal) (RG-62, algorithme 6.22).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeliveryTraceDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID userId;

    @NotNull
    private Channel channel;

    @NotNull
    private DeliveryState state;

    @NotNull
    @Min(value = 0)
    @Max(value = 6)
    private Integer attempts;

    private Instant nextAttemptAt;

    @Size(max = 500)
    private String lastError;

    @Size(max = 255)
    @Schema(description = "Identifiant rendu par le canal (message, publication).")
    private String providerMessageId;

    private Instant deliveredAt;

    private Instant acknowledgedAt;

    @NotNull
    private NotificationDTO notification;

    @Schema(description = "Gabarit utilisé pour la remise.")
    private MessageTemplateDTO template;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public DeliveryState getState() {
        return state;
    }

    public void setState(DeliveryState state) {
        this.state = state;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public NotificationDTO getNotification() {
        return notification;
    }

    public void setNotification(NotificationDTO notification) {
        this.notification = notification;
    }

    public MessageTemplateDTO getTemplate() {
        return template;
    }

    public void setTemplate(MessageTemplateDTO template) {
        this.template = template;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeliveryTraceDTO)) {
            return false;
        }

        DeliveryTraceDTO deliveryTraceDTO = (DeliveryTraceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, deliveryTraceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DeliveryTraceDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", channel='" + getChannel() + "'" +
            ", state='" + getState() + "'" +
            ", attempts=" + getAttempts() +
            ", nextAttemptAt='" + getNextAttemptAt() + "'" +
            ", lastError='" + getLastError() + "'" +
            ", providerMessageId='" + getProviderMessageId() + "'" +
            ", deliveredAt='" + getDeliveredAt() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            ", notification=" + getNotification() +
            ", template=" + getTemplate() +
            "}";
    }
}
