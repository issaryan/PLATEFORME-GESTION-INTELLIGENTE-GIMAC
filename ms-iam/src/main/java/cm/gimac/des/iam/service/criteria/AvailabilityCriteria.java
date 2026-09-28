package cm.gimac.des.iam.service.criteria;

import cm.gimac.des.iam.domain.enumeration.AvailabilityType;
import cm.gimac.des.iam.domain.enumeration.ValidationState;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.iam.domain.Availability} entity. This class is used
 * in {@link cm.gimac.des.iam.web.rest.AvailabilityResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /availabilities?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AvailabilityCriteria implements Serializable, Criteria {

    /**
     * Class for filtering AvailabilityType
     */
    public static class AvailabilityTypeFilter extends Filter<AvailabilityType> {

        public AvailabilityTypeFilter() {}

        public AvailabilityTypeFilter(AvailabilityTypeFilter filter) {
            super(filter);
        }

        @Override
        public AvailabilityTypeFilter copy() {
            return new AvailabilityTypeFilter(this);
        }
    }

    /**
     * Class for filtering ValidationState
     */
    public static class ValidationStateFilter extends Filter<ValidationState> {

        public ValidationStateFilter() {}

        public ValidationStateFilter(ValidationStateFilter filter) {
            super(filter);
        }

        @Override
        public ValidationStateFilter copy() {
            return new ValidationStateFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private AvailabilityTypeFilter type;

    private InstantFilter startsAt;

    private InstantFilter endsAt;

    private StringFilter reason;

    private UUIDFilter justificationDocumentId;

    private ValidationStateFilter validationState;

    private UUIDFilter validatorId;

    private InstantFilter validatedAt;

    private UUIDFilter collaboratorId;

    private Boolean distinct;

    public AvailabilityCriteria() {}

    public AvailabilityCriteria(AvailabilityCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.type = other.optionalType().map(AvailabilityTypeFilter::copy).orElse(null);
        this.startsAt = other.optionalStartsAt().map(InstantFilter::copy).orElse(null);
        this.endsAt = other.optionalEndsAt().map(InstantFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.justificationDocumentId = other.optionalJustificationDocumentId().map(UUIDFilter::copy).orElse(null);
        this.validationState = other.optionalValidationState().map(ValidationStateFilter::copy).orElse(null);
        this.validatorId = other.optionalValidatorId().map(UUIDFilter::copy).orElse(null);
        this.validatedAt = other.optionalValidatedAt().map(InstantFilter::copy).orElse(null);
        this.collaboratorId = other.optionalCollaboratorId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public AvailabilityCriteria copy() {
        return new AvailabilityCriteria(this);
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

    public AvailabilityTypeFilter getType() {
        return type;
    }

    public Optional<AvailabilityTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public AvailabilityTypeFilter type() {
        if (type == null) {
            setType(new AvailabilityTypeFilter());
        }
        return type;
    }

    public void setType(AvailabilityTypeFilter type) {
        this.type = type;
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

    public StringFilter getReason() {
        return reason;
    }

    public Optional<StringFilter> optionalReason() {
        return Optional.ofNullable(reason);
    }

    public StringFilter reason() {
        if (reason == null) {
            setReason(new StringFilter());
        }
        return reason;
    }

    public void setReason(StringFilter reason) {
        this.reason = reason;
    }

    public UUIDFilter getJustificationDocumentId() {
        return justificationDocumentId;
    }

    public Optional<UUIDFilter> optionalJustificationDocumentId() {
        return Optional.ofNullable(justificationDocumentId);
    }

    public UUIDFilter justificationDocumentId() {
        if (justificationDocumentId == null) {
            setJustificationDocumentId(new UUIDFilter());
        }
        return justificationDocumentId;
    }

    public void setJustificationDocumentId(UUIDFilter justificationDocumentId) {
        this.justificationDocumentId = justificationDocumentId;
    }

    public ValidationStateFilter getValidationState() {
        return validationState;
    }

    public Optional<ValidationStateFilter> optionalValidationState() {
        return Optional.ofNullable(validationState);
    }

    public ValidationStateFilter validationState() {
        if (validationState == null) {
            setValidationState(new ValidationStateFilter());
        }
        return validationState;
    }

    public void setValidationState(ValidationStateFilter validationState) {
        this.validationState = validationState;
    }

    public UUIDFilter getValidatorId() {
        return validatorId;
    }

    public Optional<UUIDFilter> optionalValidatorId() {
        return Optional.ofNullable(validatorId);
    }

    public UUIDFilter validatorId() {
        if (validatorId == null) {
            setValidatorId(new UUIDFilter());
        }
        return validatorId;
    }

    public void setValidatorId(UUIDFilter validatorId) {
        this.validatorId = validatorId;
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
        final AvailabilityCriteria that = (AvailabilityCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(type, that.type) &&
            Objects.equals(startsAt, that.startsAt) &&
            Objects.equals(endsAt, that.endsAt) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(justificationDocumentId, that.justificationDocumentId) &&
            Objects.equals(validationState, that.validationState) &&
            Objects.equals(validatorId, that.validatorId) &&
            Objects.equals(validatedAt, that.validatedAt) &&
            Objects.equals(collaboratorId, that.collaboratorId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            type,
            startsAt,
            endsAt,
            reason,
            justificationDocumentId,
            validationState,
            validatorId,
            validatedAt,
            collaboratorId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AvailabilityCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalStartsAt().map(f -> "startsAt=" + f + ", ").orElse("") +
            optionalEndsAt().map(f -> "endsAt=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalJustificationDocumentId().map(f -> "justificationDocumentId=" + f + ", ").orElse("") +
            optionalValidationState().map(f -> "validationState=" + f + ", ").orElse("") +
            optionalValidatorId().map(f -> "validatorId=" + f + ", ").orElse("") +
            optionalValidatedAt().map(f -> "validatedAt=" + f + ", ").orElse("") +
            optionalCollaboratorId().map(f -> "collaboratorId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
