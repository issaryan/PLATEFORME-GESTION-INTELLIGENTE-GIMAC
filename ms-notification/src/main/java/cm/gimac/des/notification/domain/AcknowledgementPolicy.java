package cm.gimac.des.notification.domain;

import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.UUID;

/**
 * Délai d'acquittement et profondeur d'escalade, paramétrables par type
 * (D-14) : CRITIQUE 10 min, URGENT 30 min, 3 niveaux (valeurs par défaut du CC).
 * Un type vide désigne la règle par défaut du niveau.
 */
@Entity
@Table(name = "acknowledgement_policy")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AcknowledgementPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Size(max = 150)
    @Column(name = "notification_type", length = 150)
    private String notificationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    private UrgencyLevel urgency;

    /**
     * Délai d'acquittement initial (ISO 8601), par exemple PT10M.
     */
    @NotNull
    @Column(name = "ack_delay", nullable = false)
    private Duration ackDelay;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    @Column(name = "max_escalation_level", nullable = false)
    private Integer maxEscalationLevel;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AcknowledgementPolicy id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNotificationType() {
        return this.notificationType;
    }

    public AcknowledgementPolicy notificationType(String notificationType) {
        this.setNotificationType(notificationType);
        return this;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public UrgencyLevel getUrgency() {
        return this.urgency;
    }

    public AcknowledgementPolicy urgency(UrgencyLevel urgency) {
        this.setUrgency(urgency);
        return this;
    }

    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public Duration getAckDelay() {
        return this.ackDelay;
    }

    public AcknowledgementPolicy ackDelay(Duration ackDelay) {
        this.setAckDelay(ackDelay);
        return this;
    }

    public void setAckDelay(Duration ackDelay) {
        this.ackDelay = ackDelay;
    }

    public Integer getMaxEscalationLevel() {
        return this.maxEscalationLevel;
    }

    public AcknowledgementPolicy maxEscalationLevel(Integer maxEscalationLevel) {
        this.setMaxEscalationLevel(maxEscalationLevel);
        return this;
    }

    public void setMaxEscalationLevel(Integer maxEscalationLevel) {
        this.maxEscalationLevel = maxEscalationLevel;
    }

    public Boolean getActive() {
        return this.active;
    }

    public AcknowledgementPolicy active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AcknowledgementPolicy)) {
            return false;
        }
        return getId() != null && getId().equals(((AcknowledgementPolicy) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AcknowledgementPolicy{" +
            "id=" + getId() +
            ", notificationType='" + getNotificationType() + "'" +
            ", urgency='" + getUrgency() + "'" +
            ", ackDelay='" + getAckDelay() + "'" +
            ", maxEscalationLevel=" + getMaxEscalationLevel() +
            ", active='" + getActive() + "'" +
            "}";
    }
}
