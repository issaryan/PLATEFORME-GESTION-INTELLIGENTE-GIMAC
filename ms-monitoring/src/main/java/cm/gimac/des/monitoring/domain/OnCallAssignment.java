package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Affectation d'un opérateur à une plage et un niveau (1 en salle, 2 en astreinte).
 */
@Entity
@Table(name = "on_call_assignment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OnCallAssignment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Jour de la plage (day du CC, renommé : mot réservé SQL).
     */
    @NotNull
    @Column(name = "duty_date", nullable = false)
    private LocalDate dutyDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false)
    private ShiftSlot slot;

    @NotNull
    @Min(value = 1)
    @Max(value = 2)
    @Column(name = "level", nullable = false)
    private Integer level;

    @NotNull
    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @NotNull
    @Column(name = "shift_lead", nullable = false)
    private Boolean shiftLead;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "previousRevision" }, allowSetters = true)
    private OnCallRoster roster;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public OnCallAssignment id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getDutyDate() {
        return this.dutyDate;
    }

    public OnCallAssignment dutyDate(LocalDate dutyDate) {
        this.setDutyDate(dutyDate);
        return this;
    }

    public void setDutyDate(LocalDate dutyDate) {
        this.dutyDate = dutyDate;
    }

    public ShiftSlot getSlot() {
        return this.slot;
    }

    public OnCallAssignment slot(ShiftSlot slot) {
        this.setSlot(slot);
        return this;
    }

    public void setSlot(ShiftSlot slot) {
        this.slot = slot;
    }

    public Integer getLevel() {
        return this.level;
    }

    public OnCallAssignment level(Integer level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public UUID getOperatorId() {
        return this.operatorId;
    }

    public OnCallAssignment operatorId(UUID operatorId) {
        this.setOperatorId(operatorId);
        return this;
    }

    public void setOperatorId(UUID operatorId) {
        this.operatorId = operatorId;
    }

    public Boolean getShiftLead() {
        return this.shiftLead;
    }

    public OnCallAssignment shiftLead(Boolean shiftLead) {
        this.setShiftLead(shiftLead);
        return this;
    }

    public void setShiftLead(Boolean shiftLead) {
        this.shiftLead = shiftLead;
    }

    public OnCallRoster getRoster() {
        return this.roster;
    }

    public void setRoster(OnCallRoster onCallRoster) {
        this.roster = onCallRoster;
    }

    public OnCallAssignment roster(OnCallRoster onCallRoster) {
        this.setRoster(onCallRoster);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OnCallAssignment)) {
            return false;
        }
        return getId() != null && getId().equals(((OnCallAssignment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OnCallAssignment{" +
            "id=" + getId() +
            ", dutyDate='" + getDutyDate() + "'" +
            ", slot='" + getSlot() + "'" +
            ", level=" + getLevel() +
            ", operatorId='" + getOperatorId() + "'" +
            ", shiftLead='" + getShiftLead() + "'" +
            "}";
    }
}
