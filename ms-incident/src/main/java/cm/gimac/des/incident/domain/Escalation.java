package cm.gimac.des.incident.domain;

import cm.gimac.des.incident.domain.enumeration.EscalationScope;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Escalade déclenchée (algorithme 6.41) : un palier n'est franchi qu'une fois,
 * unicité (ticket, portée, niveau) posée en base.
 */
@Entity
@Table(name = "escalation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Escalation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private EscalationScope scope;

    /**
     * Niveau N1 à N4.
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 4)
    @Column(name = "level", nullable = false)
    private Integer level;

    /**
     * Destinataire interne (MS-IAM), après substitution du délégué actif.
     */
    @Column(name = "target_user_id")
    private UUID targetUserId;

    /**
     * Contact d'escalade de l'institution (MS-PARTICIPANT).
     */
    @Column(name = "target_contact_id")
    private UUID targetContactId;

    @NotNull
    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @NotNull
    @Size(max = 500)
    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    /**
     * Délai imputable au moment du franchissement.
     */
    @Column(name = "imputable_delay")
    private Duration imputableDelay;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "relatedTicket", "incident" }, allowSetters = true)
    private Ticket ticket;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Escalation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EscalationScope getScope() {
        return this.scope;
    }

    public Escalation scope(EscalationScope scope) {
        this.setScope(scope);
        return this;
    }

    public void setScope(EscalationScope scope) {
        this.scope = scope;
    }

    public Integer getLevel() {
        return this.level;
    }

    public Escalation level(Integer level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public UUID getTargetUserId() {
        return this.targetUserId;
    }

    public Escalation targetUserId(UUID targetUserId) {
        this.setTargetUserId(targetUserId);
        return this;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public UUID getTargetContactId() {
        return this.targetContactId;
    }

    public Escalation targetContactId(UUID targetContactId) {
        this.setTargetContactId(targetContactId);
        return this;
    }

    public void setTargetContactId(UUID targetContactId) {
        this.targetContactId = targetContactId;
    }

    public Instant getTriggeredAt() {
        return this.triggeredAt;
    }

    public Escalation triggeredAt(Instant triggeredAt) {
        this.setTriggeredAt(triggeredAt);
        return this;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public String getReason() {
        return this.reason;
    }

    public Escalation reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Duration getImputableDelay() {
        return this.imputableDelay;
    }

    public Escalation imputableDelay(Duration imputableDelay) {
        this.setImputableDelay(imputableDelay);
        return this;
    }

    public void setImputableDelay(Duration imputableDelay) {
        this.imputableDelay = imputableDelay;
    }

    public Ticket getTicket() {
        return this.ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Escalation ticket(Ticket ticket) {
        this.setTicket(ticket);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Escalation)) {
            return false;
        }
        return getId() != null && getId().equals(((Escalation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Escalation{" +
            "id=" + getId() +
            ", scope='" + getScope() + "'" +
            ", level=" + getLevel() +
            ", targetUserId='" + getTargetUserId() + "'" +
            ", targetContactId='" + getTargetContactId() + "'" +
            ", triggeredAt='" + getTriggeredAt() + "'" +
            ", reason='" + getReason() + "'" +
            ", imputableDelay='" + getImputableDelay() + "'" +
            "}";
    }
}
