package cm.gimac.des.notification.service.dto;

import cm.gimac.des.notification.domain.enumeration.RuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.notification.domain.NotificationRecipient} entity.
 */
@Schema(
    description = "Destinataire résolu à l'instant de la demande (objet-valeur Recipient du\nCC, persisté par l'algorithme 6.21) ; porte l'état « lu » du centre de notifications."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationRecipientDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID userId;

    @Schema(description = "Institution du destinataire, s'il est correspondant d'un participant.")
    private UUID institutionId;

    @NotNull
    private RuleType resolvedBy;

    @NotNull
    @Size(max = 100)
    @Schema(
        description = "Canaux retenus, séparés par des virgules, par exemple PLATEFORME,POUSSE,MESSAGERIE.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String channels;

    @NotNull
    @Schema(description = "Vrai pour un suppléant atteint par escalade.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean escalated;

    @NotNull
    private Instant resolvedAt;

    private Instant readAt;

    private Instant acknowledgedAt;

    @NotNull
    private NotificationDTO notification;

    @Schema(description = "Abonnement qui a sélectionné le destinataire ; vide pour un suppléant.")
    private NotificationSubscriptionDTO sourceSubscription;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public RuleType getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(RuleType resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public Boolean getEscalated() {
        return escalated;
    }

    public void setEscalated(Boolean escalated) {
        this.escalated = escalated;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public NotificationDTO getNotification() {
        return notification;
    }

    public void setNotification(NotificationDTO notification) {
        this.notification = notification;
    }

    public NotificationSubscriptionDTO getSourceSubscription() {
        return sourceSubscription;
    }

    public void setSourceSubscription(NotificationSubscriptionDTO sourceSubscription) {
        this.sourceSubscription = sourceSubscription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationRecipientDTO)) {
            return false;
        }

        NotificationRecipientDTO notificationRecipientDTO = (NotificationRecipientDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, notificationRecipientDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NotificationRecipientDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", resolvedBy='" + getResolvedBy() + "'" +
            ", channels='" + getChannels() + "'" +
            ", escalated='" + getEscalated() + "'" +
            ", resolvedAt='" + getResolvedAt() + "'" +
            ", readAt='" + getReadAt() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            ", notification=" + getNotification() +
            ", sourceSubscription=" + getSourceSubscription() +
            "}";
    }
}
