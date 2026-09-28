package cm.gimac.des.monitoring.service.criteria;

import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.monitoring.domain.Shift} entity. This class is used
 * in {@link cm.gimac.des.monitoring.web.rest.ShiftResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /shifts?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ShiftCriteria implements Serializable, Criteria {

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

    private StringFilter reference;

    private LocalDateFilter shiftDate;

    private ShiftSlotFilter slot;

    private StringFilter operatorIds;

    private UUIDFilter shiftLeadId;

    private InstantFilter startsAt;

    private InstantFilter endsAt;

    private UUIDFilter rosterId;

    private UUIDFilter shiftReportId;

    private Boolean distinct;

    public ShiftCriteria() {}

    public ShiftCriteria(ShiftCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.shiftDate = other.optionalShiftDate().map(LocalDateFilter::copy).orElse(null);
        this.slot = other.optionalSlot().map(ShiftSlotFilter::copy).orElse(null);
        this.operatorIds = other.optionalOperatorIds().map(StringFilter::copy).orElse(null);
        this.shiftLeadId = other.optionalShiftLeadId().map(UUIDFilter::copy).orElse(null);
        this.startsAt = other.optionalStartsAt().map(InstantFilter::copy).orElse(null);
        this.endsAt = other.optionalEndsAt().map(InstantFilter::copy).orElse(null);
        this.rosterId = other.optionalRosterId().map(UUIDFilter::copy).orElse(null);
        this.shiftReportId = other.optionalShiftReportId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ShiftCriteria copy() {
        return new ShiftCriteria(this);
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

    public StringFilter getReference() {
        return reference;
    }

    public Optional<StringFilter> optionalReference() {
        return Optional.ofNullable(reference);
    }

    public StringFilter reference() {
        if (reference == null) {
            setReference(new StringFilter());
        }
        return reference;
    }

    public void setReference(StringFilter reference) {
        this.reference = reference;
    }

    public LocalDateFilter getShiftDate() {
        return shiftDate;
    }

    public Optional<LocalDateFilter> optionalShiftDate() {
        return Optional.ofNullable(shiftDate);
    }

    public LocalDateFilter shiftDate() {
        if (shiftDate == null) {
            setShiftDate(new LocalDateFilter());
        }
        return shiftDate;
    }

    public void setShiftDate(LocalDateFilter shiftDate) {
        this.shiftDate = shiftDate;
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

    public StringFilter getOperatorIds() {
        return operatorIds;
    }

    public Optional<StringFilter> optionalOperatorIds() {
        return Optional.ofNullable(operatorIds);
    }

    public StringFilter operatorIds() {
        if (operatorIds == null) {
            setOperatorIds(new StringFilter());
        }
        return operatorIds;
    }

    public void setOperatorIds(StringFilter operatorIds) {
        this.operatorIds = operatorIds;
    }

    public UUIDFilter getShiftLeadId() {
        return shiftLeadId;
    }

    public Optional<UUIDFilter> optionalShiftLeadId() {
        return Optional.ofNullable(shiftLeadId);
    }

    public UUIDFilter shiftLeadId() {
        if (shiftLeadId == null) {
            setShiftLeadId(new UUIDFilter());
        }
        return shiftLeadId;
    }

    public void setShiftLeadId(UUIDFilter shiftLeadId) {
        this.shiftLeadId = shiftLeadId;
    }

    public InstantFilter getStartsAt() {
        return startsAt;
    }

    public Optional<InstantFilter> optionalStartsAt() {
        return Optional.ofNullable(startsAt);
    }

    public InstantFilter startsAt() {
        if (startsAt == null) {
            setStartsAt(new InstantFilter());
        }
        return startsAt;
    }

    public void setStartsAt(InstantFilter startsAt) {
        this.startsAt = startsAt;
    }

    public InstantFilter getEndsAt() {
        return endsAt;
    }

    public Optional<InstantFilter> optionalEndsAt() {
        return Optional.ofNullable(endsAt);
    }

    public InstantFilter endsAt() {
        if (endsAt == null) {
            setEndsAt(new InstantFilter());
        }
        return endsAt;
    }

    public void setEndsAt(InstantFilter endsAt) {
        this.endsAt = endsAt;
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

    public UUIDFilter getShiftReportId() {
        return shiftReportId;
    }

    public Optional<UUIDFilter> optionalShiftReportId() {
        return Optional.ofNullable(shiftReportId);
    }

    public UUIDFilter shiftReportId() {
        if (shiftReportId == null) {
            setShiftReportId(new UUIDFilter());
        }
        return shiftReportId;
    }

    public void setShiftReportId(UUIDFilter shiftReportId) {
        this.shiftReportId = shiftReportId;
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
        final ShiftCriteria that = (ShiftCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(shiftDate, that.shiftDate) &&
            Objects.equals(slot, that.slot) &&
            Objects.equals(operatorIds, that.operatorIds) &&
            Objects.equals(shiftLeadId, that.shiftLeadId) &&
            Objects.equals(startsAt, that.startsAt) &&
            Objects.equals(endsAt, that.endsAt) &&
            Objects.equals(rosterId, that.rosterId) &&
            Objects.equals(shiftReportId, that.shiftReportId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reference, shiftDate, slot, operatorIds, shiftLeadId, startsAt, endsAt, rosterId, shiftReportId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ShiftCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalShiftDate().map(f -> "shiftDate=" + f + ", ").orElse("") +
            optionalSlot().map(f -> "slot=" + f + ", ").orElse("") +
            optionalOperatorIds().map(f -> "operatorIds=" + f + ", ").orElse("") +
            optionalShiftLeadId().map(f -> "shiftLeadId=" + f + ", ").orElse("") +
            optionalStartsAt().map(f -> "startsAt=" + f + ", ").orElse("") +
            optionalEndsAt().map(f -> "endsAt=" + f + ", ").orElse("") +
            optionalRosterId().map(f -> "rosterId=" + f + ", ").orElse("") +
            optionalShiftReportId().map(f -> "shiftReportId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
