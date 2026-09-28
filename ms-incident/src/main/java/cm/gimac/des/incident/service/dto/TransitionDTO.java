package cm.gimac.des.incident.service.dto;

import cm.gimac.des.incident.domain.enumeration.TicketState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.Transition} entity.
 */
@Schema(
    description = "Transition de l'automate : ligne en ajout seul, chaînée par empreinte\n(RG-38, formule 6.40 : h0 = SHA-256(id du ticket), hn = SHA-256(hn-1 || JCS(τn)))."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TransitionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 1L)
    @Schema(description = "Rang dans la chronologie du ticket, à partir de 1.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long chainIndex;

    @Schema(description = "Vide pour la transition de création.")
    private TicketState fromState;

    @NotNull
    private TicketState toState;

    @NotNull
    private UUID actorId;

    @NotNull
    private Instant occurredAt;

    @Size(max = 2000)
    @Schema(description = "Justificatif exigé pour la mise en attente, la résolution sans suite et la réouverture.")
    private String justification;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String previousHash;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String hash;

    @NotNull
    private TicketDTO ticket;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getChainIndex() {
        return chainIndex;
    }

    public void setChainIndex(Long chainIndex) {
        this.chainIndex = chainIndex;
    }

    public TicketState getFromState() {
        return fromState;
    }

    public void setFromState(TicketState fromState) {
        this.fromState = fromState;
    }

    public TicketState getToState() {
        return toState;
    }

    public void setToState(TicketState toState) {
        this.toState = toState;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
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
        if (!(o instanceof TransitionDTO)) {
            return false;
        }

        TransitionDTO transitionDTO = (TransitionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, transitionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TransitionDTO{" +
            "id='" + getId() + "'" +
            ", chainIndex=" + getChainIndex() +
            ", fromState='" + getFromState() + "'" +
            ", toState='" + getToState() + "'" +
            ", actorId='" + getActorId() + "'" +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", justification='" + getJustification() + "'" +
            ", previousHash='" + getPreviousHash() + "'" +
            ", hash='" + getHash() + "'" +
            ", ticket=" + getTicket() +
            "}";
    }
}
