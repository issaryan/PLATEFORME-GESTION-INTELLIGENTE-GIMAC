package cm.gimac.des.participant.service.criteria;

import cm.gimac.des.participant.domain.enumeration.InteractionNature;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.participant.domain.Interaction} entity. This class is used
 * in {@link cm.gimac.des.participant.web.rest.InteractionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /interactions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class InteractionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering InteractionNature
     */
    public static class InteractionNatureFilter extends Filter<InteractionNature> {

        public InteractionNatureFilter() {}

        public InteractionNatureFilter(InteractionNatureFilter filter) {
            super(filter);
        }

        @Override
        public InteractionNatureFilter copy() {
            return new InteractionNatureFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private InteractionNatureFilter nature;

    private StringFilter summary;

    private InstantFilter occurredAt;

    private UUIDFilter authorId;

    private StringFilter reference;

    private UUIDFilter notificationId;

    private UUIDFilter sourceEventId;

    private UUIDFilter institutionId;

    private Boolean distinct;

    public InteractionCriteria() {}

    public InteractionCriteria(InteractionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.nature = other.optionalNature().map(InteractionNatureFilter::copy).orElse(null);
        this.summary = other.optionalSummary().map(StringFilter::copy).orElse(null);
        this.occurredAt = other.optionalOccurredAt().map(InstantFilter::copy).orElse(null);
        this.authorId = other.optionalAuthorId().map(UUIDFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.notificationId = other.optionalNotificationId().map(UUIDFilter::copy).orElse(null);
        this.sourceEventId = other.optionalSourceEventId().map(UUIDFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public InteractionCriteria copy() {
        return new InteractionCriteria(this);
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

    public InteractionNatureFilter getNature() {
        return nature;
    }

    public Optional<InteractionNatureFilter> optionalNature() {
        return Optional.ofNullable(nature);
    }

    public InteractionNatureFilter nature() {
        if (nature == null) {
            setNature(new InteractionNatureFilter());
        }
        return nature;
    }

    public void setNature(InteractionNatureFilter nature) {
        this.nature = nature;
    }

    public StringFilter getSummary() {
        return summary;
    }

    public Optional<StringFilter> optionalSummary() {
        return Optional.ofNullable(summary);
    }

    public StringFilter summary() {
        if (summary == null) {
            setSummary(new StringFilter());
        }
        return summary;
    }

    public void setSummary(StringFilter summary) {
        this.summary = summary;
    }

    public InstantFilter getOccurredAt() {
        return occurredAt;
    }

    public Optional<InstantFilter> optionalOccurredAt() {
        return Optional.ofNullable(occurredAt);
    }

    public InstantFilter occurredAt() {
        if (occurredAt == null) {
            setOccurredAt(new InstantFilter());
        }
        return occurredAt;
    }

    public void setOccurredAt(InstantFilter occurredAt) {
        this.occurredAt = occurredAt;
    }

    public UUIDFilter getAuthorId() {
        return authorId;
    }

    public Optional<UUIDFilter> optionalAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public UUIDFilter authorId() {
        if (authorId == null) {
            setAuthorId(new UUIDFilter());
        }
        return authorId;
    }

    public void setAuthorId(UUIDFilter authorId) {
        this.authorId = authorId;
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

    public UUIDFilter getNotificationId() {
        return notificationId;
    }

    public Optional<UUIDFilter> optionalNotificationId() {
        return Optional.ofNullable(notificationId);
    }

    public UUIDFilter notificationId() {
        if (notificationId == null) {
            setNotificationId(new UUIDFilter());
        }
        return notificationId;
    }

    public void setNotificationId(UUIDFilter notificationId) {
        this.notificationId = notificationId;
    }

    public UUIDFilter getSourceEventId() {
        return sourceEventId;
    }

    public Optional<UUIDFilter> optionalSourceEventId() {
        return Optional.ofNullable(sourceEventId);
    }

    public UUIDFilter sourceEventId() {
        if (sourceEventId == null) {
            setSourceEventId(new UUIDFilter());
        }
        return sourceEventId;
    }

    public void setSourceEventId(UUIDFilter sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public UUIDFilter getInstitutionId() {
        return institutionId;
    }

    public Optional<UUIDFilter> optionalInstitutionId() {
        return Optional.ofNullable(institutionId);
    }

    public UUIDFilter institutionId() {
        if (institutionId == null) {
            setInstitutionId(new UUIDFilter());
        }
        return institutionId;
    }

    public void setInstitutionId(UUIDFilter institutionId) {
        this.institutionId = institutionId;
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
        final InteractionCriteria that = (InteractionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(nature, that.nature) &&
            Objects.equals(summary, that.summary) &&
            Objects.equals(occurredAt, that.occurredAt) &&
            Objects.equals(authorId, that.authorId) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(notificationId, that.notificationId) &&
            Objects.equals(sourceEventId, that.sourceEventId) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nature, summary, occurredAt, authorId, reference, notificationId, sourceEventId, institutionId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "InteractionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalNature().map(f -> "nature=" + f + ", ").orElse("") +
            optionalSummary().map(f -> "summary=" + f + ", ").orElse("") +
            optionalOccurredAt().map(f -> "occurredAt=" + f + ", ").orElse("") +
            optionalAuthorId().map(f -> "authorId=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalNotificationId().map(f -> "notificationId=" + f + ", ").orElse("") +
            optionalSourceEventId().map(f -> "sourceEventId=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
