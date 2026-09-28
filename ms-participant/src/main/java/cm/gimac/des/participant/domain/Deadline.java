package cm.gimac.des.participant.domain;

import cm.gimac.des.participant.domain.enumeration.DeadlineState;
import cm.gimac.des.participant.domain.enumeration.DeadlineThreshold;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Échéance armée sur une pièce légale ou une convention (J-90, J-60, J-30,
 * J-0) ; à J-60 et J-30, notification signée avec accusé de réception (RG-69).
 */
@Entity
@Table(name = "deadline")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Deadline implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "threshold", nullable = false)
    private DeadlineThreshold threshold;

    @NotNull
    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private DeadlineState state;

    @Size(max = 255)
    @Column(name = "triggered_action", length = 255)
    private String triggeredAction;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @NotNull
    @Column(name = "acknowledgement_required", nullable = false)
    private Boolean acknowledgementRequired;

    /**
     * Identifiant de la notification dans MS-NOTIFICATION.
     */
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "settlementBank", "sponsorBank" }, allowSetters = true)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "previousDocument", "institution", "legalDocument" }, allowSetters = true)
    private LegalDocument legalDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "institution" }, allowSetters = true)
    private Convention convention;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Deadline id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DeadlineThreshold getThreshold() {
        return this.threshold;
    }

    public Deadline threshold(DeadlineThreshold threshold) {
        this.setThreshold(threshold);
        return this;
    }

    public void setThreshold(DeadlineThreshold threshold) {
        this.threshold = threshold;
    }

    public LocalDate getDueOn() {
        return this.dueOn;
    }

    public Deadline dueOn(LocalDate dueOn) {
        this.setDueOn(dueOn);
        return this;
    }

    public void setDueOn(LocalDate dueOn) {
        this.dueOn = dueOn;
    }

    public DeadlineState getState() {
        return this.state;
    }

    public Deadline state(DeadlineState state) {
        this.setState(state);
        return this;
    }

    public void setState(DeadlineState state) {
        this.state = state;
    }

    public String getTriggeredAction() {
        return this.triggeredAction;
    }

    public Deadline triggeredAction(String triggeredAction) {
        this.setTriggeredAction(triggeredAction);
        return this;
    }

    public void setTriggeredAction(String triggeredAction) {
        this.triggeredAction = triggeredAction;
    }

    public Instant getTriggeredAt() {
        return this.triggeredAt;
    }

    public Deadline triggeredAt(Instant triggeredAt) {
        this.setTriggeredAt(triggeredAt);
        return this;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public Boolean getAcknowledgementRequired() {
        return this.acknowledgementRequired;
    }

    public Deadline acknowledgementRequired(Boolean acknowledgementRequired) {
        this.setAcknowledgementRequired(acknowledgementRequired);
        return this;
    }

    public void setAcknowledgementRequired(Boolean acknowledgementRequired) {
        this.acknowledgementRequired = acknowledgementRequired;
    }

    public UUID getNotificationId() {
        return this.notificationId;
    }

    public Deadline notificationId(UUID notificationId) {
        this.setNotificationId(notificationId);
        return this;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public Instant getAcknowledgedAt() {
        return this.acknowledgedAt;
    }

    public Deadline acknowledgedAt(Instant acknowledgedAt) {
        this.setAcknowledgedAt(acknowledgedAt);
        return this;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Institution getInstitution() {
        return this.institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Deadline institution(Institution institution) {
        this.setInstitution(institution);
        return this;
    }

    public LegalDocument getLegalDocument() {
        return this.legalDocument;
    }

    public void setLegalDocument(LegalDocument legalDocument) {
        this.legalDocument = legalDocument;
    }

    public Deadline legalDocument(LegalDocument legalDocument) {
        this.setLegalDocument(legalDocument);
        return this;
    }

    public Convention getConvention() {
        return this.convention;
    }

    public void setConvention(Convention convention) {
        this.convention = convention;
    }

    public Deadline convention(Convention convention) {
        this.setConvention(convention);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Deadline)) {
            return false;
        }
        return getId() != null && getId().equals(((Deadline) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Deadline{" +
            "id=" + getId() +
            ", threshold='" + getThreshold() + "'" +
            ", dueOn='" + getDueOn() + "'" +
            ", state='" + getState() + "'" +
            ", triggeredAction='" + getTriggeredAction() + "'" +
            ", triggeredAt='" + getTriggeredAt() + "'" +
            ", acknowledgementRequired='" + getAcknowledgementRequired() + "'" +
            ", notificationId='" + getNotificationId() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            "}";
    }
}
