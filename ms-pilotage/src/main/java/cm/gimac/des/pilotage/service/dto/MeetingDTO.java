package cm.gimac.des.pilotage.service.dto;

import cm.gimac.des.pilotage.domain.enumeration.MeetingState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.Meeting} entity.
 */
@Schema(
    description = "Réunion d'une instance de gouvernance (comité de pilotage hebdomadaire,\nrevue mensuelle des risques, comité de mise en service, revue post-incident)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeetingDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 150)
    @Schema(description = "Instance, par exemple « comité de pilotage du DES ».", requiredMode = Schema.RequiredMode.REQUIRED)
    private String instance;

    @NotNull
    private Instant scheduledAt;

    private Instant heldAt;

    @Schema(description = "Ordre du jour : liste JSON des points, dont « suivi des décisions ».")
    @Lob
    private String agenda;

    @Schema(description = "Compte rendu (procès-verbal WORM 10 ans, RG-47) : identifiant dans MS-DOCUMENT.")
    private UUID minutesRef;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "SHA256 du compte rendu déposé.")
    private String minutesDigest;

    @NotNull
    private MeetingState state;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getHeldAt() {
        return heldAt;
    }

    public void setHeldAt(Instant heldAt) {
        this.heldAt = heldAt;
    }

    public String getAgenda() {
        return agenda;
    }

    public void setAgenda(String agenda) {
        this.agenda = agenda;
    }

    public UUID getMinutesRef() {
        return minutesRef;
    }

    public void setMinutesRef(UUID minutesRef) {
        this.minutesRef = minutesRef;
    }

    public String getMinutesDigest() {
        return minutesDigest;
    }

    public void setMinutesDigest(String minutesDigest) {
        this.minutesDigest = minutesDigest;
    }

    public MeetingState getState() {
        return state;
    }

    public void setState(MeetingState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MeetingDTO)) {
            return false;
        }

        MeetingDTO meetingDTO = (MeetingDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, meetingDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeetingDTO{" +
            "id='" + getId() + "'" +
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
