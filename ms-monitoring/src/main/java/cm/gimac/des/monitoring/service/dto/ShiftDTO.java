package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.Shift} entity.
 */
@Schema(description = "Vacation instanciée depuis la grille publiée.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShiftDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Schema(description = "Référence lisible, par exemple VAC-2026-0927-N.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reference;

    @NotNull
    @Schema(description = "Date de la vacation (date du CC, renommé : mot réservé SQL).", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate shiftDate;

    @NotNull
    private ShiftSlot slot;

    @NotNull
    @Size(max = 1000)
    @Schema(description = "Opérateurs de la vacation : UUID séparés par des virgules.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operatorIds;

    @NotNull
    private UUID shiftLeadId;

    @NotNull
    private Instant startsAt;

    @NotNull
    private Instant endsAt;

    @NotNull
    private OnCallRosterDTO roster;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public ShiftSlot getSlot() {
        return slot;
    }

    public void setSlot(ShiftSlot slot) {
        this.slot = slot;
    }

    public String getOperatorIds() {
        return operatorIds;
    }

    public void setOperatorIds(String operatorIds) {
        this.operatorIds = operatorIds;
    }

    public UUID getShiftLeadId() {
        return shiftLeadId;
    }

    public void setShiftLeadId(UUID shiftLeadId) {
        this.shiftLeadId = shiftLeadId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public OnCallRosterDTO getRoster() {
        return roster;
    }

    public void setRoster(OnCallRosterDTO roster) {
        this.roster = roster;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShiftDTO)) {
            return false;
        }

        ShiftDTO shiftDTO = (ShiftDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, shiftDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShiftDTO{" +
            "id='" + getId() + "'" +
            ", reference='" + getReference() + "'" +
            ", shiftDate='" + getShiftDate() + "'" +
            ", slot='" + getSlot() + "'" +
            ", operatorIds='" + getOperatorIds() + "'" +
            ", shiftLeadId='" + getShiftLeadId() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            ", roster=" + getRoster() +
            "}";
    }
}
