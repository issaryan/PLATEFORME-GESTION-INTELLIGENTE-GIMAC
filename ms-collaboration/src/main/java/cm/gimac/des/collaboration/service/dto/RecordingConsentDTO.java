package cm.gimac.des.collaboration.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.RecordingConsent} entity.
 */
@Schema(description = "Consentement explicite d'un participant à l'enregistrement (RG-55).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecordingConsentDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Participant (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID userId;

    @NotNull
    private Boolean granted;

    @NotNull
    private Instant decidedAt;

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

    public Boolean getGranted() {
        return granted;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
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
        if (!(o instanceof RecordingConsentDTO)) {
            return false;
        }

        RecordingConsentDTO recordingConsentDTO = (RecordingConsentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, recordingConsentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecordingConsentDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", granted='" + getGranted() + "'" +
            ", decidedAt='" + getDecidedAt() + "'" +
            ", conference=" + getConference() +
            "}";
    }
}
