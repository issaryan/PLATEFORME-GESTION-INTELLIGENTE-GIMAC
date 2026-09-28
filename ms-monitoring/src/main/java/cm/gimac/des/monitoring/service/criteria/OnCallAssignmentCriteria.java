package cm.gimac.des.monitoring.service.criteria;

import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.monitoring.domain.OnCallAssignment} entity. This class is used
 * in {@link cm.gimac.des.monitoring.web.rest.OnCallAssignmentResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /on-call-assignments?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OnCallAssignmentCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ShiftSlot
     */
    public static class ShiftSlotFilter extends Filter<ShiftSlot> {

        public ShiftSlotFilter() {}

        public ShiftSlotFilter(ShiftSlotFilter filter) {
            super(filter);
        }

        @Override
        public ShiftSlotFilter copy() {
            return new ShiftSlotFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private LocalDateFilter dutyDate;

    private ShiftSlotFilter slot;

    private IntegerFilter level;

    private UUIDFilter operatorId;

    private BooleanFilter shiftLead;

    private UUIDFilter rosterId;

    private Boolean distinct;

    public OnCallAssignmentCriteria() {}

    public OnCallAssignmentCriteria(OnCallAssignmentCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.dutyDate = other.optionalDutyDate().map(LocalDateFilter::copy).orElse(null);
        this.slot = other.optionalSlot().map(ShiftSlotFilter::copy).orElse(null);
        this.level = other.optionalLevel().map(IntegerFilter::copy).orElse(null);
        this.operatorId = other.optionalOperatorId().map(UUIDFilter::copy).orElse(null);
        this.shiftLead = other.optionalShiftLead().map(BooleanFilter::copy).orElse(null);
        this.rosterId = other.optionalRosterId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public OnCallAssignmentCriteria copy() {
        return new OnCallAssignmentCriteria(this);
    }

    public UUIDFilter getId() {
        return id;
    }

    public Optional<UUIDFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public UUIDFilter id() {
        if (id == null) {
            setId(new UUIDFilter());
        }
        return id;
    }

    public void setId(UUIDFilter id) {
        this.id = id;
    }

    public LocalDateFilter getDutyDate() {
        return dutyDate;
    }

    public Optional<LocalDateFilter> optionalDutyDate() {
        return Optional.ofNullable(dutyDate);
    }

    public LocalDateFilter dutyDate() {
        if (dutyDate == null) {
            setDutyDate(new LocalDateFilter());
        }
        return dutyDate;
    }

    public void setDutyDate(LocalDateFilter dutyDate) {
        this.dutyDate = dutyDate;
    }

    public ShiftSlotFilter getSlot() {
        return slot;
    }

    public Optional<ShiftSlotFilter> optionalSlot() {
        return Optional.ofNullable(slot);
    }

    public ShiftSlotFilter slot() {
        if (slot == null) {
            setSlot(new ShiftSlotFilter());
        }
        return slot;
    }

    public void setSlot(ShiftSlotFilter slot) {
        this.slot = slot;
    }

    public IntegerFilter getLevel() {
        return level;
    }

    public Optional<IntegerFilter> optionalLevel() {
        return Optional.ofNullable(level);
    }

    public IntegerFilter level() {
        if (level == null) {
            setLevel(new IntegerFilter());
        }
        return level;
    }

    public void setLevel(IntegerFilter level) {
        this.level = level;
    }

    public UUIDFilter getOperatorId() {
        return operatorId;
    }

    public Optional<UUIDFilter> optionalOperatorId() {
        return Optional.ofNullable(operatorId);
    }

    public UUIDFilter operatorId() {
        if (operatorId == null) {
            setOperatorId(new UUIDFilter());
        }
        return operatorId;
    }

    public void setOperatorId(UUIDFilter operatorId) {
        this.operatorId = operatorId;
    }

    public BooleanFilter getShiftLead() {
        return shiftLead;
    }

    public Optional<BooleanFilter> optionalShiftLead() {
        return Optional.ofNullable(shiftLead);
    }

    public BooleanFilter shiftLead() {
        if (shiftLead == null) {
            setShiftLead(new BooleanFilter());
        }
        return shiftLead;
    }

    public void setShiftLead(BooleanFilter shiftLead) {
        this.shiftLead = shiftLead;
    }

    public UUIDFilter getRosterId() {
        return rosterId;
    }

    public Optional<UUIDFilter> optionalRosterId() {
        return Optional.ofNullable(rosterId);
    }

    public UUIDFilter rosterId() {
        if (rosterId == null) {
            setRosterId(new UUIDFilter());
        }
        return rosterId;
    }

    public void setRosterId(UUIDFilter rosterId) {
        this.rosterId = rosterId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final OnCallAssignmentCriteria that = (OnCallAssignmentCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(dutyDate, that.dutyDate) &&
            Objects.equals(slot, that.slot) &&
            Objects.equals(level, that.level) &&
            Objects.equals(operatorId, that.operatorId) &&
            Objects.equals(shiftLead, that.shiftLead) &&
            Objects.equals(rosterId, that.rosterId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dutyDate, slot, level, operatorId, shiftLead, rosterId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OnCallAssignmentCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalDutyDate().map(f -> "dutyDate=" + f + ", ").orElse("") +
            optionalSlot().map(f -> "slot=" + f + ", ").orElse("") +
            optionalLevel().map(f -> "level=" + f + ", ").orElse("") +
            optionalOperatorId().map(f -> "operatorId=" + f + ", ").orElse("") +
            optionalShiftLead().map(f -> "shiftLead=" + f + ", ").orElse("") +
            optionalRosterId().map(f -> "rosterId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
