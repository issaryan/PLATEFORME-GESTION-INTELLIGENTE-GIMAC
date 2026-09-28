package cm.gimac.des.notification.service.criteria;

import cm.gimac.des.notification.domain.enumeration.Channel;
import cm.gimac.des.notification.domain.enumeration.DeliveryState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.notification.domain.DeliveryTrace} entity. This class is used
 * in {@link cm.gimac.des.notification.web.rest.DeliveryTraceResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /delivery-traces?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeliveryTraceCriteria implements Serializable, Criteria {

    /**
     * Class for filtering Channel
     */
    public static class ChannelFilter extends Filter<Channel> {

        public ChannelFilter() {}

        public ChannelFilter(ChannelFilter filter) {
            super(filter);
        }

        @Override
        public ChannelFilter copy() {
            return new ChannelFilter(this);
        }
    }

    /**
     * Class for filtering DeliveryState
     */
    public static class DeliveryStateFilter extends Filter<DeliveryState> {

        public DeliveryStateFilter() {}

        public DeliveryStateFilter(DeliveryStateFilter filter) {
            super(filter);
        }

        @Override
        public DeliveryStateFilter copy() {
            return new DeliveryStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter userId;

    private ChannelFilter channel;

    private DeliveryStateFilter state;

    private IntegerFilter attempts;

    private InstantFilter nextAttemptAt;

    private StringFilter lastError;

    private StringFilter providerMessageId;

    private InstantFilter deliveredAt;

    private InstantFilter acknowledgedAt;

    private UUIDFilter notificationId;

    private UUIDFilter templateId;

    private Boolean distinct;

    public DeliveryTraceCriteria() {}

    public DeliveryTraceCriteria(DeliveryTraceCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.userId = other.optionalUserId().map(UUIDFilter::copy).orElse(null);
        this.channel = other.optionalChannel().map(ChannelFilter::copy).orElse(null);
        this.state = other.optionalState().map(DeliveryStateFilter::copy).orElse(null);
        this.attempts = other.optionalAttempts().map(IntegerFilter::copy).orElse(null);
        this.nextAttemptAt = other.optionalNextAttemptAt().map(InstantFilter::copy).orElse(null);
        this.lastError = other.optionalLastError().map(StringFilter::copy).orElse(null);
        this.providerMessageId = other.optionalProviderMessageId().map(StringFilter::copy).orElse(null);
        this.deliveredAt = other.optionalDeliveredAt().map(InstantFilter::copy).orElse(null);
        this.acknowledgedAt = other.optionalAcknowledgedAt().map(InstantFilter::copy).orElse(null);
        this.notificationId = other.optionalNotificationId().map(UUIDFilter::copy).orElse(null);
        this.templateId = other.optionalTemplateId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DeliveryTraceCriteria copy() {
        return new DeliveryTraceCriteria(this);
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

    public ChannelFilter getChannel() {
        return channel;
    }

    public Optional<ChannelFilter> optionalChannel() {
        return Optional.ofNullable(channel);
    }

    public ChannelFilter channel() {
        if (channel == null) {
            setChannel(new ChannelFilter());
        }
        return channel;
    }

    public void setChannel(ChannelFilter channel) {
        this.channel = channel;
    }

    public DeliveryStateFilter getState() {
        return state;
    }

    public Optional<DeliveryStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public DeliveryStateFilter state() {
        if (state == null) {
            setState(new DeliveryStateFilter());
        }
        return state;
    }

    public void setState(DeliveryStateFilter state) {
        this.state = state;
    }

    public IntegerFilter getAttempts() {
        return attempts;
    }

    public Optional<IntegerFilter> optionalAttempts() {
        return Optional.ofNullable(attempts);
    }

    public IntegerFilter attempts() {
        if (attempts == null) {
            setAttempts(new IntegerFilter());
        }
        return attempts;
    }

    public void setAttempts(IntegerFilter attempts) {
        this.attempts = attempts;
    }

    public InstantFilter getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Optional<InstantFilter> optionalNextAttemptAt() {
        return Optional.ofNullable(nextAttemptAt);
    }

    public InstantFilter nextAttemptAt() {
        if (nextAttemptAt == null) {
            setNextAttemptAt(new InstantFilter());
        }
        return nextAttemptAt;
    }

    public void setNextAttemptAt(InstantFilter nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public StringFilter getLastError() {
        return lastError;
    }

    public Optional<StringFilter> optionalLastError() {
        return Optional.ofNullable(lastError);
    }

    public StringFilter lastError() {
        if (lastError == null) {
            setLastError(new StringFilter());
        }
        return lastError;
    }

    public void setLastError(StringFilter lastError) {
        this.lastError = lastError;
    }

    public StringFilter getProviderMessageId() {
        return providerMessageId;
    }

    public Optional<StringFilter> optionalProviderMessageId() {
        return Optional.ofNullable(providerMessageId);
    }

    public StringFilter providerMessageId() {
        if (providerMessageId == null) {
            setProviderMessageId(new StringFilter());
        }
        return providerMessageId;
    }

    public void setProviderMessageId(StringFilter providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public InstantFilter getDeliveredAt() {
        return deliveredAt;
    }

    public Optional<InstantFilter> optionalDeliveredAt() {
        return Optional.ofNullable(deliveredAt);
    }

    public InstantFilter deliveredAt() {
        if (deliveredAt == null) {
            setDeliveredAt(new InstantFilter());
        }
        return deliveredAt;
    }

    public void setDeliveredAt(InstantFilter deliveredAt) {
        this.deliveredAt = deliveredAt;
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

    public UUIDFilter getTemplateId() {
        return templateId;
    }

    public Optional<UUIDFilter> optionalTemplateId() {
        return Optional.ofNullable(templateId);
    }

    public UUIDFilter templateId() {
        if (templateId == null) {
            setTemplateId(new UUIDFilter());
        }
        return templateId;
    }

    public void setTemplateId(UUIDFilter templateId) {
        this.templateId = templateId;
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
        final DeliveryTraceCriteria that = (DeliveryTraceCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(channel, that.channel) &&
            Objects.equals(state, that.state) &&
            Objects.equals(attempts, that.attempts) &&
            Objects.equals(nextAttemptAt, that.nextAttemptAt) &&
            Objects.equals(lastError, that.lastError) &&
            Objects.equals(providerMessageId, that.providerMessageId) &&
            Objects.equals(deliveredAt, that.deliveredAt) &&
            Objects.equals(acknowledgedAt, that.acknowledgedAt) &&
            Objects.equals(notificationId, that.notificationId) &&
            Objects.equals(templateId, that.templateId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            userId,
            channel,
            state,
            attempts,
            nextAttemptAt,
            lastError,
            providerMessageId,
            deliveredAt,
            acknowledgedAt,
            notificationId,
            templateId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DeliveryTraceCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalUserId().map(f -> "userId=" + f + ", ").orElse("") +
            optionalChannel().map(f -> "channel=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalAttempts().map(f -> "attempts=" + f + ", ").orElse("") +
            optionalNextAttemptAt().map(f -> "nextAttemptAt=" + f + ", ").orElse("") +
            optionalLastError().map(f -> "lastError=" + f + ", ").orElse("") +
            optionalProviderMessageId().map(f -> "providerMessageId=" + f + ", ").orElse("") +
            optionalDeliveredAt().map(f -> "deliveredAt=" + f + ", ").orElse("") +
            optionalAcknowledgedAt().map(f -> "acknowledgedAt=" + f + ", ").orElse("") +
            optionalNotificationId().map(f -> "notificationId=" + f + ", ").orElse("") +
            optionalTemplateId().map(f -> "templateId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
