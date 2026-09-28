package cm.gimac.des.incident.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.EngagementFreeze} entity.
 */
@Schema(
    description = "Gel d'engagement en attente de tiers (RG-37) : motivé et appuyé d'une pièce ;\nun seul gel ouvert par ticket à un instant donné."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EngagementFreezeDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 500)
    private String reason;

    @NotNull
    @Size(max = 255)
    private String thirdParty;

    @NotNull
    private Instant startedAt;

    private Instant endedAt;

    @NotNull
    @Size(max = 100)
    @Schema(description = "Pièce justificative déposée dans MS-DOCUMENT.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String proofRef;

    @NotNull
    private TicketDTO ticket;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getThirdParty() {
        return thirdParty;
    }

    public void setThirdParty(String thirdParty) {
        this.thirdParty = thirdParty;
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

    public String getProofRef() {
        return proofRef;
    }

    public void setProofRef(String proofRef) {
        this.proofRef = proofRef;
    }

    public TicketDTO getTicket() {
        return ticket;
    }

    public void setTicket(TicketDTO ticket) {
        this.ticket = ticket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EngagementFreezeDTO)) {
            return false;
        }

        EngagementFreezeDTO engagementFreezeDTO = (EngagementFreezeDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, engagementFreezeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EngagementFreezeDTO{" +
            "id='" + getId() + "'" +
            ", reason='" + getReason() + "'" +
            ", thirdParty='" + getThirdParty() + "'" +
            ", startedAt='" + getStartedAt() + "'" +
            ", endedAt='" + getEndedAt() + "'" +
            ", proofRef='" + getProofRef() + "'" +
            ", ticket=" + getTicket() +
            "}";
    }
}
