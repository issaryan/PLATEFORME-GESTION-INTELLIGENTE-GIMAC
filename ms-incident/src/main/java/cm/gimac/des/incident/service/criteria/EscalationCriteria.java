package cm.gimac.des.incident.service.criteria;

import cm.gimac.des.incident.domain.enumeration.EscalationScope;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.incident.domain.Escalation} entity. This class is used
 * in {@link cm.gimac.des.incident.web.rest.EscalationResource} to receive all the possible filtering options from
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
     * Class for filtering EscalationScope
     */
    public static class EscalationScopeFilter extends Filter<EscalationScope> {

        public EscalationScopeFilter() {}

        public EscalationScopeFilter(EscalationScopeFilter filter) {
            super(filter);
        }

        @Override
        public EscalationScopeFilter copy() {
            return new EscalationScopeFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private EscalationScopeFilter scope;

    private IntegerFilter level;

    private UUIDFilter targetUserId;

    private UUIDFilter targetContactId;

    private InstantFilter triggeredAt;

    private StringFilter reason;

    private DurationFilter imputableDelay;

    private UUIDFilter ticketId;

    private Boolean distinct;

    public EscalationCriteria() {}

    public EscalationCriteria(EscalationCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.scope = other.optionalScope().map(EscalationScopeFilter::copy).orElse(null);
        this.level = other.optionalLevel().map(IntegerFilter::copy).orElse(null);
        this.targetUserId = other.optionalTargetUserId().map(UUIDFilter::copy).orElse(null);
        this.targetContactId = other.optionalTargetContactId().map(UUIDFilter::copy).orElse(null);
        this.triggeredAt = other.optionalTriggeredAt().map(InstantFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.imputableDelay = other.optionalImputableDelay().map(DurationFilter::copy).orElse(null);
        this.ticketId = other.optionalTicketId().map(UUIDFilter::copy).orElse(null);
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

    public EscalationScopeFilter getScope() {
        return scope;
    }

    public Optional<EscalationScopeFilter> optionalScope() {
        return Optional.ofNullable(scope);
    }

    public EscalationScopeFilter scope() {
        if (scope == null) {
            setScope(new EscalationScopeFilter());
        }
        return scope;
    }

    public void setScope(EscalationScopeFilter scope) {
        this.scope = scope;
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

    public UUIDFilter getTargetUserId() {
        return targetUserId;
    }

    public Optional<UUIDFilter> optionalTargetUserId() {
        return Optional.ofNullable(targetUserId);
    }

    public UUIDFilter targetUserId() {
        if (targetUserId == null) {
            setTargetUserId(new UUIDFilter());
        }
        return targetUserId;
    }

    public void setTargetUserId(UUIDFilter targetUserId) {
        this.targetUserId = targetUserId;
    }

    public UUIDFilter getTargetContactId() {
        return targetContactId;
    }

    public Optional<UUIDFilter> optionalTargetContactId() {
        return Optional.ofNullable(targetContactId);
    }

    public UUIDFilter targetContactId() {
        if (targetContactId == null) {
            setTargetContactId(new UUIDFilter());
        }
        return targetContactId;
    }

    public void setTargetContactId(UUIDFilter targetContactId) {
        this.targetContactId = targetContactId;
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

    public DurationFilter getImputableDelay() {
        return imputableDelay;
    }

    public Optional<DurationFilter> optionalImputableDelay() {
        return Optional.ofNullable(imputableDelay);
    }

    public DurationFilter imputableDelay() {
        if (imputableDelay == null) {
            setImputableDelay(new DurationFilter());
        }
        return imputableDelay;
    }

    public void setImputableDelay(DurationFilter imputableDelay) {
        this.imputableDelay = imputableDelay;
    }

    public UUIDFilter getTicketId() {
        return ticketId;
    }

    public Optional<UUIDFilter> optionalTicketId() {
        return Optional.ofNullable(ticketId);
    }

    public UUIDFilter ticketId() {
        if (ticketId == null) {
            setTicketId(new UUIDFilter());
        }
        return ticketId;
    }

    public void setTicketId(UUIDFilter ticketId) {
        this.ticketId = ticketId;
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
            Objects.equals(scope, that.scope) &&
            Objects.equals(level, that.level) &&
            Objects.equals(targetUserId, that.targetUserId) &&
            Objects.equals(targetContactId, that.targetContactId) &&
            Objects.equals(triggeredAt, that.triggeredAt) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(imputableDelay, that.imputableDelay) &&
            Objects.equals(ticketId, that.ticketId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, scope, level, targetUserId, targetContactId, triggeredAt, reason, imputableDelay, ticketId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EscalationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalScope().map(f -> "scope=" + f + ", ").orElse("") +
            optionalLevel().map(f -> "level=" + f + ", ").orElse("") +
            optionalTargetUserId().map(f -> "targetUserId=" + f + ", ").orElse("") +
            optionalTargetContactId().map(f -> "targetContactId=" + f + ", ").orElse("") +
            optionalTriggeredAt().map(f -> "triggeredAt=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalImputableDelay().map(f -> "imputableDelay=" + f + ", ").orElse("") +
            optionalTicketId().map(f -> "ticketId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
