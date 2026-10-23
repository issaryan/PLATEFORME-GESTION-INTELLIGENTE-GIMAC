package cm.gimac.des.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Événement d'audit : feuille de l'arbre de Merkle (CC §6.1.3.3).
 * L'index est strictement croissant et sans trou ; il est attribué dans la
 * transaction d'inscription, et non par une séquence de la base.
 */
@Entity
@Table(name = "audit_event")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AuditEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Index de la feuille dans l'arbre, à partir de 0.
     */
    @NotNull
    @Min(value = 0L)
    @Column(name = "log_index", nullable = false, unique = true)
    private Long logIndex;

    /**
     * Identifiant CloudEvents de l'enveloppe reçue.
     */
    @NotNull
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    /**
     * Horodatage d'occurrence, fourni par le producteur.
     */
    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /**
     * Horodatage d'inscription dans le registre.
     */
    @NotNull
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @NotNull
    @Size(max = 100)
    @Column(name = "actor_id", length = 100, nullable = false)
    private String actorId;

    @NotNull
    @Size(max = 100)
    @Column(name = "action_type", length = 100, nullable = false)
    private String actionType;

    /**
     * Domaine producteur, par exemple homologation.
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "domain", length = 50, nullable = false)
    private String domain;

    /**
     * Sujet du bus, par exemple homologation.operation-closed.v1.
     */
    @NotNull
    @Size(max = 150)
    @Column(name = "subject", length = 150, nullable = false)
    private String subject;

    @Column(name = "correlation_id")
    private UUID correlationId;

    /**
     * Enveloppe CloudEvents canonique (clés triées, RFC 8259), sans numéro de carte en clair.
     */
    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    /**
     * SHA256(0x00 || forme canonique).
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "leaf_hash", length = 64, nullable = false)
    private String leafHash;

    /**
     * Institution concernée par un événement de la sandbox (extension CloudEvents {@code institutionid} ou
     * {@code codeParticipant} des données, US-EXI-10) ; nulle pour les autres entrées.
     */
    @Size(max = 64)
    @Column(name = "institution_id", length = 64)
    private String institutionId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AuditEvent id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getLogIndex() {
        return this.logIndex;
    }

    public AuditEvent logIndex(Long logIndex) {
        this.setLogIndex(logIndex);
        return this;
    }

    public void setLogIndex(Long logIndex) {
        this.logIndex = logIndex;
    }

    public UUID getEventId() {
        return this.eventId;
    }

    public AuditEvent eventId(UUID eventId) {
        this.setEventId(eventId);
        return this;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Instant getOccurredAt() {
        return this.occurredAt;
    }

    public AuditEvent occurredAt(Instant occurredAt) {
        this.setOccurredAt(occurredAt);
        return this;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getRecordedAt() {
        return this.recordedAt;
    }

    public AuditEvent recordedAt(Instant recordedAt) {
        this.setRecordedAt(recordedAt);
        return this;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getActorId() {
        return this.actorId;
    }

    public AuditEvent actorId(String actorId) {
        this.setActorId(actorId);
        return this;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActionType() {
        return this.actionType;
    }

    public AuditEvent actionType(String actionType) {
        this.setActionType(actionType);
        return this;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDomain() {
        return this.domain;
    }

    public AuditEvent domain(String domain) {
        this.setDomain(domain);
        return this;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSubject() {
        return this.subject;
    }

    public AuditEvent subject(String subject) {
        this.setSubject(subject);
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public UUID getCorrelationId() {
        return this.correlationId;
    }

    public AuditEvent correlationId(UUID correlationId) {
        this.setCorrelationId(correlationId);
        return this;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getPayload() {
        return this.payload;
    }

    public AuditEvent payload(String payload) {
        this.setPayload(payload);
        return this;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getLeafHash() {
        return this.leafHash;
    }

    public AuditEvent leafHash(String leafHash) {
        this.setLeafHash(leafHash);
        return this;
    }

    public void setLeafHash(String leafHash) {
        this.leafHash = leafHash;
    }

    public String getInstitutionId() {
        return this.institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditEvent)) {
            return false;
        }
        return getId() != null && getId().equals(((AuditEvent) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AuditEvent{" +
            "id=" + getId() +
            ", logIndex=" + getLogIndex() +
            ", eventId='" + getEventId() + "'" +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", recordedAt='" + getRecordedAt() + "'" +
            ", actorId='" + getActorId() + "'" +
            ", actionType='" + getActionType() + "'" +
            ", domain='" + getDomain() + "'" +
            ", subject='" + getSubject() + "'" +
            ", correlationId='" + getCorrelationId() + "'" +
            ", payload='" + getPayload() + "'" +
            ", leafHash='" + getLeafHash() + "'" +
            "}";
    }
}
