package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.AlertState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.Alert} entity.
 */
@Schema(
    description = "Alerte corrélée et dédupliquée (algorithme 6.44). La gravité n'entre pas\ndans l'empreinte ; une alerte symptôme pointe vers sa racine."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AlertDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Schema(description = "Référence lisible, par exemple ALR-2026-04417.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reference;

    @NotNull
    @Size(max = 100)
    @Schema(
        description = "Code de la source, ou « évaluation des seuils » pour une alerte interne.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String source;

    @NotNull
    @Size(max = 255)
    private String rule;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "SHA256(source || liaison || règle).", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fingerprint;

    @NotNull
    private AlertSeverity severity;

    @NotNull
    private AlertState state;

    @NotNull
    private Instant firstSeenAt;

    @NotNull
    private Instant lastSeenAt;

    @NotNull
    @Min(value = 1)
    private Integer occurrences;

    @Schema(description = "Échéance d'acquittement, pour une alerte racine.")
    private Instant ackDeadline;

    @NotNull
    @Schema(description = "Marquée hors délai par processOverdue.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean overdue;

    private UUID acknowledgedBy;

    private Instant acknowledgedAt;

    private Boolean ticketRequested;

    @Size(max = 1000)
    private String ackComment;

    @Schema(description = "Ticket ouvert par MS-INCIDENT (incident.ticket-opened.v1).")
    private UUID ticketId;

    private Instant resolvedAt;

    @NotNull
    private SupervisedLinkDTO link;

    @Schema(description = "Alerte racine dont celle-ci est un symptôme.")
    private AlertDTO correlationRoot;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public AlertState getState() {
        return state;
    }

    public void setState(AlertState state) {
        this.state = state;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Integer getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(Integer occurrences) {
        this.occurrences = occurrences;
    }

    public Instant getAckDeadline() {
        return ackDeadline;
    }

    public void setAckDeadline(Instant ackDeadline) {
        this.ackDeadline = ackDeadline;
    }

    public Boolean getOverdue() {
        return overdue;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
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

    public Boolean getTicketRequested() {
        return ticketRequested;
    }

    public void setTicketRequested(Boolean ticketRequested) {
        this.ticketRequested = ticketRequested;
    }

    public String getAckComment() {
        return ackComment;
    }

    public void setAckComment(String ackComment) {
        this.ackComment = ackComment;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public SupervisedLinkDTO getLink() {
        return link;
    }

    public void setLink(SupervisedLinkDTO link) {
        this.link = link;
    }

    public AlertDTO getCorrelationRoot() {
        return correlationRoot;
    }

    public void setCorrelationRoot(AlertDTO correlationRoot) {
        this.correlationRoot = correlationRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AlertDTO)) {
            return false;
        }

        AlertDTO alertDTO = (AlertDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, alertDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AlertDTO{" +
            "id='" + getId() + "'" +
            ", reference='" + getReference() + "'" +
            ", source='" + getSource() + "'" +
            ", rule='" + getRule() + "'" +
            ", fingerprint='" + getFingerprint() + "'" +
            ", severity='" + getSeverity() + "'" +
            ", state='" + getState() + "'" +
            ", firstSeenAt='" + getFirstSeenAt() + "'" +
            ", lastSeenAt='" + getLastSeenAt() + "'" +
            ", occurrences=" + getOccurrences() +
            ", ackDeadline='" + getAckDeadline() + "'" +
            ", overdue='" + getOverdue() + "'" +
            ", acknowledgedBy='" + getAcknowledgedBy() + "'" +
            ", acknowledgedAt='" + getAcknowledgedAt() + "'" +
            ", ticketRequested='" + getTicketRequested() + "'" +
            ", ackComment='" + getAckComment() + "'" +
            ", ticketId='" + getTicketId() + "'" +
            ", resolvedAt='" + getResolvedAt() + "'" +
            ", link=" + getLink() +
            ", correlationRoot=" + getCorrelationRoot() +
            "}";
    }
}
