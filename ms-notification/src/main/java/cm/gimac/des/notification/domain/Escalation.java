package cm.gimac.des.notification.domain;

import cm.gimac.des.notification.domain.enumeration.EscalationSource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Escalade vers un suppléant à l'expiration du délai d'acquittement (algorithme 6.23).
 */
@Entity
@Table(name = "escalation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Escalation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    @NotNull
    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Column(name = "level", nullable = false)
    private Integer level;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private EscalationSource source;

    @NotNull
    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @NotNull
    @Size(max = 255)
    @Column(name = "reason", length = 255, nullable = false)
    private String reason;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "consolidationWindow", "summarizedWindow" }, allowSetters = true)
    private Notification notification;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Escalation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFromUserId() {
        return this.fromUserId;
    }

    public Escalation fromUserId(UUID fromUserId) {
        this.setFromUserId(fromUserId);
        return this;
    }

    public void setFromUserId(UUID fromUserId) {
        this.fromUserId = fromUserId;
    }

    public UUID getToUserId() {
        return this.toUserId;
    }

    public Escalation toUserId(UUID toUserId) {
        this.setToUserId(toUserId);
        return this;
    }

    public void setToUserId(UUID toUserId) {
        this.toUserId = toUserId;
    }

    public Integer getLevel() {
        return this.level;
    }

    public Escalation level(Integer level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public EscalationSource getSource() {
        return this.source;
    }

    public Escalation source(EscalationSource source) {
        this.setSource(source);
        return this;
    }

    public void setSource(EscalationSource source) {
        this.source = source;
    }

    public Instant getTriggeredAt() {
        return this.triggeredAt;
    }

    public Escalation triggeredAt(Instant triggeredAt) {
        this.setTriggeredAt(triggeredAt);
        return this;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public String getReason() {
        return this.reason;
    }

    public Escalation reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Notification getNotification() {
        return this.notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Escalation notification(Notification notification) {
        this.setNotification(notification);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Escalation)) {
            return false;
        }
        return getId() != null && getId().equals(((Escalation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Escalation{" +
            "id=" + getId() +
            ", fromUserId='" + getFromUserId() + "'" +
            ", toUserId='" + getToUserId() + "'" +
            ", level=" + getLevel() +
            ", source='" + getSource() + "'" +
            ", triggeredAt='" + getTriggeredAt() + "'" +
            ", reason='" + getReason() + "'" +
            "}";
    }
}
