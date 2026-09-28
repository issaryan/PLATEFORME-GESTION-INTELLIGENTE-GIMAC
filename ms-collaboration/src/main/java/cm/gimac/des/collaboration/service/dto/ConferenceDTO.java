package cm.gimac.des.collaboration.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.Conference} entity.
 */
@Schema(description = "Conférence audio et vidéo sur le SFU local, SFrame obligatoire (RG-54).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConferenceDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 100)
    @Schema(description = "Nom de la salle LiveKit, par exemple SFU-C-0142.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sfuRoom;

    @NotNull
    private Instant startedAt;

    private Instant endedAt;

    @NotNull
    @Schema(description = "Sur-chiffrement SFrame exigé (toujours vrai).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean sframeRequired;

    @NotNull
    private Boolean screenShare;

    @NotNull
    @Schema(
        description = "Enregistrement actif, seulement avec le consentement de chaque présent (RG-55).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean recording;

    @NotNull
    @Min(value = 0)
    @Schema(description = "Époque courante des clés médias.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer keyEpoch;

    @NotNull
    private RoomDTO room;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSfuRoom() {
        return sfuRoom;
    }

    public void setSfuRoom(String sfuRoom) {
        this.sfuRoom = sfuRoom;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Boolean getSframeRequired() {
        return sframeRequired;
    }

    public void setSframeRequired(Boolean sframeRequired) {
        this.sframeRequired = sframeRequired;
    }

    public Boolean getScreenShare() {
        return screenShare;
    }

    public void setScreenShare(Boolean screenShare) {
        this.screenShare = screenShare;
    }

    public Boolean getRecording() {
        return recording;
    }

    public void setRecording(Boolean recording) {
        this.recording = recording;
    }

    public Integer getKeyEpoch() {
        return keyEpoch;
    }

    public void setKeyEpoch(Integer keyEpoch) {
        this.keyEpoch = keyEpoch;
    }

    public RoomDTO getRoom() {
        return room;
    }

    public void setRoom(RoomDTO room) {
        this.room = room;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConferenceDTO)) {
            return false;
        }

        ConferenceDTO conferenceDTO = (ConferenceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, conferenceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConferenceDTO{" +
            "id='" + getId() + "'" +
            ", sfuRoom='" + getSfuRoom() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", endedAt='" + getEndedAt() + "'" +
            ", sframeRequired='" + getSframeRequired() + "'" +
            ", screenShare='" + getScreenShare() + "'" +
            ", recording='" + getRecording() + "'" +
            ", keyEpoch=" + getKeyEpoch() +
            ", room=" + getRoom() +
            "}";
    }
}
