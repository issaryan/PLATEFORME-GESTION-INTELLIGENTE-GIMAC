package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vacation instanciée depuis la grille publiée.
 */
@Entity
@Table(name = "shift")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Shift implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Référence lisible, par exemple VAC-2026-0927-N.
     */
    @NotNull
    @Size(max = 30)
    @Column(name = "reference", length = 30, nullable = false, unique = true)
    private String reference;

    /**
     * Date de la vacation (date du CC, renommé : mot réservé SQL).
     */
    @NotNull
    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false)
    private ShiftSlot slot;

    /**
     * Opérateurs de la vacation : UUID séparés par des virgules.
     */
    @NotNull
    @Size(max = 1000)
    @Column(name = "operator_ids", length = 1000, nullable = false)
    private String operatorIds;

    @NotNull
    @Column(name = "shift_lead_id", nullable = false)
    private UUID shiftLeadId;

    @NotNull
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @NotNull
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "previousRevision" }, allowSetters = true)
    private OnCallRoster roster;

    @JsonIgnoreProperties(value = { "shift", "supersedes", "shiftReport" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "shift")
    private ShiftReport shiftReport;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Shift id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return this.reference;
    }

    public Shift reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getShiftDate() {
        return this.shiftDate;
    }

    public Shift shiftDate(LocalDate shiftDate) {
        this.setShiftDate(shiftDate);
        return this;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public ShiftSlot getSlot() {
        return this.slot;
    }

    public Shift slot(ShiftSlot slot) {
        this.setSlot(slot);
        return this;
    }

    public void setSlot(ShiftSlot slot) {
        this.slot = slot;
    }

    public String getOperatorIds() {
        return this.operatorIds;
    }

    public Shift operatorIds(String operatorIds) {
        this.setOperatorIds(operatorIds);
        return this;
    }

    public void setOperatorIds(String operatorIds) {
        this.operatorIds = operatorIds;
    }

    public UUID getShiftLeadId() {
        return this.shiftLeadId;
    }

    public Shift shiftLeadId(UUID shiftLeadId) {
        this.setShiftLeadId(shiftLeadId);
        return this;
    }

    public void setShiftLeadId(UUID shiftLeadId) {
        this.shiftLeadId = shiftLeadId;
    }

    public Instant getStartsAt() {
        return this.startsAt;
    }

    public Shift startsAt(Instant startsAt) {
        this.setStartsAt(startsAt);
        return this;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return this.endsAt;
    }

    public Shift endsAt(Instant endsAt) {
        this.setEndsAt(endsAt);
        return this;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public OnCallRoster getRoster() {
        return this.roster;
    }

    public void setRoster(OnCallRoster onCallRoster) {
        this.roster = onCallRoster;
    }

    public Shift roster(OnCallRoster onCallRoster) {
        this.setRoster(onCallRoster);
        return this;
    }

    public ShiftReport getShiftReport() {
        return this.shiftReport;
    }

    public void setShiftReport(ShiftReport shiftReport) {
        if (this.shiftReport != null) {
            this.shiftReport.setShift(null);
        }
        if (shiftReport != null) {
            shiftReport.setShift(this);
        }
        this.shiftReport = shiftReport;
    }

    public Shift shiftReport(ShiftReport shiftReport) {
        this.setShiftReport(shiftReport);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Shift)) {
            return false;
        }
        return getId() != null && getId().equals(((Shift) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Shift{" +
            "id=" + getId() +
            ", reference='" + getReference() + "'" +
            ", shiftDate='" + getShiftDate() + "'" +
            ", slot='" + getSlot() + "'" +
            ", operatorIds='" + getOperatorIds() + "'" +
            ", shiftLeadId='" + getShiftLeadId() + "'" +
            ", startsAt='" + getStartsAt() + "'" +
            ", endsAt='" + getEndsAt() + "'" +
            "}";
    }
}
