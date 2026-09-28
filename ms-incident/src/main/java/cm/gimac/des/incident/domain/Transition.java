package cm.gimac.des.incident.domain;

import cm.gimac.des.incident.domain.enumeration.TicketState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Transition de l'automate : ligne en ajout seul, chaînée par empreinte
 * (RG-38, formule 6.40 : h0 = SHA-256(id du ticket), hn = SHA-256(hn-1 || JCS(τn))).
 */
@Entity
@Table(name = "transition")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Transition implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Rang dans la chronologie du ticket, à partir de 1.
     */
    @NotNull
    @Min(value = 1L)
    @Column(name = "chain_index", nullable = false)
    private Long chainIndex;

    /**
     * Vide pour la transition de création.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_state")
    private TicketState fromState;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false)
    private TicketState toState;

    @NotNull
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /**
     * Justificatif exigé pour la mise en attente, la résolution sans suite et la réouverture.
     */
    @Size(max = 2000)
    @Column(name = "justification", length = 2000)
    private String justification;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "previous_hash", length = 64, nullable = false)
    private String previousHash;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "hash", length = 64, nullable = false, unique = true)
    private String hash;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "relatedTicket", "incident" }, allowSetters = true)
    private Ticket ticket;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Transition id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getChainIndex() {
        return this.chainIndex;
    }

    public Transition chainIndex(Long chainIndex) {
        this.setChainIndex(chainIndex);
        return this;
    }

    public void setChainIndex(Long chainIndex) {
        this.chainIndex = chainIndex;
    }

    public TicketState getFromState() {
        return this.fromState;
    }

    public Transition fromState(TicketState fromState) {
        this.setFromState(fromState);
        return this;
    }

    public void setFromState(TicketState fromState) {
        this.fromState = fromState;
    }

    public TicketState getToState() {
        return this.toState;
    }

    public Transition toState(TicketState toState) {
        this.setToState(toState);
        return this;
    }

    public void setToState(TicketState toState) {
        this.toState = toState;
    }

    public UUID getActorId() {
        return this.actorId;
    }

    public Transition actorId(UUID actorId) {
        this.setActorId(actorId);
        return this;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public Instant getOccurredAt() {
        return this.occurredAt;
    }

    public Transition occurredAt(Instant occurredAt) {
        this.setOccurredAt(occurredAt);
        return this;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getJustification() {
        return this.justification;
    }

    public Transition justification(String justification) {
        this.setJustification(justification);
        return this;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getPreviousHash() {
        return this.previousHash;
    }

    public Transition previousHash(String previousHash) {
        this.setPreviousHash(previousHash);
        return this;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getHash() {
        return this.hash;
    }

    public Transition hash(String hash) {
        this.setHash(hash);
        return this;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Ticket getTicket() {
        return this.ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Transition ticket(Ticket ticket) {
        this.setTicket(ticket);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transition)) {
            return false;
        }
        return getId() != null && getId().equals(((Transition) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Transition{" +
            "id=" + getId() +
            ", chainIndex=" + getChainIndex() +
            ", fromState='" + getFromState() + "'" +
            ", toState='" + getToState() + "'" +
            ", actorId='" + getActorId() + "'" +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", justification='" + getJustification() + "'" +
            ", previousHash='" + getPreviousHash() + "'" +
            ", hash='" + getHash() + "'" +
            "}";
    }
}
