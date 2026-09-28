package cm.gimac.des.collaboration.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.RecordingSegment} entity.
 */
@Schema(
    description = "Segment enregistré, avec la liste des consentements qui l'autorisent.\nÉcart CC corrigé : le segment est cité (§6.1.14.1, algorithme 6.61) sans\nêtre modélisé."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecordingSegmentDTO implements Serializable {

    private UUID id;

    @NotNull
    private Instant startedAt;

    private Instant endedAt;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Identifiant de l'enregistrement (egress LiveKit).", requiredMode = Schema.RequiredMode.REQUIRED)
    private String egressReference;

    @NotNull
    @Size(max = 4000)
    @Schema(
        description = "Participants consentants : identifiants (MS-IAM), séparés par des virgules.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String consentingUserIds;

    @NotNull
    private ConferenceDTO conference;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getEgressReference() {
        return egressReference;
    }

    public void setEgressReference(String egressReference) {
        this.egressReference = egressReference;
    }

    public String getConsentingUserIds() {
        return consentingUserIds;
    }

    public void setConsentingUserIds(String consentingUserIds) {
        this.consentingUserIds = consentingUserIds;
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
        if (!(o instanceof RecordingSegmentDTO)) {
            return false;
        }

        RecordingSegmentDTO recordingSegmentDTO = (RecordingSegmentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, recordingSegmentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecordingSegmentDTO{" +
            "id='" + getId() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", endedAt='" + getEndedAt() + "'" +
            ", egressReference='" + getEgressReference() + "'" +
            ", consentingUserIds='" + getConsentingUserIds() + "'" +
            ", conference=" + getConference() +
            "}";
    }
}
