package cm.gimac.des.iam.service.criteria;

import cm.gimac.des.iam.domain.enumeration.DelegationState;
import cm.gimac.des.iam.domain.enumeration.DelegationType;
import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.iam.domain.Delegation} entity. This class is used
 * in {@link cm.gimac.des.iam.web.rest.DelegationResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /delegations?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DelegationCriteria implements Serializable, Criteria {

    /**
     * Class for filtering DelegationType
     */
    public static class DelegationTypeFilter extends Filter<DelegationType> {

        public DelegationTypeFilter() {}

        public DelegationTypeFilter(DelegationTypeFilter filter) {
            super(filter);
        }

        @Override
        public DelegationTypeFilter copy() {
            return new DelegationTypeFilter(this);
        }
    }

    /**
     * Class for filtering HabilitationLevel
     */
    public static class HabilitationLevelFilter extends Filter<HabilitationLevel> {

        public HabilitationLevelFilter() {}

        public HabilitationLevelFilter(HabilitationLevelFilter filter) {
            super(filter);
        }

        @Override
        public HabilitationLevelFilter copy() {
            return new HabilitationLevelFilter(this);
        }
    }

    /**
     * Class for filtering DelegationState
     */
    public static class DelegationStateFilter extends Filter<DelegationState> {

        public DelegationStateFilter() {}

        public DelegationStateFilter(DelegationStateFilter filter) {
            super(filter);
        }

        @Override
        public DelegationStateFilter copy() {
            return new DelegationStateFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private DelegationTypeFilter type;

    private StringFilter reason;

    private HabilitationLevelFilter delegatedLevel;

    private UUIDFilter requesterId;

    private UUIDFilter approverId;

    private InstantFilter startsAt;

    private InstantFilter endsAt;

    private DelegationStateFilter state;

    private UUIDFilter collaboratorId;

    private Boolean distinct;

    public DelegationCriteria() {}

    public DelegationCriteria(DelegationCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.type = other.optionalType().map(DelegationTypeFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.delegatedLevel = other.optionalDelegatedLevel().map(HabilitationLevelFilter::copy).orElse(null);
        this.requesterId = other.optionalRequesterId().map(UUIDFilter::copy).orElse(null);
        this.approverId = other.optionalApproverId().map(UUIDFilter::copy).orElse(null);
        this.startsAt = other.optionalStartsAt().map(InstantFilter::copy).orElse(null);
        this.endsAt = other.optionalEndsAt().map(InstantFilter::copy).orElse(null);
        this.state = other.optionalState().map(DelegationStateFilter::copy).orElse(null);
        this.collaboratorId = other.optionalCollaboratorId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DelegationCriteria copy() {
        return new DelegationCriteria(this);
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

    public DelegationTypeFilter getType() {
        return type;
    }

    public Optional<DelegationTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public DelegationTypeFilter type() {
        if (type == null) {
            setType(new DelegationTypeFilter());
        }
        return type;
    }

    public void setType(DelegationTypeFilter type) {
        this.type = type;
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

    public HabilitationLevelFilter getDelegatedLevel() {
        return delegatedLevel;
    }

    public Optional<HabilitationLevelFilter> optionalDelegatedLevel() {
        return Optional.ofNullable(delegatedLevel);
    }

    public HabilitationLevelFilter delegatedLevel() {
        if (delegatedLevel == null) {
            setDelegatedLevel(new HabilitationLevelFilter());
        }
        return delegatedLevel;
    }

    public void setDelegatedLevel(HabilitationLevelFilter delegatedLevel) {
        this.delegatedLevel = delegatedLevel;
    }

    public UUIDFilter getRequesterId() {
        return requesterId;
    }

    public Optional<UUIDFilter> optionalRequesterId() {
        return Optional.ofNullable(requesterId);
    }

    public UUIDFilter requesterId() {
        if (requesterId == null) {
            setRequesterId(new UUIDFilter());
        }
        return requesterId;
    }

    public void setRequesterId(UUIDFilter requesterId) {
        this.requesterId = requesterId;
    }

    public UUIDFilter getApproverId() {
        return approverId;
    }

    public Optional<UUIDFilter> optionalApproverId() {
        return Optional.ofNullable(approverId);
    }

    public UUIDFilter approverId() {
        if (approverId == null) {
            setApproverId(new UUIDFilter());
        }
        return approverId;
    }

    public void setApproverId(UUIDFilter approverId) {
        this.approverId = approverId;
    }

    public InstantFilter getStartsAt() {
        return startsAt;
    }

    public Optional<InstantFilter> optionalStartsAt() {
        return Optional.ofNullable(startsAt);
    }

    public InstantFilter startsAt() {
        if (startsAt == null) {
            setStartsAt(new InstantFilter());
        }
        return startsAt;
    }

    public void setStartsAt(InstantFilter startsAt) {
        this.startsAt = startsAt;
    }

    public InstantFilter getEndsAt() {
        return endsAt;
    }

    public Optional<InstantFilter> optionalEndsAt() {
        return Optional.ofNullable(endsAt);
    }

    public InstantFilter endsAt() {
        if (endsAt == null) {
            setEndsAt(new InstantFilter());
        }
        return endsAt;
    }

    public void setEndsAt(InstantFilter endsAt) {
        this.endsAt = endsAt;
    }

    public DelegationStateFilter getState() {
        return state;
    }

    public Optional<DelegationStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public DelegationStateFilter state() {
        if (state == null) {
            setState(new DelegationStateFilter());
        }
        return state;
    }

    public void setState(DelegationStateFilter state) {
        this.state = state;
    }

    public UUIDFilter getCollaboratorId() {
        return collaboratorId;
    }

    public Optional<UUIDFilter> optionalCollaboratorId() {
        return Optional.ofNullable(collaboratorId);
    }

    public UUIDFilter collaboratorId() {
        if (collaboratorId == null) {
            setCollaboratorId(new UUIDFilter());
        }
        return collaboratorId;
    }

    public void setCollaboratorId(UUIDFilter collaboratorId) {
        this.collaboratorId = collaboratorId;
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
        final DelegationCriteria that = (DelegationCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(type, that.type) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(delegatedLevel, that.delegatedLevel) &&
            Objects.equals(requesterId, that.requesterId) &&
            Objects.equals(approverId, that.approverId) &&
            Objects.equals(startsAt, that.startsAt) &&
            Objects.equals(endsAt, that.endsAt) &&
            Objects.equals(state, that.state) &&
            Objects.equals(collaboratorId, that.collaboratorId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, reason, delegatedLevel, requesterId, approverId, startsAt, endsAt, state, collaboratorId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DelegationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalDelegatedLevel().map(f -> "delegatedLevel=" + f + ", ").orElse("") +
            optionalRequesterId().map(f -> "requesterId=" + f + ", ").orElse("") +
            optionalApproverId().map(f -> "approverId=" + f + ", ").orElse("") +
            optionalStartsAt().map(f -> "startsAt=" + f + ", ").orElse("") +
            optionalEndsAt().map(f -> "endsAt=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalCollaboratorId().map(f -> "collaboratorId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
