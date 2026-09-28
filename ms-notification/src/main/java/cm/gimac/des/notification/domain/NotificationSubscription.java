package cm.gimac.des.notification.domain;

import cm.gimac.des.notification.domain.enumeration.RuleType;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Abonnement : associe un type de notification à une règle de destinataires
 * (Subscription du CC, renommé car le nom est réservé par JHipster).
 * Les destinataires nominatifs ne sont jamais stockés ici. Règles versionnées (NOT-02).
 */
@Entity
@Table(name = "notification_subscription")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Type de notification, par exemple incident.escalated.
     */
    @NotNull
    @Size(max = 150)
    @Column(name = "notification_type", length = 150, nullable = false)
    private String notificationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    /**
     * Valeur de la règle : code de rôle, identifiant d'institution, service du DES ou type d'objet.
     */
    @NotNull
    @Size(max = 150)
    @Column(name = "rule_value", length = 150, nullable = false)
    private String ruleValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_urgency", nullable = false)
    private UrgencyLevel minimumUrgency;

    /**
     * Pour une règle par rôle d'opérateur : restreindre aux personnes en vacation ou d'astreinte.
     */
    @NotNull
    @Column(name = "on_duty_only", nullable = false)
    private Boolean onDutyOnly;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @NotNull
    @Min(value = 1)
    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion;

    @NotNull
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public NotificationSubscription id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNotificationType() {
        return this.notificationType;
    }

    public NotificationSubscription notificationType(String notificationType) {
        this.setNotificationType(notificationType);
        return this;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public RuleType getRuleType() {
        return this.ruleType;
    }

    public NotificationSubscription ruleType(RuleType ruleType) {
        this.setRuleType(ruleType);
        return this;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleValue() {
        return this.ruleValue;
    }

    public NotificationSubscription ruleValue(String ruleValue) {
        this.setRuleValue(ruleValue);
        return this;
    }

    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
    }

    public UrgencyLevel getMinimumUrgency() {
        return this.minimumUrgency;
    }

    public NotificationSubscription minimumUrgency(UrgencyLevel minimumUrgency) {
        this.setMinimumUrgency(minimumUrgency);
        return this;
    }

    public void setMinimumUrgency(UrgencyLevel minimumUrgency) {
        this.minimumUrgency = minimumUrgency;
    }

    public Boolean getOnDutyOnly() {
        return this.onDutyOnly;
    }

    public NotificationSubscription onDutyOnly(Boolean onDutyOnly) {
        this.setOnDutyOnly(onDutyOnly);
        return this;
    }

    public void setOnDutyOnly(Boolean onDutyOnly) {
        this.onDutyOnly = onDutyOnly;
    }

    public Boolean getActive() {
        return this.active;
    }

    public NotificationSubscription active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getRuleVersion() {
        return this.ruleVersion;
    }

    public NotificationSubscription ruleVersion(Integer ruleVersion) {
        this.setRuleVersion(ruleVersion);
        return this;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public UUID getUpdatedBy() {
        return this.updatedBy;
    }

    public NotificationSubscription updatedBy(UUID updatedBy) {
        this.setUpdatedBy(updatedBy);
        return this;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public NotificationSubscription updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationSubscription)) {
            return false;
        }
        return getId() != null && getId().equals(((NotificationSubscription) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NotificationSubscription{" +
            "id=" + getId() +
            ", notificationType='" + getNotificationType() + "'" +
            ", ruleType='" + getRuleType() + "'" +
            ", ruleValue='" + getRuleValue() + "'" +
            ", minimumUrgency='" + getMinimumUrgency() + "'" +
            ", onDutyOnly='" + getOnDutyOnly() + "'" +
            ", active='" + getActive() + "'" +
            ", ruleVersion=" + getRuleVersion() +
            ", updatedBy='" + getUpdatedBy() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
