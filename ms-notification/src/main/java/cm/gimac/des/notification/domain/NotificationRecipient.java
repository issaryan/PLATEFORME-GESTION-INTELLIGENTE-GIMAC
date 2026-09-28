package cm.gimac.des.notification.domain;

import cm.gimac.des.notification.domain.enumeration.RuleType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Destinataire résolu à l'instant de la demande (objet-valeur Recipient du
 * CC, persisté par l'algorithme 6.21) ; porte l'état « lu » du centre de notifications.
 */
@Entity
@Table(name = "notification_recipient")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationRecipient implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Institution du destinataire, s'il est correspondant d'un participant.
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_by", nullable = false)
    private RuleType resolvedBy;

    /**
     * Canaux retenus, séparés par des virgules, par exemple PLATEFORME,POUSSE,MESSAGERIE.
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "channels", length = 100, nullable = false)
    private String channels;

    /**
     * Vrai pour un suppléant atteint par escalade.
     */
    @NotNull
    @Column(name = "escalated", nullable = false)
    private Boolean escalated;

    @NotNull
    @Column(name = "resolved_at", nullable = false)
    private Instant resolvedAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "consolidationWindow", "summarizedWindow" }, allowSetters = true)
    private Notification notification;

    /**
     * Abonnement qui a sélectionné le destinataire ; vide pour un suppléant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private NotificationSubscription sourceSubscription;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public NotificationRecipient id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public NotificationRecipient userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public NotificationRecipient institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public RuleType getResolvedBy() {
        return this.resolvedBy;
    }

    public NotificationRecipient resolvedBy(RuleType resolvedBy) {
        this.setResolvedBy(resolvedBy);
        return this;
    }

    public void setResolvedBy(RuleType resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getChannels() {
        return this.channels;
    }

    public NotificationRecipient channels(String channels) {
        this.setChannels(channels);
        return this;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public Boolean getEscalated() {
        return this.escalated;
    }

    public NotificationRecipient escalated(Boolean escalated) {
        this.setEscalated(escalated);
        return this;
    }

    public void setEscalated(Boolean escalated) {
        this.escalated = escalated;
    }

    public Instant getResolvedAt() {
        return this.resolvedAt;
    }

    public NotificationRecipient resolvedAt(Instant resolvedAt) {
        this.setResolvedAt(resolvedAt);
        return this;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getReadAt() {
        return this.readAt;
    }

    public NotificationRecipient readAt(Instant readAt) {
        this.setReadAt(readAt);
        return this;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getAcknowledgedAt() {
        return this.acknowledgedAt;
    }

    public NotificationRecipient acknowledgedAt(Instant acknowledgedAt) {
        this.setAcknowledgedAt(acknowledgedAt);
        return this;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Notification getNotification() {
        return this.notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public NotificationRecipient notification(Notification notification) {
        this.setNotification(notification);
        return this;
    }

    public NotificationSubscription getSourceSubscription() {
        return this.sourceSubscription;
    }

    public void setSourceSubscription(NotificationSubscription notificationSubscription) {
        this.sourceSubscription = notificationSubscription;
    }

    public NotificationRecipient sourceSubscription(NotificationSubscription notificationSubscription) {
        this.setSourceSubscription(notificationSubscription);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationRecipient)) {
            return false;
        }
        return getId() != null && getId().equals(((NotificationRecipient) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NotificationRecipient{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", resolvedBy='" + getResolvedBy() + "'" +
            ", channels='" + getChannels() + "'" +
            ", escalated='" + getEscalated() + "'" +
            ", resolvedAt='" + getResolvedAt() + "'" +
            ", readAt='" + getReadAt() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            "}";
    }
}
