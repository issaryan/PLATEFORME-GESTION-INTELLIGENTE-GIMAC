package cm.gimac.des.notification.service.criteria;

import cm.gimac.des.notification.domain.enumeration.RuleType;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.notification.domain.NotificationRecipient} entity. This class is used
 * in {@link cm.gimac.des.notification.web.rest.NotificationRecipientResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /notification-recipients?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationRecipientCriteria implements Serializable, Criteria {

    /**
     * Class for filtering RuleType
     */
    public static class RuleTypeFilter extends Filter<RuleType> {

        public RuleTypeFilter() {}

        public RuleTypeFilter(RuleTypeFilter filter) {
            super(filter);
        }

        @Override
        public RuleTypeFilter copy() {
            return new RuleTypeFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter userId;

    private UUIDFilter institutionId;

    private RuleTypeFilter resolvedBy;

    private StringFilter channels;

    private BooleanFilter escalated;

    private InstantFilter resolvedAt;

    private InstantFilter readAt;

    private InstantFilter acknowledgedAt;

    private UUIDFilter notificationId;

    private UUIDFilter sourceSubscriptionId;

    private Boolean distinct;

    public NotificationRecipientCriteria() {}

    public NotificationRecipientCriteria(NotificationRecipientCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.userId = other.optionalUserId().map(UUIDFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.resolvedBy = other.optionalResolvedBy().map(RuleTypeFilter::copy).orElse(null);
        this.channels = other.optionalChannels().map(StringFilter::copy).orElse(null);
        this.escalated = other.optionalEscalated().map(BooleanFilter::copy).orElse(null);
        this.resolvedAt = other.optionalResolvedAt().map(InstantFilter::copy).orElse(null);
        this.readAt = other.optionalReadAt().map(InstantFilter::copy).orElse(null);
        this.acknowledgedAt = other.optionalAcknowledgedAt().map(InstantFilter::copy).orElse(null);
        this.notificationId = other.optionalNotificationId().map(UUIDFilter::copy).orElse(null);
        this.sourceSubscriptionId = other.optionalSourceSubscriptionId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public NotificationRecipientCriteria copy() {
        return new NotificationRecipientCriteria(this);
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

    public RuleTypeFilter getResolvedBy() {
        return resolvedBy;
    }

    public Optional<RuleTypeFilter> optionalResolvedBy() {
        return Optional.ofNullable(resolvedBy);
    }

    public RuleTypeFilter resolvedBy() {
        if (resolvedBy == null) {
            setResolvedBy(new RuleTypeFilter());
        }
        return resolvedBy;
    }

    public void setResolvedBy(RuleTypeFilter resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public StringFilter getChannels() {
        return channels;
    }

    public Optional<StringFilter> optionalChannels() {
        return Optional.ofNullable(channels);
    }

    public StringFilter channels() {
        if (channels == null) {
            setChannels(new StringFilter());
        }
        return channels;
    }

    public void setChannels(StringFilter channels) {
        this.channels = channels;
    }

    public BooleanFilter getEscalated() {
        return escalated;
    }

    public Optional<BooleanFilter> optionalEscalated() {
        return Optional.ofNullable(escalated);
    }

    public BooleanFilter escalated() {
        if (escalated == null) {
            setEscalated(new BooleanFilter());
        }
        return escalated;
    }

    public void setEscalated(BooleanFilter escalated) {
        this.escalated = escalated;
    }

    public InstantFilter getResolvedAt() {
        return resolvedAt;
    }

    public Optional<InstantFilter> optionalResolvedAt() {
        return Optional.ofNullable(resolvedAt);
    }

    public InstantFilter resolvedAt() {
        if (resolvedAt == null) {
            setResolvedAt(new InstantFilter());
        }
        return resolvedAt;
    }

    public void setResolvedAt(InstantFilter resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public InstantFilter getReadAt() {
        return readAt;
    }

    public Optional<InstantFilter> optionalReadAt() {
        return Optional.ofNullable(readAt);
    }

    public InstantFilter readAt() {
        if (readAt == null) {
            setReadAt(new InstantFilter());
        }
        return readAt;
    }

    public void setReadAt(InstantFilter readAt) {
        this.readAt = readAt;
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

    public UUIDFilter getSourceSubscriptionId() {
        return sourceSubscriptionId;
    }

    public Optional<UUIDFilter> optionalSourceSubscriptionId() {
        return Optional.ofNullable(sourceSubscriptionId);
    }

    public UUIDFilter sourceSubscriptionId() {
        if (sourceSubscriptionId == null) {
            setSourceSubscriptionId(new UUIDFilter());
        }
        return sourceSubscriptionId;
    }

    public void setSourceSubscriptionId(UUIDFilter sourceSubscriptionId) {
        this.sourceSubscriptionId = sourceSubscriptionId;
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
        final NotificationRecipientCriteria that = (NotificationRecipientCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(resolvedBy, that.resolvedBy) &&
            Objects.equals(channels, that.channels) &&
            Objects.equals(escalated, that.escalated) &&
            Objects.equals(resolvedAt, that.resolvedAt) &&
            Objects.equals(readAt, that.readAt) &&
            Objects.equals(acknowledgedAt, that.acknowledgedAt) &&
            Objects.equals(notificationId, that.notificationId) &&
            Objects.equals(sourceSubscriptionId, that.sourceSubscriptionId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            userId,
            institutionId,
            resolvedBy,
            channels,
            escalated,
            resolvedAt,
            readAt,
            acknowledgedAt,
            notificationId,
            sourceSubscriptionId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NotificationRecipientCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalUserId().map(f -> "userId=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalResolvedBy().map(f -> "resolvedBy=" + f + ", ").orElse("") +
            optionalChannels().map(f -> "channels=" + f + ", ").orElse("") +
            optionalEscalated().map(f -> "escalated=" + f + ", ").orElse("") +
            optionalResolvedAt().map(f -> "resolvedAt=" + f + ", ").orElse("") +
            optionalReadAt().map(f -> "readAt=" + f + ", ").orElse("") +
            optionalAcknowledgedAt().map(f -> "acknowledgedAt=" + f + ", ").orElse("") +
            optionalNotificationId().map(f -> "notificationId=" + f + ", ").orElse("") +
            optionalSourceSubscriptionId().map(f -> "sourceSubscriptionId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
