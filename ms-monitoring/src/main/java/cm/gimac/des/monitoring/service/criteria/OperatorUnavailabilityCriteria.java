package cm.gimac.des.monitoring.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.monitoring.domain.OperatorUnavailability} entity. This class is used
 * in {@link cm.gimac.des.monitoring.web.rest.OperatorUnavailabilityResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /operator-unavailabilities?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OperatorUnavailabilityCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter availabilityId;

    private UUIDFilter operatorId;

    private InstantFilter startsAt;

    private InstantFilter endsAt;

    private InstantFilter validatedAt;

    private BooleanFilter cancelled;

    private Boolean distinct;

    public OperatorUnavailabilityCriteria() {}

    public OperatorUnavailabilityCriteria(OperatorUnavailabilityCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.availabilityId = other.optionalAvailabilityId().map(UUIDFilter::copy).orElse(null);
        this.operatorId = other.optionalOperatorId().map(UUIDFilter::copy).orElse(null);
        this.startsAt = other.optionalStartsAt().map(InstantFilter::copy).orElse(null);
        this.endsAt = other.optionalEndsAt().map(InstantFilter::copy).orElse(null);
        this.validatedAt = other.optionalValidatedAt().map(InstantFilter::copy).orElse(null);
        this.cancelled = other.optionalCancelled().map(BooleanFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public OperatorUnavailabilityCriteria copy() {
        return new OperatorUnavailabilityCriteria(this);
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

    public UUIDFilter getAvailabilityId() {
        return availabilityId;
    }

    public Optional<UUIDFilter> optionalAvailabilityId() {
        return Optional.ofNullable(availabilityId);
    }

    public UUIDFilter availabilityId() {
        if (availabilityId == null) {
            setAvailabilityId(new UUIDFilter());
        }
        return availabilityId;
    }

    public void setAvailabilityId(UUIDFilter availabilityId) {
        this.availabilityId = availabilityId;
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

    public InstantFilter getValidatedAt() {
        return validatedAt;
    }

    public Optional<InstantFilter> optionalValidatedAt() {
        return Optional.ofNullable(validatedAt);
    }

    public InstantFilter validatedAt() {
        if (validatedAt == null) {
            setValidatedAt(new InstantFilter());
        }
        return validatedAt;
    }

    public void setValidatedAt(InstantFilter validatedAt) {
        this.validatedAt = validatedAt;
    }

    public BooleanFilter getCancelled() {
        return cancelled;
    }

    public Optional<BooleanFilter> optionalCancelled() {
        return Optional.ofNullable(cancelled);
    }

    public BooleanFilter cancelled() {
        if (cancelled == null) {
            setCancelled(new BooleanFilter());
        }
        return cancelled;
    }

    public void setCancelled(BooleanFilter cancelled) {
        this.cancelled = cancelled;
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
        final OperatorUnavailabilityCriteria that = (OperatorUnavailabilityCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(availabilityId, that.availabilityId) &&
            Objects.equals(operatorId, that.operatorId) &&
            Objects.equals(startsAt, that.startsAt) &&
            Objects.equals(endsAt, that.endsAt) &&
            Objects.equals(validatedAt, that.validatedAt) &&
            Objects.equals(cancelled, that.cancelled) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, availabilityId, operatorId, startsAt, endsAt, validatedAt, cancelled, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OperatorUnavailabilityCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalAvailabilityId().map(f -> "availabilityId=" + f + ", ").orElse("") +
            optionalOperatorId().map(f -> "operatorId=" + f + ", ").orElse("") +
            optionalStartsAt().map(f -> "startsAt=" + f + ", ").orElse("") +
            optionalEndsAt().map(f -> "endsAt=" + f + ", ").orElse("") +
            optionalValidatedAt().map(f -> "validatedAt=" + f + ", ").orElse("") +
            optionalCancelled().map(f -> "cancelled=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
