package cm.gimac.des.notification.domain;

import cm.gimac.des.notification.domain.enumeration.NotificationState;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Notification demandée par un service (bus ou port synchrone), ou synthèse
 * d'une fenêtre de consolidation (RG-62 : niveau d'urgence obligatoire).
 */
@Entity
@Table(name = "notification")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 150)
    @Column(name = "notification_type", length = 150, nullable = false)
    private String notificationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    private UrgencyLevel urgency;

    /**
     * Titre affiché dans la plateforme ; jamais transmis sur un canal externe (RG-64).
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    @Size(max = 100)
    @Column(name = "object_type", length = 100)
    private String objectType;

    @Size(max = 100)
    @Column(name = "object_id", length = 100)
    private String objectId;

    /**
     * Variables du gabarit, en JSON (objet clé-valeur).
     */
    @Lob
    @Column(name = "variables")
    private String variables;

    /**
     * Institution concernée (MS-PARTICIPANT), qui borne les règles par institution.
     */
    @Column(name = "institution_id")
    private UUID institutionId;

    /**
     * Identifiant CloudEvents de la demande : clé d'idempotence (RG-15, RG-35).
     */
    @Column(name = "source_event_id", unique = true)
    private UUID sourceEventId;

    /**
     * Service émetteur, par exemple MS-INCIDENT.
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "source_service", length = 50, nullable = false)
    private String sourceService;

    /**
     * Vrai pour une demande reçue par le port synchrone d'urgence S1.
     */
    @NotNull
    @Column(name = "urgent_port", nullable = false)
    private Boolean urgentPort;

    @Column(name = "correlation_id")
    private UUID correlationId;

    /**
     * Clé de regroupement (type:objet) pour la consolidation.
     */
    @Size(max = 255)
    @Column(name = "group_key", length = 255)
    private String groupKey;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "ack_deadline")
    private Instant ackDeadline;

    /**
     * Niveau d'escalade courant, de 0 au maximum de la politique.
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "escalation_level", nullable = false)
    private Integer escalationLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private NotificationState state;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    /**
     * Vrai pour une synthèse de consolidation.
     */
    @NotNull
    @Column(name = "summary", nullable = false)
    private Boolean summary;

    /**
     * Nombre d'occurrences regroupées, pour une synthèse.
     */
    @Min(value = 0)
    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    /**
     * Signature détachée (JWS) de la plateforme, pour une escalade S1 ou S2 (RG-71).
     */
    @Lob
    @Column(name = "signature")
    private String signature;

    /**
     * Fenêtre à laquelle une notification retenue est rattachée.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "summaryNotification" }, allowSetters = true)
    private ConsolidationWindow consolidationWindow;

    @JsonIgnoreProperties(value = { "summaryNotification" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "summaryNotification")
    private ConsolidationWindow summarizedWindow;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Notification id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNotificationType() {
        return this.notificationType;
    }

    public Notification notificationType(String notificationType) {
        this.setNotificationType(notificationType);
        return this;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public UrgencyLevel getUrgency() {
        return this.urgency;
    }

    public Notification urgency(UrgencyLevel urgency) {
        this.setUrgency(urgency);
        return this;
    }

    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public String getSubject() {
        return this.subject;
    }

    public Notification subject(String subject) {
        this.setSubject(subject);
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getObjectType() {
        return this.objectType;
    }

    public Notification objectType(String objectType) {
        this.setObjectType(objectType);
        return this;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectId() {
        return this.objectId;
    }

    public Notification objectId(String objectId) {
        this.setObjectId(objectId);
        return this;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getVariables() {
        return this.variables;
    }

    public Notification variables(String variables) {
        this.setVariables(variables);
        return this;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Notification institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public UUID getSourceEventId() {
        return this.sourceEventId;
    }

    public Notification sourceEventId(UUID sourceEventId) {
        this.setSourceEventId(sourceEventId);
        return this;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getSourceService() {
        return this.sourceService;
    }

    public Notification sourceService(String sourceService) {
        this.setSourceService(sourceService);
        return this;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public Boolean getUrgentPort() {
        return this.urgentPort;
    }

    public Notification urgentPort(Boolean urgentPort) {
        this.setUrgentPort(urgentPort);
        return this;
    }

    public void setUrgentPort(Boolean urgentPort) {
        this.urgentPort = urgentPort;
    }

    public UUID getCorrelationId() {
        return this.correlationId;
    }

    public Notification correlationId(UUID correlationId) {
        this.setCorrelationId(correlationId);
        return this;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getGroupKey() {
        return this.groupKey;
    }

    public Notification groupKey(String groupKey) {
        this.setGroupKey(groupKey);
        return this;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public Instant getRequestedAt() {
        return this.requestedAt;
    }

    public Notification requestedAt(Instant requestedAt) {
        this.setRequestedAt(requestedAt);
        return this;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getAckDeadline() {
        return this.ackDeadline;
    }

    public Notification ackDeadline(Instant ackDeadline) {
        this.setAckDeadline(ackDeadline);
        return this;
    }

    public void setAckDeadline(Instant ackDeadline) {
        this.ackDeadline = ackDeadline;
    }

    public Integer getEscalationLevel() {
        return this.escalationLevel;
    }

    public Notification escalationLevel(Integer escalationLevel) {
        this.setEscalationLevel(escalationLevel);
        return this;
    }

    public void setEscalationLevel(Integer escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public NotificationState getState() {
        return this.state;
    }

    public Notification state(NotificationState state) {
        this.setState(state);
        return this;
    }

    public void setState(NotificationState state) {
        this.state = state;
    }

    public UUID getAcknowledgedBy() {
        return this.acknowledgedBy;
    }

    public Notification acknowledgedBy(UUID acknowledgedBy) {
        this.setAcknowledgedBy(acknowledgedBy);
        return this;
    }

    public void setAcknowledgedBy(UUID acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Instant getAcknowledgedAt() {
        return this.acknowledgedAt;
    }

    public Notification acknowledgedAt(Instant acknowledgedAt) {
        this.setAcknowledgedAt(acknowledgedAt);
        return this;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Boolean getSummary() {
        return this.summary;
    }

    public Notification summary(Boolean summary) {
        this.setSummary(summary);
        return this;
    }

    public void setSummary(Boolean summary) {
        this.summary = summary;
    }

    public Integer getOccurrenceCount() {
        return this.occurrenceCount;
    }

    public Notification occurrenceCount(Integer occurrenceCount) {
        this.setOccurrenceCount(occurrenceCount);
        return this;
    }

    public void setOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public String getSignature() {
        return this.signature;
    }

    public Notification signature(String signature) {
        this.setSignature(signature);
        return this;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public ConsolidationWindow getConsolidationWindow() {
        return this.consolidationWindow;
    }

    public void setConsolidationWindow(ConsolidationWindow consolidationWindow) {
        this.consolidationWindow = consolidationWindow;
    }

    public Notification consolidationWindow(ConsolidationWindow consolidationWindow) {
        this.setConsolidationWindow(consolidationWindow);
        return this;
    }

    public ConsolidationWindow getSummarizedWindow() {
        return this.summarizedWindow;
    }

    public void setSummarizedWindow(ConsolidationWindow consolidationWindow) {
        if (this.summarizedWindow != null) {
            this.summarizedWindow.setSummaryNotification(null);
        }
        if (consolidationWindow != null) {
            consolidationWindow.setSummaryNotification(this);
        }
        this.summarizedWindow = consolidationWindow;
    }

    public Notification summarizedWindow(ConsolidationWindow consolidationWindow) {
        this.setSummarizedWindow(consolidationWindow);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notification)) {
            return false;
        }
        return getId() != null && getId().equals(((Notification) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Notification{" +
            "id=" + getId() +
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
            "}";
    }
}
