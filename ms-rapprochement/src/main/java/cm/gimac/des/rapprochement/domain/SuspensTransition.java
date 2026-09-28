package cm.gimac.des.rapprochement.domain;

import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Trace de chaque transition d'un suspens, en ajout seul (RG-44 ; écart CC complété).
 */
@Entity
@Table(name = "suspens_transition")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SuspensTransition implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state")
    private SuspensState fromState;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false)
    private SuspensState toState;

    @NotNull
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @NotNull
    @Size(max = 2000)
    @Column(name = "justification", length = 2000, nullable = false)
    private String justification;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "reconciliation", "regularizationAttestation" }, allowSetters = true)
    private Suspens suspens;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public SuspensTransition id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SuspensState getFromState() {
        return this.fromState;
    }

    public SuspensTransition fromState(SuspensState fromState) {
        this.setFromState(fromState);
        return this;
    }

    public void setFromState(SuspensState fromState) {
        this.fromState = fromState;
    }

    public SuspensState getToState() {
        return this.toState;
    }

    public SuspensTransition toState(SuspensState toState) {
        this.setToState(toState);
        return this;
    }

    public void setToState(SuspensState toState) {
        this.toState = toState;
    }

    public UUID getActorId() {
        return this.actorId;
    }

    public SuspensTransition actorId(UUID actorId) {
        this.setActorId(actorId);
        return this;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public Instant getOccurredAt() {
        return this.occurredAt;
    }

    public SuspensTransition occurredAt(Instant occurredAt) {
        this.setOccurredAt(occurredAt);
        return this;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getJustification() {
        return this.justification;
    }

    public SuspensTransition justification(String justification) {
        this.setJustification(justification);
        return this;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Suspens getSuspens() {
        return this.suspens;
    }

    public void setSuspens(Suspens suspens) {
        this.suspens = suspens;
    }

    public SuspensTransition suspens(Suspens suspens) {
        this.setSuspens(suspens);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SuspensTransition)) {
            return false;
        }
        return getId() != null && getId().equals(((SuspensTransition) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SuspensTransition{" +
            "id=" + getId() +
            ", fromState='" + getFromState() + "'" +
            ", toState='" + getToState() + "'" +
            ", actorId='" + getActorId() + "'" +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", justification='" + getJustification() + "'" +
            "}";
    }
}
