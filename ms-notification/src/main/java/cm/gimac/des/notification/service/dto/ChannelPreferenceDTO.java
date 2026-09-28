package cm.gimac.des.notification.service.dto;

import cm.gimac.des.notification.domain.enumeration.Channel;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.notification.domain.ChannelPreference} entity.
 */
@Schema(
    description = "Préférence de canal externe d'un utilisateur (RG-63) : niveau minimal et\nplage horaire. URGENT et CRITIQUE ignorent la plage horaire ; le couple\n(utilisateur, canal) est unique."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChannelPreferenceDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID userId;

    @NotNull
    @Schema(
        description = "MESSAGERIE ou SMS seulement : les autres canaux ne sont pas débrayables.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Channel channel;

    @NotNull
    private UrgencyLevel minimumUrgency;

    private LocalTime windowStart;

    private LocalTime windowEnd;

    @NotNull
    private Boolean enabled;

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

    public UrgencyLevel getMinimumUrgency() {
        return minimumUrgency;
    }

    public void setMinimumUrgency(UrgencyLevel minimumUrgency) {
        this.minimumUrgency = minimumUrgency;
    }

    public LocalTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelPreferenceDTO)) {
            return false;
        }

        ChannelPreferenceDTO channelPreferenceDTO = (ChannelPreferenceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, channelPreferenceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChannelPreferenceDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", channel='" + getChannel() + "'" +
            ", minimumUrgency='" + getMinimumUrgency() + "'" +
            ", windowStart='" + getWindowStart() + "'" +
            ", windowEnd='" + getWindowEnd() + "'" +
            ", enabled='" + getEnabled() + "'" +
            "}";
    }
}
