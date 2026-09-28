package cm.gimac.des.notification.domain;

import cm.gimac.des.notification.domain.enumeration.Channel;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Préférence de canal externe d'un utilisateur (RG-63) : niveau minimal et
 * plage horaire. URGENT et CRITIQUE ignorent la plage horaire ; le couple
 * (utilisateur, canal) est unique.
 */
@Entity
@Table(name = "channel_preference")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChannelPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * MESSAGERIE ou SMS seulement : les autres canaux ne sont pas débrayables.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_urgency", nullable = false)
    private UrgencyLevel minimumUrgency;

    @Column(name = "window_start")
    private LocalTime windowStart;

    @Column(name = "window_end")
    private LocalTime windowEnd;

    @NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ChannelPreference id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public ChannelPreference userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public ChannelPreference channel(Channel channel) {
        this.setChannel(channel);
        return this;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public UrgencyLevel getMinimumUrgency() {
        return this.minimumUrgency;
    }

    public ChannelPreference minimumUrgency(UrgencyLevel minimumUrgency) {
        this.setMinimumUrgency(minimumUrgency);
        return this;
    }

    public void setMinimumUrgency(UrgencyLevel minimumUrgency) {
        this.minimumUrgency = minimumUrgency;
    }

    public LocalTime getWindowStart() {
        return this.windowStart;
    }

    public ChannelPreference windowStart(LocalTime windowStart) {
        this.setWindowStart(windowStart);
        return this;
    }

    public void setWindowStart(LocalTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalTime getWindowEnd() {
        return this.windowEnd;
    }

    public ChannelPreference windowEnd(LocalTime windowEnd) {
        this.setWindowEnd(windowEnd);
        return this;
    }

    public void setWindowEnd(LocalTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public ChannelPreference enabled(Boolean enabled) {
        this.setEnabled(enabled);
        return this;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelPreference)) {
            return false;
        }
        return getId() != null && getId().equals(((ChannelPreference) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChannelPreference{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", channel='" + getChannel() + "'" +
            ", minimumUrgency='" + getMinimumUrgency() + "'" +
            ", windowStart='" + getWindowStart() + "'" +
            ", windowEnd='" + getWindowEnd() + "'" +
            ", enabled='" + getEnabled() + "'" +
            "}";
    }
}
