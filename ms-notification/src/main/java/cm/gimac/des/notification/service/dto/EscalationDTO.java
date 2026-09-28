package cm.gimac.des.notification.service.dto;

import cm.gimac.des.notification.domain.enumeration.EscalationSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.notification.domain.Escalation} entity.
 */
@Schema(description = "Escalade vers un suppléant à l'expiration du délai d'acquittement (algorithme 6.23).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EscalationDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID fromUserId;

    @NotNull
    private UUID toUserId;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer level;

    @NotNull
    private EscalationSource source;

    @NotNull
    private Instant triggeredAt;

    @NotNull
    @Size(max = 255)
    private String reason;

    @NotNull
    private NotificationDTO notification;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(UUID fromUserId) {
        this.fromUserId = fromUserId;
    }

    public UUID getToUserId() {
        return toUserId;
    }

    public void setToUserId(UUID toUserId) {
        this.toUserId = toUserId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public EscalationSource getSource() {
        return source;
    }

    public void setSource(EscalationSource source) {
        this.source = source;
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

    public NotificationDTO getNotification() {
        return notification;
    }

    public void setNotification(NotificationDTO notification) {
        this.notification = notification;
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
            ", fromUserId='" + getFromUserId() + "'" +
            ", toUserId='" + getToUserId() + "'" +
            ", level=" + getLevel() +
            ", source='" + getSource() + "'" +
            ", triggeredAt='" + getTriggeredAt() + "'" +
            ", reason='" + getReason() + "'" +
            ", notification=" + getNotification() +
            "}";
    }
}
