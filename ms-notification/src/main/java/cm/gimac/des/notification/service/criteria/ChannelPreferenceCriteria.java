package cm.gimac.des.notification.service.criteria;

import cm.gimac.des.notification.domain.enumeration.Channel;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.notification.domain.ChannelPreference} entity. This class is used
 * in {@link cm.gimac.des.notification.web.rest.ChannelPreferenceResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /channel-preferences?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChannelPreferenceCriteria implements Serializable, Criteria {

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
     * Class for filtering LocalTime
     */
    public static class LocalTimeFilter extends RangeFilter<LocalTime> {

        public LocalTimeFilter() {}

        public LocalTimeFilter(LocalTimeFilter filter) {
            super(filter);
        }

        @Override
        public LocalTimeFilter copy() {
            return new LocalTimeFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter userId;

    private ChannelFilter channel;

    private UrgencyLevelFilter minimumUrgency;

    private LocalTimeFilter windowStart;

    private LocalTimeFilter windowEnd;

    private BooleanFilter enabled;

    private Boolean distinct;

    public ChannelPreferenceCriteria() {}

    public ChannelPreferenceCriteria(ChannelPreferenceCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.userId = other.optionalUserId().map(UUIDFilter::copy).orElse(null);
        this.channel = other.optionalChannel().map(ChannelFilter::copy).orElse(null);
        this.minimumUrgency = other.optionalMinimumUrgency().map(UrgencyLevelFilter::copy).orElse(null);
        this.windowStart = other.optionalWindowStart().map(LocalTimeFilter::copy).orElse(null);
        this.windowEnd = other.optionalWindowEnd().map(LocalTimeFilter::copy).orElse(null);
        this.enabled = other.optionalEnabled().map(BooleanFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ChannelPreferenceCriteria copy() {
        return new ChannelPreferenceCriteria(this);
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

    public UrgencyLevelFilter getMinimumUrgency() {
        return minimumUrgency;
    }

    public Optional<UrgencyLevelFilter> optionalMinimumUrgency() {
        return Optional.ofNullable(minimumUrgency);
    }

    public UrgencyLevelFilter minimumUrgency() {
        if (minimumUrgency == null) {
            setMinimumUrgency(new UrgencyLevelFilter());
        }
        return minimumUrgency;
    }

    public void setMinimumUrgency(UrgencyLevelFilter minimumUrgency) {
        this.minimumUrgency = minimumUrgency;
    }

    public LocalTimeFilter getWindowStart() {
        return windowStart;
    }

    public Optional<LocalTimeFilter> optionalWindowStart() {
        return Optional.ofNullable(windowStart);
    }

    public LocalTimeFilter windowStart() {
        if (windowStart == null) {
            setWindowStart(new LocalTimeFilter());
        }
        return windowStart;
    }

    public void setWindowStart(LocalTimeFilter windowStart) {
        this.windowStart = windowStart;
    }

    public LocalTimeFilter getWindowEnd() {
        return windowEnd;
    }

    public Optional<LocalTimeFilter> optionalWindowEnd() {
        return Optional.ofNullable(windowEnd);
    }

    public LocalTimeFilter windowEnd() {
        if (windowEnd == null) {
            setWindowEnd(new LocalTimeFilter());
        }
        return windowEnd;
    }

    public void setWindowEnd(LocalTimeFilter windowEnd) {
        this.windowEnd = windowEnd;
    }

    public BooleanFilter getEnabled() {
        return enabled;
    }

    public Optional<BooleanFilter> optionalEnabled() {
        return Optional.ofNullable(enabled);
    }

    public BooleanFilter enabled() {
        if (enabled == null) {
            setEnabled(new BooleanFilter());
        }
        return enabled;
    }

    public void setEnabled(BooleanFilter enabled) {
        this.enabled = enabled;
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
        final ChannelPreferenceCriteria that = (ChannelPreferenceCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(channel, that.channel) &&
            Objects.equals(minimumUrgency, that.minimumUrgency) &&
            Objects.equals(windowStart, that.windowStart) &&
            Objects.equals(windowEnd, that.windowEnd) &&
            Objects.equals(enabled, that.enabled) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, channel, minimumUrgency, windowStart, windowEnd, enabled, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChannelPreferenceCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalUserId().map(f -> "userId=" + f + ", ").orElse("") +
            optionalChannel().map(f -> "channel=" + f + ", ").orElse("") +
            optionalMinimumUrgency().map(f -> "minimumUrgency=" + f + ", ").orElse("") +
            optionalWindowStart().map(f -> "windowStart=" + f + ", ").orElse("") +
            optionalWindowEnd().map(f -> "windowEnd=" + f + ", ").orElse("") +
            optionalEnabled().map(f -> "enabled=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
