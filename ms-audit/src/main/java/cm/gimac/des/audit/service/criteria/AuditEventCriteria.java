package cm.gimac.des.audit.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.audit.domain.AuditEvent} entity. This class is used
 * in {@link cm.gimac.des.audit.web.rest.AuditEventResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /audit-events?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AuditEventCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private LongFilter logIndex;

    private UUIDFilter eventId;

    private InstantFilter occurredAt;

    private InstantFilter recordedAt;

    private StringFilter actorId;

    private StringFilter actionType;

    private StringFilter domain;

    private StringFilter subject;

    private UUIDFilter correlationId;

    private StringFilter leafHash;

    private Boolean distinct;

    public AuditEventCriteria() {}

    public AuditEventCriteria(AuditEventCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.logIndex = other.optionalLogIndex().map(LongFilter::copy).orElse(null);
        this.eventId = other.optionalEventId().map(UUIDFilter::copy).orElse(null);
        this.occurredAt = other.optionalOccurredAt().map(InstantFilter::copy).orElse(null);
        this.recordedAt = other.optionalRecordedAt().map(InstantFilter::copy).orElse(null);
        this.actorId = other.optionalActorId().map(StringFilter::copy).orElse(null);
        this.actionType = other.optionalActionType().map(StringFilter::copy).orElse(null);
        this.domain = other.optionalDomain().map(StringFilter::copy).orElse(null);
        this.subject = other.optionalSubject().map(StringFilter::copy).orElse(null);
        this.correlationId = other.optionalCorrelationId().map(UUIDFilter::copy).orElse(null);
        this.leafHash = other.optionalLeafHash().map(StringFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public AuditEventCriteria copy() {
        return new AuditEventCriteria(this);
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

    public LongFilter getLogIndex() {
        return logIndex;
    }

    public Optional<LongFilter> optionalLogIndex() {
        return Optional.ofNullable(logIndex);
    }

    public LongFilter logIndex() {
        if (logIndex == null) {
            setLogIndex(new LongFilter());
        }
        return logIndex;
    }

    public void setLogIndex(LongFilter logIndex) {
        this.logIndex = logIndex;
    }

    public UUIDFilter getEventId() {
        return eventId;
    }

    public Optional<UUIDFilter> optionalEventId() {
        return Optional.ofNullable(eventId);
    }

    public UUIDFilter eventId() {
        if (eventId == null) {
            setEventId(new UUIDFilter());
        }
        return eventId;
    }

    public void setEventId(UUIDFilter eventId) {
        this.eventId = eventId;
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

    public InstantFilter getRecordedAt() {
        return recordedAt;
    }

    public Optional<InstantFilter> optionalRecordedAt() {
        return Optional.ofNullable(recordedAt);
    }

    public InstantFilter recordedAt() {
        if (recordedAt == null) {
            setRecordedAt(new InstantFilter());
        }
        return recordedAt;
    }

    public void setRecordedAt(InstantFilter recordedAt) {
        this.recordedAt = recordedAt;
    }

    public StringFilter getActorId() {
        return actorId;
    }

    public Optional<StringFilter> optionalActorId() {
        return Optional.ofNullable(actorId);
    }

    public StringFilter actorId() {
        if (actorId == null) {
            setActorId(new StringFilter());
        }
        return actorId;
    }

    public void setActorId(StringFilter actorId) {
        this.actorId = actorId;
    }

    public StringFilter getActionType() {
        return actionType;
    }

    public Optional<StringFilter> optionalActionType() {
        return Optional.ofNullable(actionType);
    }

    public StringFilter actionType() {
        if (actionType == null) {
            setActionType(new StringFilter());
        }
        return actionType;
    }

    public void setActionType(StringFilter actionType) {
        this.actionType = actionType;
    }

    public StringFilter getDomain() {
        return domain;
    }

    public Optional<StringFilter> optionalDomain() {
        return Optional.ofNullable(domain);
    }

    public StringFilter domain() {
        if (domain == null) {
            setDomain(new StringFilter());
        }
        return domain;
    }

    public void setDomain(StringFilter domain) {
        this.domain = domain;
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

    public StringFilter getLeafHash() {
        return leafHash;
    }

    public Optional<StringFilter> optionalLeafHash() {
        return Optional.ofNullable(leafHash);
    }

    public StringFilter leafHash() {
        if (leafHash == null) {
            setLeafHash(new StringFilter());
        }
        return leafHash;
    }

    public void setLeafHash(StringFilter leafHash) {
        this.leafHash = leafHash;
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
        final AuditEventCriteria that = (AuditEventCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(logIndex, that.logIndex) &&
            Objects.equals(eventId, that.eventId) &&
            Objects.equals(occurredAt, that.occurredAt) &&
            Objects.equals(recordedAt, that.recordedAt) &&
            Objects.equals(actorId, that.actorId) &&
            Objects.equals(actionType, that.actionType) &&
            Objects.equals(domain, that.domain) &&
            Objects.equals(subject, that.subject) &&
            Objects.equals(correlationId, that.correlationId) &&
            Objects.equals(leafHash, that.leafHash) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            logIndex,
            eventId,
            occurredAt,
            recordedAt,
            actorId,
            actionType,
            domain,
            subject,
            correlationId,
            leafHash,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AuditEventCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalLogIndex().map(f -> "logIndex=" + f + ", ").orElse("") +
            optionalEventId().map(f -> "eventId=" + f + ", ").orElse("") +
            optionalOccurredAt().map(f -> "occurredAt=" + f + ", ").orElse("") +
            optionalRecordedAt().map(f -> "recordedAt=" + f + ", ").orElse("") +
            optionalActorId().map(f -> "actorId=" + f + ", ").orElse("") +
            optionalActionType().map(f -> "actionType=" + f + ", ").orElse("") +
            optionalDomain().map(f -> "domain=" + f + ", ").orElse("") +
            optionalSubject().map(f -> "subject=" + f + ", ").orElse("") +
            optionalCorrelationId().map(f -> "correlationId=" + f + ", ").orElse("") +
            optionalLeafHash().map(f -> "leafHash=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
