package cm.gimac.des.notification.service.dto;

import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.notification.domain.AcknowledgementPolicy} entity.
 */
@Schema(
    description = "Délai d'acquittement et profondeur d'escalade, paramétrables par type\n(D-14) : CRITIQUE 10 min, URGENT 30 min, 3 niveaux (valeurs par défaut du CC).\nUn type vide désigne la règle par défaut du niveau."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AcknowledgementPolicyDTO implements Serializable {

    private UUID id;

    @Size(max = 150)
    private String notificationType;

    @NotNull
    private UrgencyLevel urgency;

    @NotNull
    @Schema(description = "Délai d'acquittement initial (ISO 8601), par exemple PT10M.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Duration ackDelay;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer maxEscalationLevel;

    @NotNull
    private Boolean active;

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

    public UrgencyLevel getUrgency() {
        return urgency;
    }

    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public Duration getAckDelay() {
        return ackDelay;
    }

    public void setAckDelay(Duration ackDelay) {
        this.ackDelay = ackDelay;
    }

    public Integer getMaxEscalationLevel() {
        return maxEscalationLevel;
    }

    public void setMaxEscalationLevel(Integer maxEscalationLevel) {
        this.maxEscalationLevel = maxEscalationLevel;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AcknowledgementPolicyDTO)) {
            return false;
        }

        AcknowledgementPolicyDTO acknowledgementPolicyDTO = (AcknowledgementPolicyDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, acknowledgementPolicyDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AcknowledgementPolicyDTO{" +
            "id='" + getId() + "'" +
            ", notificationType='" + getNotificationType() + "'" +
            ", urgency='" + getUrgency() + "'" +
            ", ackDelay='" + getAckDelay() + "'" +
            ", maxEscalationLevel=" + getMaxEscalationLevel() +
            ", active='" + getActive() + "'" +
            "}";
    }
}
