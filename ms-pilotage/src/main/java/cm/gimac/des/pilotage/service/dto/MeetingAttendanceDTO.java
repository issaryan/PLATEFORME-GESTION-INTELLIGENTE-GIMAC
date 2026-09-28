package cm.gimac.des.pilotage.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.MeetingAttendance} entity.
 */
@Schema(
    description = "Convocation et présence d'un participant. Écart CC corrigé : les attributs\nparticipants (List<UUID>) et attendance (Map<UUID, Boolean>) du CC sont\nnormalisés en une entité."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeetingAttendanceDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Participant convoqué (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID participantId;

    private Boolean present;

    private Instant recordedAt;

    @NotNull
    private MeetingDTO meeting;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public void setParticipantId(UUID participantId) {
        this.participantId = participantId;
    }

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public MeetingDTO getMeeting() {
        return meeting;
    }

    public void setMeeting(MeetingDTO meeting) {
        this.meeting = meeting;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MeetingAttendanceDTO)) {
            return false;
        }

        MeetingAttendanceDTO meetingAttendanceDTO = (MeetingAttendanceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, meetingAttendanceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeetingAttendanceDTO{" +
            "id='" + getId() + "'" +
            ", participantId='" + getParticipantId() + "'" +
            ", present='" + getPresent() + "'" +
            ", recordedAt='" + getRecordedAt() + "'" +
            ", meeting=" + getMeeting() +
            "}";
    }
}
