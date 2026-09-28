package cm.gimac.des.collaboration.service.dto;

import cm.gimac.des.collaboration.domain.enumeration.MediaKeyRotationReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.MediaKeyEpoch} entity.
 */
@Schema(description = "Changement d'époque des clés médias, consigné sans aucune clé (algorithme 6.61).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MediaKeyEpochDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 0)
    private Integer epoch;

    @NotNull
    private MediaKeyRotationReason reason;

    @NotNull
    private Instant rotatedAt;

    @NotNull
    private ConferenceDTO conference;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getEpoch() {
        return epoch;
    }

    public void setEpoch(Integer epoch) {
        this.epoch = epoch;
    }

    public MediaKeyRotationReason getReason() {
        return reason;
    }

    public void setReason(MediaKeyRotationReason reason) {
        this.reason = reason;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }

    public void setRotatedAt(Instant rotatedAt) {
        this.rotatedAt = rotatedAt;
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
        if (!(o instanceof MediaKeyEpochDTO)) {
            return false;
        }

        MediaKeyEpochDTO mediaKeyEpochDTO = (MediaKeyEpochDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, mediaKeyEpochDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MediaKeyEpochDTO{" +
            "id='" + getId() + "'" +
            ", epoch=" + getEpoch() +
            ", reason='" + getReason() + "'" +
            ", rotatedAt='" + getRotatedAt() + "'" +
            ", conference=" + getConference() +
            "}";
    }
}
