package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.AlertState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Alerte corrélée et dédupliquée (algorithme 6.44). La gravité n'entre pas
 * dans l'empreinte ; une alerte symptôme pointe vers sa racine.
 */
@Entity
@Table(name = "alert")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Alert implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Référence lisible, par exemple ALR-2026-04417.
     */
    @NotNull
    @Size(max = 30)
    @Column(name = "reference", length = 30, nullable = false, unique = true)
    private String reference;

    /**
     * Code de la source, ou « évaluation des seuils » pour une alerte interne.
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "source", length = 100, nullable = false)
    private String source;

    @NotNull
    @Size(max = 255)
    @Column(name = "rule", length = 255, nullable = false)
    private String rule;

    /**
     * SHA256(source || liaison || règle).
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "fingerprint", length = 64, nullable = false)
    private String fingerprint;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private AlertState state;

    @NotNull
    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @NotNull
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @NotNull
    @Min(value = 1)
    @Column(name = "occurrences", nullable = false)
    private Integer occurrences;

    /**
     * Échéance d'acquittement, pour une alerte racine.
     */
    @Column(name = "ack_deadline")
    private Instant ackDeadline;

    /**
     * Marquée hors délai par processOverdue.
     */
    @NotNull
    @Column(name = "overdue", nullable = false)
    private Boolean overdue;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "ticket_requested")
    private Boolean ticketRequested;

    @Size(max = 1000)
    @Column(name = "ack_comment", length = 1000)
    private String ackComment;

    /**
     * Ticket ouvert par MS-INCIDENT (incident.ticket-opened.v1).
     */
    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "parentPlatform" }, allowSetters = true)
    private SupervisedLink link;

    /**
     * Alerte racine dont celle-ci est un symptôme.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "link", "correlationRoot" }, allowSetters = true)
    private Alert correlationRoot;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Alert id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return this.reference;
    }

    public Alert reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getSource() {
        return this.source;
    }

    public Alert source(String source) {
        this.setSource(source);
        return this;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRule() {
        return this.rule;
    }

    public Alert rule(String rule) {
        this.setRule(rule);
        return this;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getFingerprint() {
        return this.fingerprint;
    }

    public Alert fingerprint(String fingerprint) {
        this.setFingerprint(fingerprint);
        return this;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public AlertSeverity getSeverity() {
        return this.severity;
    }

    public Alert severity(AlertSeverity severity) {
        this.setSeverity(severity);
        return this;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public AlertState getState() {
        return this.state;
    }

    public Alert state(AlertState state) {
        this.setState(state);
        return this;
    }

    public void setState(AlertState state) {
        this.state = state;
    }

    public Instant getFirstSeenAt() {
        return this.firstSeenAt;
    }

    public Alert firstSeenAt(Instant firstSeenAt) {
        this.setFirstSeenAt(firstSeenAt);
        return this;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return this.lastSeenAt;
    }

    public Alert lastSeenAt(Instant lastSeenAt) {
        this.setLastSeenAt(lastSeenAt);
        return this;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Integer getOccurrences() {
        return this.occurrences;
    }

    public Alert occurrences(Integer occurrences) {
        this.setOccurrences(occurrences);
        return this;
    }

    public void setOccurrences(Integer occurrences) {
        this.occurrences = occurrences;
    }

    public Instant getAckDeadline() {
        return this.ackDeadline;
    }

    public Alert ackDeadline(Instant ackDeadline) {
        this.setAckDeadline(ackDeadline);
        return this;
    }

    public void setAckDeadline(Instant ackDeadline) {
        this.ackDeadline = ackDeadline;
    }

    public Boolean getOverdue() {
        return this.overdue;
    }

    public Alert overdue(Boolean overdue) {
        this.setOverdue(overdue);
        return this;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }

    public UUID getAcknowledgedBy() {
        return this.acknowledgedBy;
    }

    public Alert acknowledgedBy(UUID acknowledgedBy) {
        this.setAcknowledgedBy(acknowledgedBy);
        return this;
    }

    public void setAcknowledgedBy(UUID acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Instant getAcknowledgedAt() {
        return this.acknowledgedAt;
    }

    public Alert acknowledgedAt(Instant acknowledgedAt) {
        this.setAcknowledgedAt(acknowledgedAt);
        return this;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Boolean getTicketRequested() {
        return this.ticketRequested;
    }

    public Alert ticketRequested(Boolean ticketRequested) {
        this.setTicketRequested(ticketRequested);
        return this;
    }

    public void setTicketRequested(Boolean ticketRequested) {
        this.ticketRequested = ticketRequested;
    }

    public String getAckComment() {
        return this.ackComment;
    }

    public Alert ackComment(String ackComment) {
        this.setAckComment(ackComment);
        return this;
    }

    public void setAckComment(String ackComment) {
        this.ackComment = ackComment;
    }

    public UUID getTicketId() {
        return this.ticketId;
    }

    public Alert ticketId(UUID ticketId) {
        this.setTicketId(ticketId);
        return this;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public Instant getResolvedAt() {
        return this.resolvedAt;
    }

    public Alert resolvedAt(Instant resolvedAt) {
        this.setResolvedAt(resolvedAt);
        return this;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public SupervisedLink getLink() {
        return this.link;
    }

    public void setLink(SupervisedLink supervisedLink) {
        this.link = supervisedLink;
    }

    public Alert link(SupervisedLink supervisedLink) {
        this.setLink(supervisedLink);
        return this;
    }

    public Alert getCorrelationRoot() {
        return this.correlationRoot;
    }

    public void setCorrelationRoot(Alert alert) {
        this.correlationRoot = alert;
    }

    public Alert correlationRoot(Alert alert) {
        this.setCorrelationRoot(alert);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Alert)) {
            return false;
        }
        return getId() != null && getId().equals(((Alert) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Alert{" +
            "id=" + getId() +
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
            "}";
    }
}
