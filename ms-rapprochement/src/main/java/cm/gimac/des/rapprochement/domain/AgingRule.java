package cm.gimac.des.rapprochement.domain;

import cm.gimac.des.rapprochement.domain.enumeration.AgingUrgency;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Règle de vieillissement : J+1, J+5, J+30 (algorithme 6.51, valeurs paramétrables).
 */
@Entity
@Table(name = "aging_rule")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AgingRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Min(value = 1)
    @Column(name = "threshold_days", nullable = false, unique = true)
    private Integer thresholdDays;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    private AgingUrgency urgency;

    /**
     * Rôle destinataire, par exemple « responsable compensation ».
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "recipient_role", length = 100, nullable = false)
    private String recipientRole;

    @NotNull
    @Column(name = "acknowledgement_required", nullable = false)
    private Boolean acknowledgementRequired;

    /**
     * Vrai : la règle s'applique aussi dès le dépassement de l'échéance.
     */
    @NotNull
    @Column(name = "triggered_by_due_date", nullable = false)
    private Boolean triggeredByDueDate;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AgingRule id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getThresholdDays() {
        return this.thresholdDays;
    }

    public AgingRule thresholdDays(Integer thresholdDays) {
        this.setThresholdDays(thresholdDays);
        return this;
    }

    public void setThresholdDays(Integer thresholdDays) {
        this.thresholdDays = thresholdDays;
    }

    public AgingUrgency getUrgency() {
        return this.urgency;
    }

    public AgingRule urgency(AgingUrgency urgency) {
        this.setUrgency(urgency);
        return this;
    }

    public void setUrgency(AgingUrgency urgency) {
        this.urgency = urgency;
    }

    public String getRecipientRole() {
        return this.recipientRole;
    }

    public AgingRule recipientRole(String recipientRole) {
        this.setRecipientRole(recipientRole);
        return this;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public Boolean getAcknowledgementRequired() {
        return this.acknowledgementRequired;
    }

    public AgingRule acknowledgementRequired(Boolean acknowledgementRequired) {
        this.setAcknowledgementRequired(acknowledgementRequired);
        return this;
    }

    public void setAcknowledgementRequired(Boolean acknowledgementRequired) {
        this.acknowledgementRequired = acknowledgementRequired;
    }

    public Boolean getTriggeredByDueDate() {
        return this.triggeredByDueDate;
    }

    public AgingRule triggeredByDueDate(Boolean triggeredByDueDate) {
        this.setTriggeredByDueDate(triggeredByDueDate);
        return this;
    }

    public void setTriggeredByDueDate(Boolean triggeredByDueDate) {
        this.triggeredByDueDate = triggeredByDueDate;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgingRule)) {
            return false;
        }
        return getId() != null && getId().equals(((AgingRule) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AgingRule{" +
            "id=" + getId() +
            ", thresholdDays=" + getThresholdDays() +
            ", urgency='" + getUrgency() + "'" +
            ", recipientRole='" + getRecipientRole() + "'" +
            ", acknowledgementRequired='" + getAcknowledgementRequired() + "'" +
            ", triggeredByDueDate='" + getTriggeredByDueDate() + "'" +
            "}";
    }
}
