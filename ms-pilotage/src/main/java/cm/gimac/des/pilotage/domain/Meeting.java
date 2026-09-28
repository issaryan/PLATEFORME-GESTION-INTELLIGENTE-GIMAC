package cm.gimac.des.pilotage.domain;

import cm.gimac.des.pilotage.domain.enumeration.MeetingState;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Réunion d'une instance de gouvernance (comité de pilotage hebdomadaire,
 * revue mensuelle des risques, comité de mise en service, revue post-incident).
 */
@Entity
@Table(name = "meeting")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Meeting implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Instance, par exemple « comité de pilotage du DES ».
     */
    @NotNull
    @Size(max = 150)
    @Column(name = "instance", length = 150, nullable = false)
    private String instance;

    @NotNull
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "held_at")
    private Instant heldAt;

    /**
     * Ordre du jour : liste JSON des points, dont « suivi des décisions ».
     */
    @Lob
    @Column(name = "agenda")
    private String agenda;

    /**
     * Compte rendu (procès-verbal WORM 10 ans, RG-47) : identifiant dans MS-DOCUMENT.
     */
    @Column(name = "minutes_ref")
    private UUID minutesRef;

    /**
     * SHA256 du compte rendu déposé.
     */
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "minutes_digest", length = 64)
    private String minutesDigest;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private MeetingState state;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Meeting id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInstance() {
        return this.instance;
    }

    public Meeting instance(String instance) {
        this.setInstance(instance);
        return this;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public Instant getScheduledAt() {
        return this.scheduledAt;
    }

    public Meeting scheduledAt(Instant scheduledAt) {
        this.setScheduledAt(scheduledAt);
        return this;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getHeldAt() {
        return this.heldAt;
    }

    public Meeting heldAt(Instant heldAt) {
        this.setHeldAt(heldAt);
        return this;
    }

    public void setHeldAt(Instant heldAt) {
        this.heldAt = heldAt;
    }

    public String getAgenda() {
        return this.agenda;
    }

    public Meeting agenda(String agenda) {
        this.setAgenda(agenda);
        return this;
    }

    public void setAgenda(String agenda) {
        this.agenda = agenda;
    }

    public UUID getMinutesRef() {
        return this.minutesRef;
    }

    public Meeting minutesRef(UUID minutesRef) {
        this.setMinutesRef(minutesRef);
        return this;
    }

    public void setMinutesRef(UUID minutesRef) {
        this.minutesRef = minutesRef;
    }

    public String getMinutesDigest() {
        return this.minutesDigest;
    }

    public Meeting minutesDigest(String minutesDigest) {
        this.setMinutesDigest(minutesDigest);
        return this;
    }

    public void setMinutesDigest(String minutesDigest) {
        this.minutesDigest = minutesDigest;
    }

    public MeetingState getState() {
        return this.state;
    }

    public Meeting state(MeetingState state) {
        this.setState(state);
        return this;
    }

    public void setState(MeetingState state) {
        this.state = state;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Meeting)) {
            return false;
        }
        return getId() != null && getId().equals(((Meeting) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Meeting{" +
            "id=" + getId() +
            ", instance='" + getInstance() + "'" +
            ", scheduledAt='" + getScheduledAt() + "'" +
            ", heldAt='" + getHeldAt() + "'" +
            ", agenda='" + getAgenda() + "'" +
            ", minutesRef='" + getMinutesRef() + "'" +
            ", minutesDigest='" + getMinutesDigest() + "'" +
            ", state='" + getState() + "'" +
            "}";
    }
}
