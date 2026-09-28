package cm.gimac.des.collaboration.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Entrée et sortie d'un participant. Écart CC corrigé : l'attribut
 * participants (List<UUID>) du CC est normalisé pour la chronologie de l'archive.
 */
@Entity
@Table(name = "conference_participant")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConferenceParticipant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Participant (MS-IAM).
     */
    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Size(max = 100)
    @Column(name = "terminal_id", length = 100, nullable = false)
    private String terminalId;

    @NotNull
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "room" }, allowSetters = true)
    private Conference conference;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ConferenceParticipant id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public ConferenceParticipant userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTerminalId() {
        return this.terminalId;
    }

    public ConferenceParticipant terminalId(String terminalId) {
        this.setTerminalId(terminalId);
        return this;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public Instant getJoinedAt() {
        return this.joinedAt;
    }

    public ConferenceParticipant joinedAt(Instant joinedAt) {
        this.setJoinedAt(joinedAt);
        return this;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLeftAt() {
        return this.leftAt;
    }

    public ConferenceParticipant leftAt(Instant leftAt) {
        this.setLeftAt(leftAt);
        return this;
    }

    public void setLeftAt(Instant leftAt) {
        this.leftAt = leftAt;
    }

    public Conference getConference() {
        return this.conference;
    }

    public void setConference(Conference conference) {
        this.conference = conference;
    }

    public ConferenceParticipant conference(Conference conference) {
        this.setConference(conference);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConferenceParticipant)) {
            return false;
        }
        return getId() != null && getId().equals(((ConferenceParticipant) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConferenceParticipant{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", terminalId='" + getTerminalId() + "'" +
            ", joinedAt='" + getJoinedAt() + "'" +
            ", leftAt='" + getLeftAt() + "'" +
            "}";
    }
}
