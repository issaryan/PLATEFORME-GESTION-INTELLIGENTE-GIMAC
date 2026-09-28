package cm.gimac.des.rapprochement.service.dto;

import cm.gimac.des.rapprochement.domain.enumeration.AgingUrgency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.AgingRule} entity.
 */
@Schema(description = "Règle de vieillissement : J+1, J+5, J+30 (algorithme 6.51, valeurs paramétrables).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AgingRuleDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 1)
    private Integer thresholdDays;

    @NotNull
    private AgingUrgency urgency;

    @NotNull
    @Size(max = 100)
    @Schema(description = "Rôle destinataire, par exemple « responsable compensation ».", requiredMode = Schema.RequiredMode.REQUIRED)
    private String recipientRole;

    @NotNull
    private Boolean acknowledgementRequired;

    @NotNull
    @Schema(description = "Vrai : la règle s'applique aussi dès le dépassement de l'échéance.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean triggeredByDueDate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getThresholdDays() {
        return thresholdDays;
    }

    public void setThresholdDays(Integer thresholdDays) {
        this.thresholdDays = thresholdDays;
    }

    public AgingUrgency getUrgency() {
        return urgency;
    }

    public void setUrgency(AgingUrgency urgency) {
        this.urgency = urgency;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public Boolean getAcknowledgementRequired() {
        return acknowledgementRequired;
    }

    public void setAcknowledgementRequired(Boolean acknowledgementRequired) {
        this.acknowledgementRequired = acknowledgementRequired;
    }

    public Boolean getTriggeredByDueDate() {
        return triggeredByDueDate;
    }

    public void setTriggeredByDueDate(Boolean triggeredByDueDate) {
        this.triggeredByDueDate = triggeredByDueDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgingRuleDTO)) {
            return false;
        }

        AgingRuleDTO agingRuleDTO = (AgingRuleDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, agingRuleDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AgingRuleDTO{" +
            "id='" + getId() + "'" +
            ", thresholdDays=" + getThresholdDays() +
            ", urgency='" + getUrgency() + "'" +
            ", recipientRole='" + getRecipientRole() + "'" +
            ", acknowledgementRequired='" + getAcknowledgementRequired() + "'" +
            ", triggeredByDueDate='" + getTriggeredByDueDate() + "'" +
            "}";
    }
}
