package cm.gimac.des.pilotage.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.pilotage.domain.AbsenceProjection} entity. This class is used
 * in {@link cm.gimac.des.pilotage.web.rest.AbsenceProjectionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /absence-projections?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AbsenceProjectionCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter availabilityId;

    private UUIDFilter collaboratorId;

    private InstantFilter startsAt;

    private InstantFilter endsAt;

    private InstantFilter updatedAt;

    private Boolean distinct;

    public AbsenceProjectionCriteria() {}

    public AbsenceProjectionCriteria(AbsenceProjectionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.availabilityId = other.optionalAvailabilityId().map(UUIDFilter::copy).orElse(null);
        this.collaboratorId = other.optionalCollaboratorId().map(UUIDFilter::copy).orElse(null);
        this.startsAt = other.optionalStartsAt().map(InstantFilter::copy).orElse(null);
        this.endsAt = other.optionalEndsAt().map(InstantFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public AbsenceProjectionCriteria copy() {
        return new AbsenceProjectionCriteria(this);
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

    public UUIDFilter getCollaboratorId() {
        return collaboratorId;
    }

    public Optional<UUIDFilter> optionalCollaboratorId() {
        return Optional.ofNullable(collaboratorId);
    }

    public UUIDFilter collaboratorId() {
        if (collaboratorId == null) {
            setCollaboratorId(new UUIDFilter());
        }
        return collaboratorId;
    }

    public void setCollaboratorId(UUIDFilter collaboratorId) {
        this.collaboratorId = collaboratorId;
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

    public InstantFilter getUpdatedAt() {
        return updatedAt;
    }

    public Optional<InstantFilter> optionalUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public InstantFilter updatedAt() {
        if (updatedAt == null) {
            setUpdatedAt(new InstantFilter());
        }
        return updatedAt;
    }

    public void setUpdatedAt(InstantFilter updatedAt) {
        this.updatedAt = updatedAt;
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
        final AbsenceProjectionCriteria that = (AbsenceProjectionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(availabilityId, that.availabilityId) &&
            Objects.equals(collaboratorId, that.collaboratorId) &&
            Objects.equals(startsAt, that.startsAt) &&
            Objects.equals(endsAt, that.endsAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, availabilityId, collaboratorId, startsAt, endsAt, updatedAt, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AbsenceProjectionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalAvailabilityId().map(f -> "availabilityId=" + f + ", ").orElse("") +
            optionalCollaboratorId().map(f -> "collaboratorId=" + f + ", ").orElse("") +
            optionalStartsAt().map(f -> "startsAt=" + f + ", ").orElse("") +
            optionalEndsAt().map(f -> "endsAt=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
