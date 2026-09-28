package cm.gimac.des.notification.service.dto;

import cm.gimac.des.notification.domain.enumeration.RuleType;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.notification.domain.NotificationSubscription} entity.
 */
@Schema(
    description = "Abonnement : associe un type de notification à une règle de destinataires\n(Subscription du CC, renommé car le nom est réservé par JHipster).\nLes destinataires nominatifs ne sont jamais stockés ici. Règles versionnées (NOT-02)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationSubscriptionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 150)
    @Schema(description = "Type de notification, par exemple incident.escalated.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String notificationType;

    @NotNull
    private RuleType ruleType;

    @NotNull
    @Size(max = 150)
    @Schema(
        description = "Valeur de la règle : code de rôle, identifiant d'institution, service du DES ou type d'objet.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String ruleValue;

    @NotNull
    private UrgencyLevel minimumUrgency;

    @NotNull
    @Schema(
        description = "Pour une règle par rôle d'opérateur : restreindre aux personnes en vacation ou d'astreinte.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean onDutyOnly;

    @NotNull
    private Boolean active;

    @NotNull
    @Min(value = 1)
    private Integer ruleVersion;

    @NotNull
    private UUID updatedBy;

    @NotNull
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleValue() {
        return ruleValue;
    }

    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
    }

    public UrgencyLevel getMinimumUrgency() {
        return minimumUrgency;
    }

    public void setMinimumUrgency(UrgencyLevel minimumUrgency) {
        this.minimumUrgency = minimumUrgency;
    }

    public Boolean getOnDutyOnly() {
        return onDutyOnly;
    }

    public void setOnDutyOnly(Boolean onDutyOnly) {
        this.onDutyOnly = onDutyOnly;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationSubscriptionDTO)) {
            return false;
        }

        NotificationSubscriptionDTO notificationSubscriptionDTO = (NotificationSubscriptionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, notificationSubscriptionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NotificationSubscriptionDTO{" +
            "id='" + getId() + "'" +
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
