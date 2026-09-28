package cm.gimac.des.rapprochement.service.dto;

import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.SuspensTransition} entity.
 */
@Schema(description = "Trace de chaque transition d'un suspens, en ajout seul (RG-44 ; écart CC complété).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SuspensTransitionDTO implements Serializable {

    private UUID id;

    private SuspensState fromState;

    @NotNull
    private SuspensState toState;

    @NotNull
    private UUID actorId;

    @NotNull
    private Instant occurredAt;

    @NotNull
    @Size(max = 2000)
    private String justification;

    @NotNull
    private SuspensDTO suspens;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SuspensState getFromState() {
        return fromState;
    }

    public void setFromState(SuspensState fromState) {
        this.fromState = fromState;
    }

    public SuspensState getToState() {
        return toState;
    }

    public void setToState(SuspensState toState) {
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

    public SuspensDTO getSuspens() {
        return suspens;
    }

    public void setSuspens(SuspensDTO suspens) {
        this.suspens = suspens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SuspensTransitionDTO)) {
            return false;
        }

        SuspensTransitionDTO suspensTransitionDTO = (SuspensTransitionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, suspensTransitionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SuspensTransitionDTO{" +
            "id='" + getId() + "'" +
            ", fromState='" + getFromState() + "'" +
            ", toState='" + getToState() + "'" +
            ", actorId='" + getActorId() + "'" +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", justification='" + getJustification() + "'" +
            ", suspens=" + getSuspens() +
            "}";
    }
}
