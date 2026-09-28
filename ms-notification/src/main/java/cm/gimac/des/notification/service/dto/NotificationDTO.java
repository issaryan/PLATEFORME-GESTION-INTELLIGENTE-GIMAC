package cm.gimac.des.notification.service.dto;

import cm.gimac.des.notification.domain.enumeration.NotificationState;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.notification.domain.Notification} entity.
 */
@Schema(
    description = "Notification demandée par un service (bus ou port synchrone), ou synthèse\nd'une fenêtre de consolidation (RG-62 : niveau d'urgence obligatoire)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 150)
    private String notificationType;

    @NotNull
    private UrgencyLevel urgency;

    @NotNull
    @Size(max = 255)
    @Schema(
        description = "Titre affiché dans la plateforme ; jamais transmis sur un canal externe (RG-64).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String subject;

    @Size(max = 100)
    private String objectType;

    @Size(max = 100)
    private String objectId;

    @Schema(description = "Variables du gabarit, en JSON (objet clé-valeur).")
    @Lob
    private String variables;

    @Schema(description = "Institution concernée (MS-PARTICIPANT), qui borne les règles par institution.")
    private UUID institutionId;

    @Schema(description = "Identifiant CloudEvents de la demande : clé d'idempotence (RG-15, RG-35).")
    private UUID sourceEventId;

    @NotNull
    @Size(max = 50)
    @Schema(description = "Service émetteur, par exemple MS-INCIDENT.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceService;

    @NotNull
    @Schema(description = "Vrai pour une demande reçue par le port synchrone d'urgence S1.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean urgentPort;

    private UUID correlationId;

    @Size(max = 255)
    @Schema(description = "Clé de regroupement (type:objet) pour la consolidation.")
    private String groupKey;

    @NotNull
    private Instant requestedAt;

    private Instant ackDeadline;

    @NotNull
    @Min(value = 0)
    @Schema(description = "Niveau d'escalade courant, de 0 au maximum de la politique.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer escalationLevel;

    @NotNull
    private NotificationState state;

    private UUID acknowledgedBy;

    private Instant acknowledgedAt;

    @NotNull
    @Schema(description = "Vrai pour une synthèse de consolidation.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean summary;

    @Min(value = 0)
    @Schema(description = "Nombre d'occurrences regroupées, pour une synthèse.")
    private Integer occurrenceCount;

    @Schema(description = "Signature détachée (JWS) de la plateforme, pour une escalade S1 ou S2 (RG-71).")
    @Lob
    private String signature;

    @Schema(description = "Fenêtre à laquelle une notification retenue est rattachée.")
    private ConsolidationWindowDTO consolidationWindow;

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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public Boolean getUrgentPort() {
        return urgentPort;
    }

    public void setUrgentPort(Boolean urgentPort) {
        this.urgentPort = urgentPort;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getAckDeadline() {
        return ackDeadline;
    }

    public void setAckDeadline(Instant ackDeadline) {
        this.ackDeadline = ackDeadline;
    }

    public Integer getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(Integer escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public NotificationState getState() {
        return state;
    }

    public void setState(NotificationState state) {
        this.state = state;
    }

    public UUID getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(UUID acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Boolean getSummary() {
        return summary;
    }

    public void setSummary(Boolean summary) {
        this.summary = summary;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public ConsolidationWindowDTO getConsolidationWindow() {
        return consolidationWindow;
    }

    public void setConsolidationWindow(ConsolidationWindowDTO consolidationWindow) {
        this.consolidationWindow = consolidationWindow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationDTO)) {
            return false;
        }

        NotificationDTO notificationDTO = (NotificationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, notificationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NotificationDTO{" +
            "id='" + getId() + "'" +
            ", notificationType='" + getNotificationType() + "'" +
            ", urgency='" + getUrgency() + "'" +
            ", subject='" + getSubject() + "'" +
            ", objectType='" + getObjectType() + "'" +
            ", objectId='" + getObjectId() + "'" +
            ", variables='" + getVariables() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", sourceEventId='" + getSourceEventId() + "'" +
            ", sourceService='" + getSourceService() + "'" +
            ", urgentPort='" + getUrgentPort() + "'" +
            ", correlationId='" + getCorrelationId() + "'" +
            ", groupKey='" + getGroupKey() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", ackDeadline='" + getAckDeadline() + "'" +
            ", escalationLevel=" + getEscalationLevel() +
            ", state='" + getState() + "'" +
            ", acknowledgedBy='" + getAcknowledgedBy() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            ", summary='" + getSummary() + "'" +
            ", occurrenceCount=" + getOccurrenceCount() +
            ", signature='" + getSignature() + "'" +
            ", consolidationWindow=" + getConsolidationWindow() +
            "}";
    }
}
