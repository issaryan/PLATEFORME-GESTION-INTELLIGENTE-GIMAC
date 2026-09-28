package cm.gimac.des.pilotage.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Convocation et présence d'un participant. Écart CC corrigé : les attributs
 * participants (List<UUID>) et attendance (Map<UUID, Boolean>) du CC sont
 * normalisés en une entité.
 */
@Entity
@Table(name = "meeting_attendance")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeetingAttendance implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Participant convoqué (MS-IAM).
     */
    @NotNull
    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @Column(name = "present")
    private Boolean present;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @ManyToOne(optional = false)
    @NotNull
    private Meeting meeting;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public MeetingAttendance id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParticipantId() {
        return this.participantId;
    }

    public MeetingAttendance participantId(UUID participantId) {
        this.setParticipantId(participantId);
        return this;
    }

    public void setParticipantId(UUID participantId) {
        this.participantId = participantId;
    }

    public Boolean getPresent() {
        return this.present;
    }

    public MeetingAttendance present(Boolean present) {
        this.setPresent(present);
        return this;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

    public Instant getRecordedAt() {
        return this.recordedAt;
    }

    public MeetingAttendance recordedAt(Instant recordedAt) {
        this.setRecordedAt(recordedAt);
        return this;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Meeting getMeeting() {
        return this.meeting;
    }

    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }

    public MeetingAttendance meeting(Meeting meeting) {
        this.setMeeting(meeting);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MeetingAttendance)) {
            return false;
        }
        return getId() != null && getId().equals(((MeetingAttendance) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeetingAttendance{" +
            "id=" + getId() +
            ", participantId='" + getParticipantId() + "'" +
            ", present='" + getPresent() + "'" +
            ", recordedAt='" + getRecordedAt() + "'" +
            "}";
    }
}
