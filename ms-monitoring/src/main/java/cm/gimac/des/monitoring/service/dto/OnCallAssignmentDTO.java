package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.OnCallAssignment} entity.
 */
@Schema(description = "Affectation d'un opérateur à une plage et un niveau (1 en salle, 2 en astreinte).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OnCallAssignmentDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Jour de la plage (day du CC, renommé : mot réservé SQL).", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dutyDate;

    @NotNull
    private ShiftSlot slot;

    @NotNull
    @Min(value = 1)
    @Max(value = 2)
    private Integer level;

    @NotNull
    private UUID operatorId;

    @NotNull
    private Boolean shiftLead;

    @NotNull
    private OnCallRosterDTO roster;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getDutyDate() {
        return dutyDate;
    }

    public void setDutyDate(LocalDate dutyDate) {
        this.dutyDate = dutyDate;
    }

    public ShiftSlot getSlot() {
        return slot;
    }

    public void setSlot(ShiftSlot slot) {
        this.slot = slot;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(UUID operatorId) {
        this.operatorId = operatorId;
    }

    public Boolean getShiftLead() {
        return shiftLead;
    }

    public void setShiftLead(Boolean shiftLead) {
        this.shiftLead = shiftLead;
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
        if (!(o instanceof OnCallAssignmentDTO)) {
            return false;
        }

        OnCallAssignmentDTO onCallAssignmentDTO = (OnCallAssignmentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, onCallAssignmentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OnCallAssignmentDTO{" +
            "id='" + getId() + "'" +
            ", dutyDate='" + getDutyDate() + "'" +
            ", slot='" + getSlot() + "'" +
            ", level=" + getLevel() +
            ", operatorId='" + getOperatorId() + "'" +
            ", shiftLead='" + getShiftLead() + "'" +
            ", roster=" + getRoster() +
            "}";
    }
}
