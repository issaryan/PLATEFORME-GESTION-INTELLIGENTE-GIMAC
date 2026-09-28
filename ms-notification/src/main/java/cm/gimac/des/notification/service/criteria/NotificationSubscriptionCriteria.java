package cm.gimac.des.notification.service.criteria;

import cm.gimac.des.notification.domain.enumeration.RuleType;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.notification.domain.NotificationSubscription} entity. This class is used
 * in {@link cm.gimac.des.notification.web.rest.NotificationSubscriptionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /notification-subscriptions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationSubscriptionCriteria implements Serializable, Criteria {

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

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter notificationType;

    private RuleTypeFilter ruleType;

    private StringFilter ruleValue;

    private UrgencyLevelFilter minimumUrgency;

    private BooleanFilter onDutyOnly;

    private BooleanFilter active;

    private IntegerFilter ruleVersion;

    private UUIDFilter updatedBy;

    private InstantFilter updatedAt;

    private Boolean distinct;

    public NotificationSubscriptionCriteria() {}

    public NotificationSubscriptionCriteria(NotificationSubscriptionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.notificationType = other.optionalNotificationType().map(StringFilter::copy).orElse(null);
        this.ruleType = other.optionalRuleType().map(RuleTypeFilter::copy).orElse(null);
        this.ruleValue = other.optionalRuleValue().map(StringFilter::copy).orElse(null);
        this.minimumUrgency = other.optionalMinimumUrgency().map(UrgencyLevelFilter::copy).orElse(null);
        this.onDutyOnly = other.optionalOnDutyOnly().map(BooleanFilter::copy).orElse(null);
        this.active = other.optionalActive().map(BooleanFilter::copy).orElse(null);
        this.ruleVersion = other.optionalRuleVersion().map(IntegerFilter::copy).orElse(null);
        this.updatedBy = other.optionalUpdatedBy().map(UUIDFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public NotificationSubscriptionCriteria copy() {
        return new NotificationSubscriptionCriteria(this);
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

    public RuleTypeFilter getRuleType() {
        return ruleType;
    }

    public Optional<RuleTypeFilter> optionalRuleType() {
        return Optional.ofNullable(ruleType);
    }

    public RuleTypeFilter ruleType() {
        if (ruleType == null) {
            setRuleType(new RuleTypeFilter());
        }
        return ruleType;
    }

    public void setRuleType(RuleTypeFilter ruleType) {
        this.ruleType = ruleType;
    }

    public StringFilter getRuleValue() {
        return ruleValue;
    }

    public Optional<StringFilter> optionalRuleValue() {
        return Optional.ofNullable(ruleValue);
    }

    public StringFilter ruleValue() {
        if (ruleValue == null) {
            setRuleValue(new StringFilter());
        }
        return ruleValue;
    }

    public void setRuleValue(StringFilter ruleValue) {
        this.ruleValue = ruleValue;
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

    public BooleanFilter getOnDutyOnly() {
        return onDutyOnly;
    }

    public Optional<BooleanFilter> optionalOnDutyOnly() {
        return Optional.ofNullable(onDutyOnly);
    }

    public BooleanFilter onDutyOnly() {
        if (onDutyOnly == null) {
            setOnDutyOnly(new BooleanFilter());
        }
        return onDutyOnly;
    }

    public void setOnDutyOnly(BooleanFilter onDutyOnly) {
        this.onDutyOnly = onDutyOnly;
    }

    public BooleanFilter getActive() {
        return active;
    }

    public Optional<BooleanFilter> optionalActive() {
        return Optional.ofNullable(active);
    }

    public BooleanFilter active() {
        if (active == null) {
            setActive(new BooleanFilter());
        }
        return active;
    }

    public void setActive(BooleanFilter active) {
        this.active = active;
    }

    public IntegerFilter getRuleVersion() {
        return ruleVersion;
    }

    public Optional<IntegerFilter> optionalRuleVersion() {
        return Optional.ofNullable(ruleVersion);
    }

    public IntegerFilter ruleVersion() {
        if (ruleVersion == null) {
            setRuleVersion(new IntegerFilter());
        }
        return ruleVersion;
    }

    public void setRuleVersion(IntegerFilter ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public UUIDFilter getUpdatedBy() {
        return updatedBy;
    }

    public Optional<UUIDFilter> optionalUpdatedBy() {
        return Optional.ofNullable(updatedBy);
    }

    public UUIDFilter updatedBy() {
        if (updatedBy == null) {
            setUpdatedBy(new UUIDFilter());
        }
        return updatedBy;
    }

    public void setUpdatedBy(UUIDFilter updatedBy) {
        this.updatedBy = updatedBy;
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
        final NotificationSubscriptionCriteria that = (NotificationSubscriptionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(notificationType, that.notificationType) &&
            Objects.equals(ruleType, that.ruleType) &&
            Objects.equals(ruleValue, that.ruleValue) &&
            Objects.equals(minimumUrgency, that.minimumUrgency) &&
            Objects.equals(onDutyOnly, that.onDutyOnly) &&
            Objects.equals(active, that.active) &&
            Objects.equals(ruleVersion, that.ruleVersion) &&
            Objects.equals(updatedBy, that.updatedBy) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            notificationType,
            ruleType,
            ruleValue,
            minimumUrgency,
            onDutyOnly,
            active,
            ruleVersion,
            updatedBy,
            updatedAt,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NotificationSubscriptionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalNotificationType().map(f -> "notificationType=" + f + ", ").orElse("") +
            optionalRuleType().map(f -> "ruleType=" + f + ", ").orElse("") +
            optionalRuleValue().map(f -> "ruleValue=" + f + ", ").orElse("") +
            optionalMinimumUrgency().map(f -> "minimumUrgency=" + f + ", ").orElse("") +
            optionalOnDutyOnly().map(f -> "onDutyOnly=" + f + ", ").orElse("") +
            optionalActive().map(f -> "active=" + f + ", ").orElse("") +
            optionalRuleVersion().map(f -> "ruleVersion=" + f + ", ").orElse("") +
            optionalUpdatedBy().map(f -> "updatedBy=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
