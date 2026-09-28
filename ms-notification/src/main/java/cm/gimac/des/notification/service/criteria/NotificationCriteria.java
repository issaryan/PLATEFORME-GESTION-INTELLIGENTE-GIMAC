package cm.gimac.des.notification.service.criteria;

import cm.gimac.des.notification.domain.enumeration.NotificationState;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.notification.domain.Notification} entity. This class is used
 * in {@link cm.gimac.des.notification.web.rest.NotificationResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /notifications?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationCriteria implements Serializable, Criteria {

    /**
     * Class for filtering UrgencyLevel
     */
    public static class UrgencyLevelFilter extends Filter<UrgencyLevel> {

        public UrgencyLevelFilter() {}

        public UrgencyLevelFilter(UrgencyLevelFilter filter) {
            super(filter);
        }

        @Override
        public UrgencyLevelFilter copy() {
            return new UrgencyLevelFilter(this);
        }
    }

    /**
     * Class for filtering NotificationState
     */
    public static class NotificationStateFilter extends Filter<NotificationState> {

        public NotificationStateFilter() {}

        public NotificationStateFilter(NotificationStateFilter filter) {
            super(filter);
        }

        @Override
        public NotificationStateFilter copy() {
            return new NotificationStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter notificationType;

    private UrgencyLevelFilter urgency;

    private StringFilter subject;

    private StringFilter objectType;

    private StringFilter objectId;

    private UUIDFilter institutionId;

    private UUIDFilter sourceEventId;

    private StringFilter sourceService;

    private BooleanFilter urgentPort;

    private UUIDFilter correlationId;

    private StringFilter groupKey;

    private InstantFilter requestedAt;

    private InstantFilter ackDeadline;

    private IntegerFilter escalationLevel;

    private NotificationStateFilter state;

    private UUIDFilter acknowledgedBy;

    private InstantFilter acknowledgedAt;

    private BooleanFilter summary;

    private IntegerFilter occurrenceCount;

    private UUIDFilter consolidationWindowId;

    private UUIDFilter summarizedWindowId;

    private Boolean distinct;

    public NotificationCriteria() {}

    public NotificationCriteria(NotificationCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.notificationType = other.optionalNotificationType().map(StringFilter::copy).orElse(null);
        this.urgency = other.optionalUrgency().map(UrgencyLevelFilter::copy).orElse(null);
        this.subject = other.optionalSubject().map(StringFilter::copy).orElse(null);
        this.objectType = other.optionalObjectType().map(StringFilter::copy).orElse(null);
        this.objectId = other.optionalObjectId().map(StringFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.sourceEventId = other.optionalSourceEventId().map(UUIDFilter::copy).orElse(null);
        this.sourceService = other.optionalSourceService().map(StringFilter::copy).orElse(null);
        this.urgentPort = other.optionalUrgentPort().map(BooleanFilter::copy).orElse(null);
        this.correlationId = other.optionalCorrelationId().map(UUIDFilter::copy).orElse(null);
        this.groupKey = other.optionalGroupKey().map(StringFilter::copy).orElse(null);
        this.requestedAt = other.optionalRequestedAt().map(InstantFilter::copy).orElse(null);
        this.ackDeadline = other.optionalAckDeadline().map(InstantFilter::copy).orElse(null);
        this.escalationLevel = other.optionalEscalationLevel().map(IntegerFilter::copy).orElse(null);
        this.state = other.optionalState().map(NotificationStateFilter::copy).orElse(null);
        this.acknowledgedBy = other.optionalAcknowledgedBy().map(UUIDFilter::copy).orElse(null);
        this.acknowledgedAt = other.optionalAcknowledgedAt().map(InstantFilter::copy).orElse(null);
        this.summary = other.optionalSummary().map(BooleanFilter::copy).orElse(null);
        this.occurrenceCount = other.optionalOccurrenceCount().map(IntegerFilter::copy).orElse(null);
        this.consolidationWindowId = other.optionalConsolidationWindowId().map(UUIDFilter::copy).orElse(null);
        this.summarizedWindowId = other.optionalSummarizedWindowId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public NotificationCriteria copy() {
        return new NotificationCriteria(this);
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

    public StringFilter getNotificationType() {
        return notificationType;
    }

    public Optional<StringFilter> optionalNotificationType() {
        return Optional.ofNullable(notificationType);
    }

    public StringFilter notificationType() {
        if (notificationType == null) {
            setNotificationType(new StringFilter());
        }
        return notificationType;
    }

    public void setNotificationType(StringFilter notificationType) {
        this.notificationType = notificationType;
    }

    public UrgencyLevelFilter getUrgency() {
        return urgency;
    }

    public Optional<UrgencyLevelFilter> optionalUrgency() {
        return Optional.ofNullable(urgency);
    }

    public UrgencyLevelFilter urgency() {
        if (urgency == null) {
            setUrgency(new UrgencyLevelFilter());
        }
        return urgency;
    }

    public void setUrgency(UrgencyLevelFilter urgency) {
        this.urgency = urgency;
    }

    public StringFilter getSubject() {
        return subject;
    }

    public Optional<StringFilter> optionalSubject() {
        return Optional.ofNullable(subject);
    }

    public StringFilter subject() {
        if (subject == null) {
            setSubject(new StringFilter());
        }
        return subject;
    }

    public void setSubject(StringFilter subject) {
        this.subject = subject;
    }

    public StringFilter getObjectType() {
        return objectType;
    }

    public Optional<StringFilter> optionalObjectType() {
        return Optional.ofNullable(objectType);
    }

    public StringFilter objectType() {
        if (objectType == null) {
            setObjectType(new StringFilter());
        }
        return objectType;
    }

    public void setObjectType(StringFilter objectType) {
        this.objectType = objectType;
    }

    public StringFilter getObjectId() {
        return objectId;
    }

    public Optional<StringFilter> optionalObjectId() {
        return Optional.ofNullable(objectId);
    }

    public StringFilter objectId() {
        if (objectId == null) {
            setObjectId(new StringFilter());
        }
        return objectId;
    }

    public void setObjectId(StringFilter objectId) {
        this.objectId = objectId;
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

    public StringFilter getSourceService() {
        return sourceService;
    }

    public Optional<StringFilter> optionalSourceService() {
        return Optional.ofNullable(sourceService);
    }

    public StringFilter sourceService() {
        if (sourceService == null) {
            setSourceService(new StringFilter());
        }
        return sourceService;
    }

    public void setSourceService(StringFilter sourceService) {
        this.sourceService = sourceService;
    }

    public BooleanFilter getUrgentPort() {
        return urgentPort;
    }

    public Optional<BooleanFilter> optionalUrgentPort() {
        return Optional.ofNullable(urgentPort);
    }

    public BooleanFilter urgentPort() {
        if (urgentPort == null) {
            setUrgentPort(new BooleanFilter());
        }
        return urgentPort;
    }

    public void setUrgentPort(BooleanFilter urgentPort) {
        this.urgentPort = urgentPort;
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

    public StringFilter getGroupKey() {
        return groupKey;
    }

    public Optional<StringFilter> optionalGroupKey() {
        return Optional.ofNullable(groupKey);
    }

    public StringFilter groupKey() {
        if (groupKey == null) {
            setGroupKey(new StringFilter());
        }
        return groupKey;
    }

    public void setGroupKey(StringFilter groupKey) {
        this.groupKey = groupKey;
    }

    public InstantFilter getRequestedAt() {
        return requestedAt;
    }

    public Optional<InstantFilter> optionalRequestedAt() {
        return Optional.ofNullable(requestedAt);
    }

    public InstantFilter requestedAt() {
        if (requestedAt == null) {
            setRequestedAt(new InstantFilter());
        }
        return requestedAt;
    }

    public void setRequestedAt(InstantFilter requestedAt) {
        this.requestedAt = requestedAt;
    }

    public InstantFilter getAckDeadline() {
        return ackDeadline;
    }

    public Optional<InstantFilter> optionalAckDeadline() {
        return Optional.ofNullable(ackDeadline);
    }

    public InstantFilter ackDeadline() {
        if (ackDeadline == null) {
            setAckDeadline(new InstantFilter());
        }
        return ackDeadline;
    }

    public void setAckDeadline(InstantFilter ackDeadline) {
        this.ackDeadline = ackDeadline;
    }

    public IntegerFilter getEscalationLevel() {
        return escalationLevel;
    }

    public Optional<IntegerFilter> optionalEscalationLevel() {
        return Optional.ofNullable(escalationLevel);
    }

    public IntegerFilter escalationLevel() {
        if (escalationLevel == null) {
            setEscalationLevel(new IntegerFilter());
        }
        return escalationLevel;
    }

    public void setEscalationLevel(IntegerFilter escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public NotificationStateFilter getState() {
        return state;
    }

    public Optional<NotificationStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public NotificationStateFilter state() {
        if (state == null) {
            setState(new NotificationStateFilter());
        }
        return state;
    }

    public void setState(NotificationStateFilter state) {
        this.state = state;
    }

    public UUIDFilter getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public Optional<UUIDFilter> optionalAcknowledgedBy() {
        return Optional.ofNullable(acknowledgedBy);
    }

    public UUIDFilter acknowledgedBy() {
        if (acknowledgedBy == null) {
            setAcknowledgedBy(new UUIDFilter());
        }
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(UUIDFilter acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public InstantFilter getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Optional<InstantFilter> optionalAcknowledgedAt() {
        return Optional.ofNullable(acknowledgedAt);
    }

    public InstantFilter acknowledgedAt() {
        if (acknowledgedAt == null) {
            setAcknowledgedAt(new InstantFilter());
        }
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(InstantFilter acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public BooleanFilter getSummary() {
        return summary;
    }

    public Optional<BooleanFilter> optionalSummary() {
        return Optional.ofNullable(summary);
    }

    public BooleanFilter summary() {
        if (summary == null) {
            setSummary(new BooleanFilter());
        }
        return summary;
    }

    public void setSummary(BooleanFilter summary) {
        this.summary = summary;
    }

    public IntegerFilter getOccurrenceCount() {
        return occurrenceCount;
    }

    public Optional<IntegerFilter> optionalOccurrenceCount() {
        return Optional.ofNullable(occurrenceCount);
    }

    public IntegerFilter occurrenceCount() {
        if (occurrenceCount == null) {
            setOccurrenceCount(new IntegerFilter());
        }
        return occurrenceCount;
    }

    public void setOccurrenceCount(IntegerFilter occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public UUIDFilter getConsolidationWindowId() {
        return consolidationWindowId;
    }

    public Optional<UUIDFilter> optionalConsolidationWindowId() {
        return Optional.ofNullable(consolidationWindowId);
    }

    public UUIDFilter consolidationWindowId() {
        if (consolidationWindowId == null) {
            setConsolidationWindowId(new UUIDFilter());
        }
        return consolidationWindowId;
    }

    public void setConsolidationWindowId(UUIDFilter consolidationWindowId) {
        this.consolidationWindowId = consolidationWindowId;
    }

    public UUIDFilter getSummarizedWindowId() {
        return summarizedWindowId;
    }

    public Optional<UUIDFilter> optionalSummarizedWindowId() {
        return Optional.ofNullable(summarizedWindowId);
    }

    public UUIDFilter summarizedWindowId() {
        if (summarizedWindowId == null) {
            setSummarizedWindowId(new UUIDFilter());
        }
        return summarizedWindowId;
    }

    public void setSummarizedWindowId(UUIDFilter summarizedWindowId) {
        this.summarizedWindowId = summarizedWindowId;
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
        final NotificationCriteria that = (NotificationCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(notificationType, that.notificationType) &&
            Objects.equals(urgency, that.urgency) &&
            Objects.equals(subject, that.subject) &&
            Objects.equals(objectType, that.objectType) &&
            Objects.equals(objectId, that.objectId) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(sourceEventId, that.sourceEventId) &&
            Objects.equals(sourceService, that.sourceService) &&
            Objects.equals(urgentPort, that.urgentPort) &&
            Objects.equals(correlationId, that.correlationId) &&
            Objects.equals(groupKey, that.groupKey) &&
            Objects.equals(requestedAt, that.requestedAt) &&
            Objects.equals(ackDeadline, that.ackDeadline) &&
            Objects.equals(escalationLevel, that.escalationLevel) &&
            Objects.equals(state, that.state) &&
            Objects.equals(acknowledgedBy, that.acknowledgedBy) &&
            Objects.equals(acknowledgedAt, that.acknowledgedAt) &&
            Objects.equals(summary, that.summary) &&
            Objects.equals(occurrenceCount, that.occurrenceCount) &&
            Objects.equals(consolidationWindowId, that.consolidationWindowId) &&
            Objects.equals(summarizedWindowId, that.summarizedWindowId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            notificationType,
            urgency,
            subject,
            objectType,
            objectId,
            institutionId,
            sourceEventId,
            sourceService,
            urgentPort,
            correlationId,
            groupKey,
            requestedAt,
            ackDeadline,
            escalationLevel,
            state,
            acknowledgedBy,
            acknowledgedAt,
            summary,
            occurrenceCount,
            consolidationWindowId,
            summarizedWindowId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NotificationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalNotificationType().map(f -> "notificationType=" + f + ", ").orElse("") +
            optionalUrgency().map(f -> "urgency=" + f + ", ").orElse("") +
            optionalSubject().map(f -> "subject=" + f + ", ").orElse("") +
            optionalObjectType().map(f -> "objectType=" + f + ", ").orElse("") +
            optionalObjectId().map(f -> "objectId=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalSourceEventId().map(f -> "sourceEventId=" + f + ", ").orElse("") +
            optionalSourceService().map(f -> "sourceService=" + f + ", ").orElse("") +
            optionalUrgentPort().map(f -> "urgentPort=" + f + ", ").orElse("") +
            optionalCorrelationId().map(f -> "correlationId=" + f + ", ").orElse("") +
            optionalGroupKey().map(f -> "groupKey=" + f + ", ").orElse("") +
            optionalRequestedAt().map(f -> "requestedAt=" + f + ", ").orElse("") +
            optionalAckDeadline().map(f -> "ackDeadline=" + f + ", ").orElse("") +
            optionalEscalationLevel().map(f -> "escalationLevel=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalAcknowledgedBy().map(f -> "acknowledgedBy=" + f + ", ").orElse("") +
            optionalAcknowledgedAt().map(f -> "acknowledgedAt=" + f + ", ").orElse("") +
            optionalSummary().map(f -> "summary=" + f + ", ").orElse("") +
            optionalOccurrenceCount().map(f -> "occurrenceCount=" + f + ", ").orElse("") +
            optionalConsolidationWindowId().map(f -> "consolidationWindowId=" + f + ", ").orElse("") +
            optionalSummarizedWindowId().map(f -> "summarizedWindowId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
