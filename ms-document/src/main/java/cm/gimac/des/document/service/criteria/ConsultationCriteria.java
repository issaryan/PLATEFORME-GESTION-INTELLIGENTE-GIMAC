package cm.gimac.des.document.service.criteria;

import cm.gimac.des.document.domain.enumeration.ConsultationAction;
import cm.gimac.des.document.domain.enumeration.ConsultationChannel;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.document.domain.Consultation} entity. This class is used
 * in {@link cm.gimac.des.document.web.rest.ConsultationResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /consultations?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConsultationCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ConsultationAction
     */
    public static class ConsultationActionFilter extends Filter<ConsultationAction> {

        public ConsultationActionFilter() {}

        public ConsultationActionFilter(ConsultationActionFilter filter) {
            super(filter);
        }

        @Override
        public ConsultationActionFilter copy() {
            return new ConsultationActionFilter(this);
        }
    }

    /**
     * Class for filtering ConsultationChannel
     */
    public static class ConsultationChannelFilter extends Filter<ConsultationChannel> {

        public ConsultationChannelFilter() {}

        public ConsultationChannelFilter(ConsultationChannelFilter filter) {
            super(filter);
        }

        @Override
        public ConsultationChannelFilter copy() {
            return new ConsultationChannelFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter userId;

    private UUIDFilter institutionId;

    private ConsultationActionFilter action;

    private ConsultationChannelFilter channel;

    private InstantFilter consultedAt;

    private UUIDFilter correlationId;

    private BooleanFilter granted;

    private StringFilter refusalReason;

    private UUIDFilter documentId;

    private Boolean distinct;

    public ConsultationCriteria() {}

    public ConsultationCriteria(ConsultationCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.userId = other.optionalUserId().map(UUIDFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.action = other.optionalAction().map(ConsultationActionFilter::copy).orElse(null);
        this.channel = other.optionalChannel().map(ConsultationChannelFilter::copy).orElse(null);
        this.consultedAt = other.optionalConsultedAt().map(InstantFilter::copy).orElse(null);
        this.correlationId = other.optionalCorrelationId().map(UUIDFilter::copy).orElse(null);
        this.granted = other.optionalGranted().map(BooleanFilter::copy).orElse(null);
        this.refusalReason = other.optionalRefusalReason().map(StringFilter::copy).orElse(null);
        this.documentId = other.optionalDocumentId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ConsultationCriteria copy() {
        return new ConsultationCriteria(this);
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

    public UUIDFilter getUserId() {
        return userId;
    }

    public Optional<UUIDFilter> optionalUserId() {
        return Optional.ofNullable(userId);
    }

    public UUIDFilter userId() {
        if (userId == null) {
            setUserId(new UUIDFilter());
        }
        return userId;
    }

    public void setUserId(UUIDFilter userId) {
        this.userId = userId;
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

    public ConsultationActionFilter getAction() {
        return action;
    }

    public Optional<ConsultationActionFilter> optionalAction() {
        return Optional.ofNullable(action);
    }

    public ConsultationActionFilter action() {
        if (action == null) {
            setAction(new ConsultationActionFilter());
        }
        return action;
    }

    public void setAction(ConsultationActionFilter action) {
        this.action = action;
    }

    public ConsultationChannelFilter getChannel() {
        return channel;
    }

    public Optional<ConsultationChannelFilter> optionalChannel() {
        return Optional.ofNullable(channel);
    }

    public ConsultationChannelFilter channel() {
        if (channel == null) {
            setChannel(new ConsultationChannelFilter());
        }
        return channel;
    }

    public void setChannel(ConsultationChannelFilter channel) {
        this.channel = channel;
    }

    public InstantFilter getConsultedAt() {
        return consultedAt;
    }

    public Optional<InstantFilter> optionalConsultedAt() {
        return Optional.ofNullable(consultedAt);
    }

    public InstantFilter consultedAt() {
        if (consultedAt == null) {
            setConsultedAt(new InstantFilter());
        }
        return consultedAt;
    }

    public void setConsultedAt(InstantFilter consultedAt) {
        this.consultedAt = consultedAt;
    }

    public UUIDFilter getCorrelationId() {
        return correlationId;
    }

    public Optional<UUIDFilter> optionalCorrelationId() {
        return Optional.ofNullable(correlationId);
    }

    public UUIDFilter correlationId() {
        if (correlationId == null) {
            setCorrelationId(new UUIDFilter());
        }
        return correlationId;
    }

    public void setCorrelationId(UUIDFilter correlationId) {
        this.correlationId = correlationId;
    }

    public BooleanFilter getGranted() {
        return granted;
    }

    public Optional<BooleanFilter> optionalGranted() {
        return Optional.ofNullable(granted);
    }

    public BooleanFilter granted() {
        if (granted == null) {
            setGranted(new BooleanFilter());
        }
        return granted;
    }

    public void setGranted(BooleanFilter granted) {
        this.granted = granted;
    }

    public StringFilter getRefusalReason() {
        return refusalReason;
    }

    public Optional<StringFilter> optionalRefusalReason() {
        return Optional.ofNullable(refusalReason);
    }

    public StringFilter refusalReason() {
        if (refusalReason == null) {
            setRefusalReason(new StringFilter());
        }
        return refusalReason;
    }

    public void setRefusalReason(StringFilter refusalReason) {
        this.refusalReason = refusalReason;
    }

    public UUIDFilter getDocumentId() {
        return documentId;
    }

    public Optional<UUIDFilter> optionalDocumentId() {
        return Optional.ofNullable(documentId);
    }

    public UUIDFilter documentId() {
        if (documentId == null) {
            setDocumentId(new UUIDFilter());
        }
        return documentId;
    }

    public void setDocumentId(UUIDFilter documentId) {
        this.documentId = documentId;
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
        final ConsultationCriteria that = (ConsultationCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(action, that.action) &&
            Objects.equals(channel, that.channel) &&
            Objects.equals(consultedAt, that.consultedAt) &&
            Objects.equals(correlationId, that.correlationId) &&
            Objects.equals(granted, that.granted) &&
            Objects.equals(refusalReason, that.refusalReason) &&
            Objects.equals(documentId, that.documentId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            userId,
            institutionId,
            action,
            channel,
            consultedAt,
            correlationId,
            granted,
            refusalReason,
            documentId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConsultationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalUserId().map(f -> "userId=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalAction().map(f -> "action=" + f + ", ").orElse("") +
            optionalChannel().map(f -> "channel=" + f + ", ").orElse("") +
            optionalConsultedAt().map(f -> "consultedAt=" + f + ", ").orElse("") +
            optionalCorrelationId().map(f -> "correlationId=" + f + ", ").orElse("") +
            optionalGranted().map(f -> "granted=" + f + ", ").orElse("") +
            optionalRefusalReason().map(f -> "refusalReason=" + f + ", ").orElse("") +
            optionalDocumentId().map(f -> "documentId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
