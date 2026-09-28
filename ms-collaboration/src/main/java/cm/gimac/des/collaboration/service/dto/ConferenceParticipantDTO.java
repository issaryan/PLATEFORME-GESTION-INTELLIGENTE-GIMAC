package cm.gimac.des.collaboration.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.ConferenceParticipant} entity.
 */
@Schema(
    description = "Entrée et sortie d'un participant. Écart CC corrigé : l'attribut\nparticipants (List<UUID>) du CC est normalisé pour la chronologie de l'archive."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConferenceParticipantDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Participant (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID userId;

    @NotNull
    @Size(max = 100)
    private String terminalId;

    @NotNull
    private Instant joinedAt;

    private Instant leftAt;

    @NotNull
    private ConferenceDTO conference;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Instant leftAt) {
        this.leftAt = leftAt;
    }

    public ConferenceDTO getConference() {
        return conference;
    }

    public void setConference(ConferenceDTO conference) {
        this.conference = conference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConferenceParticipantDTO)) {
            return false;
        }

        ConferenceParticipantDTO conferenceParticipantDTO = (ConferenceParticipantDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, conferenceParticipantDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConferenceParticipantDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", terminalId='" + getTerminalId() + "'" +
            ", joinedAt='" + getJoinedAt() + "'" +
            ", leftAt='" + getLeftAt() + "'" +
            ", conference=" + getConference() +
            "}";
    }
}
