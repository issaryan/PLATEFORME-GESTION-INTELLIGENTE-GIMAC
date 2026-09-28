package cm.gimac.des.incident.service.criteria;

import cm.gimac.des.incident.domain.enumeration.TicketOrigin;
import cm.gimac.des.incident.domain.enumeration.TicketState;
import cm.gimac.des.incident.domain.enumeration.TicketType;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.incident.domain.Ticket} entity. This class is used
 * in {@link cm.gimac.des.incident.web.rest.TicketResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /tickets?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TicketCriteria implements Serializable, Criteria {

    /**
     * Class for filtering TicketType
     */
    public static class TicketTypeFilter extends Filter<TicketType> {

        public TicketTypeFilter() {}

        public TicketTypeFilter(TicketTypeFilter filter) {
            super(filter);
        }

        @Override
        public TicketTypeFilter copy() {
            return new TicketTypeFilter(this);
        }
    }

    /**
     * Class for filtering TicketOrigin
     */
    public static class TicketOriginFilter extends Filter<TicketOrigin> {

        public TicketOriginFilter() {}

        public TicketOriginFilter(TicketOriginFilter filter) {
            super(filter);
        }

        @Override
        public TicketOriginFilter copy() {
            return new TicketOriginFilter(this);
        }
    }

    /**
     * Class for filtering TicketState
     */
    public static class TicketStateFilter extends Filter<TicketState> {

        public TicketStateFilter() {}

        public TicketStateFilter(TicketStateFilter filter) {
            super(filter);
        }

        @Override
        public TicketStateFilter copy() {
            return new TicketStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter ticketNumber;

    private TicketTypeFilter type;

    private StringFilter subject;

    private TicketOriginFilter origin;

    private StringFilter originRef;

    private UUIDFilter requesterId;

    private UUIDFilter institutionId;

    private StringFilter linkId;

    private UUIDFilter alertId;

    private IntegerFilter priority;

    private TicketStateFilter state;

    private UUIDFilter assigneeId;

    private InstantFilter openedAt;

    private InstantFilter resolvedAt;

    private UUIDFilter resolverId;

    private InstantFilter closedAt;

    private UUIDFilter closedBy;

    private LongFilter version;

    private UUIDFilter relatedTicketId;

    private UUIDFilter incidentId;

    private Boolean distinct;

    public TicketCriteria() {}

    public TicketCriteria(TicketCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.ticketNumber = other.optionalTicketNumber().map(StringFilter::copy).orElse(null);
        this.type = other.optionalType().map(TicketTypeFilter::copy).orElse(null);
        this.subject = other.optionalSubject().map(StringFilter::copy).orElse(null);
        this.origin = other.optionalOrigin().map(TicketOriginFilter::copy).orElse(null);
        this.originRef = other.optionalOriginRef().map(StringFilter::copy).orElse(null);
        this.requesterId = other.optionalRequesterId().map(UUIDFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.linkId = other.optionalLinkId().map(StringFilter::copy).orElse(null);
        this.alertId = other.optionalAlertId().map(UUIDFilter::copy).orElse(null);
        this.priority = other.optionalPriority().map(IntegerFilter::copy).orElse(null);
        this.state = other.optionalState().map(TicketStateFilter::copy).orElse(null);
        this.assigneeId = other.optionalAssigneeId().map(UUIDFilter::copy).orElse(null);
        this.openedAt = other.optionalOpenedAt().map(InstantFilter::copy).orElse(null);
        this.resolvedAt = other.optionalResolvedAt().map(InstantFilter::copy).orElse(null);
        this.resolverId = other.optionalResolverId().map(UUIDFilter::copy).orElse(null);
        this.closedAt = other.optionalClosedAt().map(InstantFilter::copy).orElse(null);
        this.closedBy = other.optionalClosedBy().map(UUIDFilter::copy).orElse(null);
        this.version = other.optionalVersion().map(LongFilter::copy).orElse(null);
        this.relatedTicketId = other.optionalRelatedTicketId().map(UUIDFilter::copy).orElse(null);
        this.incidentId = other.optionalIncidentId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public TicketCriteria copy() {
        return new TicketCriteria(this);
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

    public StringFilter getTicketNumber() {
        return ticketNumber;
    }

    public Optional<StringFilter> optionalTicketNumber() {
        return Optional.ofNullable(ticketNumber);
    }

    public StringFilter ticketNumber() {
        if (ticketNumber == null) {
            setTicketNumber(new StringFilter());
        }
        return ticketNumber;
    }

    public void setTicketNumber(StringFilter ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public TicketTypeFilter getType() {
        return type;
    }

    public Optional<TicketTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public TicketTypeFilter type() {
        if (type == null) {
            setType(new TicketTypeFilter());
        }
        return type;
    }

    public void setType(TicketTypeFilter type) {
        this.type = type;
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

    public TicketOriginFilter getOrigin() {
        return origin;
    }

    public Optional<TicketOriginFilter> optionalOrigin() {
        return Optional.ofNullable(origin);
    }

    public TicketOriginFilter origin() {
        if (origin == null) {
            setOrigin(new TicketOriginFilter());
        }
        return origin;
    }

    public void setOrigin(TicketOriginFilter origin) {
        this.origin = origin;
    }

    public StringFilter getOriginRef() {
        return originRef;
    }

    public Optional<StringFilter> optionalOriginRef() {
        return Optional.ofNullable(originRef);
    }

    public StringFilter originRef() {
        if (originRef == null) {
            setOriginRef(new StringFilter());
        }
        return originRef;
    }

    public void setOriginRef(StringFilter originRef) {
        this.originRef = originRef;
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

    public StringFilter getLinkId() {
        return linkId;
    }

    public Optional<StringFilter> optionalLinkId() {
        return Optional.ofNullable(linkId);
    }

    public StringFilter linkId() {
        if (linkId == null) {
            setLinkId(new StringFilter());
        }
        return linkId;
    }

    public void setLinkId(StringFilter linkId) {
        this.linkId = linkId;
    }

    public UUIDFilter getAlertId() {
        return alertId;
    }

    public Optional<UUIDFilter> optionalAlertId() {
        return Optional.ofNullable(alertId);
    }

    public UUIDFilter alertId() {
        if (alertId == null) {
            setAlertId(new UUIDFilter());
        }
        return alertId;
    }

    public void setAlertId(UUIDFilter alertId) {
        this.alertId = alertId;
    }

    public IntegerFilter getPriority() {
        return priority;
    }

    public Optional<IntegerFilter> optionalPriority() {
        return Optional.ofNullable(priority);
    }

    public IntegerFilter priority() {
        if (priority == null) {
            setPriority(new IntegerFilter());
        }
        return priority;
    }

    public void setPriority(IntegerFilter priority) {
        this.priority = priority;
    }

    public TicketStateFilter getState() {
        return state;
    }

    public Optional<TicketStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public TicketStateFilter state() {
        if (state == null) {
            setState(new TicketStateFilter());
        }
        return state;
    }

    public void setState(TicketStateFilter state) {
        this.state = state;
    }

    public UUIDFilter getAssigneeId() {
        return assigneeId;
    }

    public Optional<UUIDFilter> optionalAssigneeId() {
        return Optional.ofNullable(assigneeId);
    }

    public UUIDFilter assigneeId() {
        if (assigneeId == null) {
            setAssigneeId(new UUIDFilter());
        }
        return assigneeId;
    }

    public void setAssigneeId(UUIDFilter assigneeId) {
        this.assigneeId = assigneeId;
    }

    public InstantFilter getOpenedAt() {
        return openedAt;
    }

    public Optional<InstantFilter> optionalOpenedAt() {
        return Optional.ofNullable(openedAt);
    }

    public InstantFilter openedAt() {
        if (openedAt == null) {
            setOpenedAt(new InstantFilter());
        }
        return openedAt;
    }

    public void setOpenedAt(InstantFilter openedAt) {
        this.openedAt = openedAt;
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

    public UUIDFilter getResolverId() {
        return resolverId;
    }

    public Optional<UUIDFilter> optionalResolverId() {
        return Optional.ofNullable(resolverId);
    }

    public UUIDFilter resolverId() {
        if (resolverId == null) {
            setResolverId(new UUIDFilter());
        }
        return resolverId;
    }

    public void setResolverId(UUIDFilter resolverId) {
        this.resolverId = resolverId;
    }

    public InstantFilter getClosedAt() {
        return closedAt;
    }

    public Optional<InstantFilter> optionalClosedAt() {
        return Optional.ofNullable(closedAt);
    }

    public InstantFilter closedAt() {
        if (closedAt == null) {
            setClosedAt(new InstantFilter());
        }
        return closedAt;
    }

    public void setClosedAt(InstantFilter closedAt) {
        this.closedAt = closedAt;
    }

    public UUIDFilter getClosedBy() {
        return closedBy;
    }

    public Optional<UUIDFilter> optionalClosedBy() {
        return Optional.ofNullable(closedBy);
    }

    public UUIDFilter closedBy() {
        if (closedBy == null) {
            setClosedBy(new UUIDFilter());
        }
        return closedBy;
    }

    public void setClosedBy(UUIDFilter closedBy) {
        this.closedBy = closedBy;
    }

    public LongFilter getVersion() {
        return version;
    }

    public Optional<LongFilter> optionalVersion() {
        return Optional.ofNullable(version);
    }

    public LongFilter version() {
        if (version == null) {
            setVersion(new LongFilter());
        }
        return version;
    }

    public void setVersion(LongFilter version) {
        this.version = version;
    }

    public UUIDFilter getRelatedTicketId() {
        return relatedTicketId;
    }

    public Optional<UUIDFilter> optionalRelatedTicketId() {
        return Optional.ofNullable(relatedTicketId);
    }

    public UUIDFilter relatedTicketId() {
        if (relatedTicketId == null) {
            setRelatedTicketId(new UUIDFilter());
        }
        return relatedTicketId;
    }

    public void setRelatedTicketId(UUIDFilter relatedTicketId) {
        this.relatedTicketId = relatedTicketId;
    }

    public UUIDFilter getIncidentId() {
        return incidentId;
    }

    public Optional<UUIDFilter> optionalIncidentId() {
        return Optional.ofNullable(incidentId);
    }

    public UUIDFilter incidentId() {
        if (incidentId == null) {
            setIncidentId(new UUIDFilter());
        }
        return incidentId;
    }

    public void setIncidentId(UUIDFilter incidentId) {
        this.incidentId = incidentId;
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
        final TicketCriteria that = (TicketCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(ticketNumber, that.ticketNumber) &&
            Objects.equals(type, that.type) &&
            Objects.equals(subject, that.subject) &&
            Objects.equals(origin, that.origin) &&
            Objects.equals(originRef, that.originRef) &&
            Objects.equals(requesterId, that.requesterId) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(linkId, that.linkId) &&
            Objects.equals(alertId, that.alertId) &&
            Objects.equals(priority, that.priority) &&
            Objects.equals(state, that.state) &&
            Objects.equals(assigneeId, that.assigneeId) &&
            Objects.equals(openedAt, that.openedAt) &&
            Objects.equals(resolvedAt, that.resolvedAt) &&
            Objects.equals(resolverId, that.resolverId) &&
            Objects.equals(closedAt, that.closedAt) &&
            Objects.equals(closedBy, that.closedBy) &&
            Objects.equals(version, that.version) &&
            Objects.equals(relatedTicketId, that.relatedTicketId) &&
            Objects.equals(incidentId, that.incidentId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            ticketNumber,
            type,
            subject,
            origin,
            originRef,
            requesterId,
            institutionId,
            linkId,
            alertId,
            priority,
            state,
            assigneeId,
            openedAt,
            resolvedAt,
            resolverId,
            closedAt,
            closedBy,
            version,
            relatedTicketId,
            incidentId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TicketCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTicketNumber().map(f -> "ticketNumber=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalSubject().map(f -> "subject=" + f + ", ").orElse("") +
            optionalOrigin().map(f -> "origin=" + f + ", ").orElse("") +
            optionalOriginRef().map(f -> "originRef=" + f + ", ").orElse("") +
            optionalRequesterId().map(f -> "requesterId=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalLinkId().map(f -> "linkId=" + f + ", ").orElse("") +
            optionalAlertId().map(f -> "alertId=" + f + ", ").orElse("") +
            optionalPriority().map(f -> "priority=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalAssigneeId().map(f -> "assigneeId=" + f + ", ").orElse("") +
            optionalOpenedAt().map(f -> "openedAt=" + f + ", ").orElse("") +
            optionalResolvedAt().map(f -> "resolvedAt=" + f + ", ").orElse("") +
            optionalResolverId().map(f -> "resolverId=" + f + ", ").orElse("") +
            optionalClosedAt().map(f -> "closedAt=" + f + ", ").orElse("") +
            optionalClosedBy().map(f -> "closedBy=" + f + ", ").orElse("") +
            optionalVersion().map(f -> "version=" + f + ", ").orElse("") +
            optionalRelatedTicketId().map(f -> "relatedTicketId=" + f + ", ").orElse("") +
            optionalIncidentId().map(f -> "incidentId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
