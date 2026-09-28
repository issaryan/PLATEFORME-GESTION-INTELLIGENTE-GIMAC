package cm.gimac.des.notification.service.criteria;

import cm.gimac.des.notification.domain.enumeration.EscalationSource;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.notification.domain.Escalation} entity. This class is used
 * in {@link cm.gimac.des.notification.web.rest.EscalationResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /escalations?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EscalationCriteria implements Serializable, Criteria {

    /**
     * Class for filtering EscalationSource
     */
    public static class EscalationSourceFilter extends Filter<EscalationSource> {

        public EscalationSourceFilter() {}

        public EscalationSourceFilter(EscalationSourceFilter filter) {
            super(filter);
        }

        @Override
        public EscalationSourceFilter copy() {
            return new EscalationSourceFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter fromUserId;

    private UUIDFilter toUserId;

    private IntegerFilter level;

    private EscalationSourceFilter source;

    private InstantFilter triggeredAt;

    private StringFilter reason;

    private UUIDFilter notificationId;

    private Boolean distinct;

    public EscalationCriteria() {}

    public EscalationCriteria(EscalationCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.fromUserId = other.optionalFromUserId().map(UUIDFilter::copy).orElse(null);
        this.toUserId = other.optionalToUserId().map(UUIDFilter::copy).orElse(null);
        this.level = other.optionalLevel().map(IntegerFilter::copy).orElse(null);
        this.source = other.optionalSource().map(EscalationSourceFilter::copy).orElse(null);
        this.triggeredAt = other.optionalTriggeredAt().map(InstantFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.notificationId = other.optionalNotificationId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public EscalationCriteria copy() {
        return new EscalationCriteria(this);
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

    public UUIDFilter getFromUserId() {
        return fromUserId;
    }

    public Optional<UUIDFilter> optionalFromUserId() {
        return Optional.ofNullable(fromUserId);
    }

    public UUIDFilter fromUserId() {
        if (fromUserId == null) {
            setFromUserId(new UUIDFilter());
        }
        return fromUserId;
    }

    public void setFromUserId(UUIDFilter fromUserId) {
        this.fromUserId = fromUserId;
    }

    public UUIDFilter getToUserId() {
        return toUserId;
    }

    public Optional<UUIDFilter> optionalToUserId() {
        return Optional.ofNullable(toUserId);
    }

    public UUIDFilter toUserId() {
        if (toUserId == null) {
            setToUserId(new UUIDFilter());
        }
        return toUserId;
    }

    public void setToUserId(UUIDFilter toUserId) {
        this.toUserId = toUserId;
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

    public EscalationSourceFilter getSource() {
        return source;
    }

    public Optional<EscalationSourceFilter> optionalSource() {
        return Optional.ofNullable(source);
    }

    public EscalationSourceFilter source() {
        if (source == null) {
            setSource(new EscalationSourceFilter());
        }
        return source;
    }

    public void setSource(EscalationSourceFilter source) {
        this.source = source;
    }

    public InstantFilter getTriggeredAt() {
        return triggeredAt;
    }

    public Optional<InstantFilter> optionalTriggeredAt() {
        return Optional.ofNullable(triggeredAt);
    }

    public InstantFilter triggeredAt() {
        if (triggeredAt == null) {
            setTriggeredAt(new InstantFilter());
        }
        return triggeredAt;
    }

    public void setTriggeredAt(InstantFilter triggeredAt) {
        this.triggeredAt = triggeredAt;
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
        final EscalationCriteria that = (EscalationCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(fromUserId, that.fromUserId) &&
            Objects.equals(toUserId, that.toUserId) &&
            Objects.equals(level, that.level) &&
            Objects.equals(source, that.source) &&
            Objects.equals(triggeredAt, that.triggeredAt) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(notificationId, that.notificationId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fromUserId, toUserId, level, source, triggeredAt, reason, notificationId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EscalationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalFromUserId().map(f -> "fromUserId=" + f + ", ").orElse("") +
            optionalToUserId().map(f -> "toUserId=" + f + ", ").orElse("") +
            optionalLevel().map(f -> "level=" + f + ", ").orElse("") +
            optionalSource().map(f -> "source=" + f + ", ").orElse("") +
            optionalTriggeredAt().map(f -> "triggeredAt=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalNotificationId().map(f -> "notificationId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
