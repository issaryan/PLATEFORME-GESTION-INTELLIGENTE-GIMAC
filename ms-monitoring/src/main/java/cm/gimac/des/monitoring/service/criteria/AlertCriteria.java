package cm.gimac.des.monitoring.service.criteria;

import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.AlertState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.monitoring.domain.Alert} entity. This class is used
 * in {@link cm.gimac.des.monitoring.web.rest.AlertResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /alerts?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AlertCriteria implements Serializable, Criteria {

    /**
     * Class for filtering AlertSeverity
     */
    public static class AlertSeverityFilter extends Filter<AlertSeverity> {

        public AlertSeverityFilter() {}

        public AlertSeverityFilter(AlertSeverityFilter filter) {
            super(filter);
        }

        @Override
        public AlertSeverityFilter copy() {
            return new AlertSeverityFilter(this);
        }
    }

    /**
     * Class for filtering AlertState
     */
    public static class AlertStateFilter extends Filter<AlertState> {

        public AlertStateFilter() {}

        public AlertStateFilter(AlertStateFilter filter) {
            super(filter);
        }

        @Override
        public AlertStateFilter copy() {
            return new AlertStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter reference;

    private StringFilter source;

    private StringFilter rule;

    private StringFilter fingerprint;

    private AlertSeverityFilter severity;

    private AlertStateFilter state;

    private InstantFilter firstSeenAt;

    private InstantFilter lastSeenAt;

    private IntegerFilter occurrences;

    private InstantFilter ackDeadline;

    private BooleanFilter overdue;

    private UUIDFilter acknowledgedBy;

    private InstantFilter acknowledgedAt;

    private BooleanFilter ticketRequested;

    private StringFilter ackComment;

    private UUIDFilter ticketId;

    private InstantFilter resolvedAt;

    private UUIDFilter linkId;

    private UUIDFilter correlationRootId;

    private Boolean distinct;

    public AlertCriteria() {}

    public AlertCriteria(AlertCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.reference = other.optionalReference().map(StringFilter::copy).orElse(null);
        this.source = other.optionalSource().map(StringFilter::copy).orElse(null);
        this.rule = other.optionalRule().map(StringFilter::copy).orElse(null);
        this.fingerprint = other.optionalFingerprint().map(StringFilter::copy).orElse(null);
        this.severity = other.optionalSeverity().map(AlertSeverityFilter::copy).orElse(null);
        this.state = other.optionalState().map(AlertStateFilter::copy).orElse(null);
        this.firstSeenAt = other.optionalFirstSeenAt().map(InstantFilter::copy).orElse(null);
        this.lastSeenAt = other.optionalLastSeenAt().map(InstantFilter::copy).orElse(null);
        this.occurrences = other.optionalOccurrences().map(IntegerFilter::copy).orElse(null);
        this.ackDeadline = other.optionalAckDeadline().map(InstantFilter::copy).orElse(null);
        this.overdue = other.optionalOverdue().map(BooleanFilter::copy).orElse(null);
        this.acknowledgedBy = other.optionalAcknowledgedBy().map(UUIDFilter::copy).orElse(null);
        this.acknowledgedAt = other.optionalAcknowledgedAt().map(InstantFilter::copy).orElse(null);
        this.ticketRequested = other.optionalTicketRequested().map(BooleanFilter::copy).orElse(null);
        this.ackComment = other.optionalAckComment().map(StringFilter::copy).orElse(null);
        this.ticketId = other.optionalTicketId().map(UUIDFilter::copy).orElse(null);
        this.resolvedAt = other.optionalResolvedAt().map(InstantFilter::copy).orElse(null);
        this.linkId = other.optionalLinkId().map(UUIDFilter::copy).orElse(null);
        this.correlationRootId = other.optionalCorrelationRootId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public AlertCriteria copy() {
        return new AlertCriteria(this);
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

    public StringFilter getReference() {
        return reference;
    }

    public Optional<StringFilter> optionalReference() {
        return Optional.ofNullable(reference);
    }

    public StringFilter reference() {
        if (reference == null) {
            setReference(new StringFilter());
        }
        return reference;
    }

    public void setReference(StringFilter reference) {
        this.reference = reference;
    }

    public StringFilter getSource() {
        return source;
    }

    public Optional<StringFilter> optionalSource() {
        return Optional.ofNullable(source);
    }

    public StringFilter source() {
        if (source == null) {
            setSource(new StringFilter());
        }
        return source;
    }

    public void setSource(StringFilter source) {
        this.source = source;
    }

    public StringFilter getRule() {
        return rule;
    }

    public Optional<StringFilter> optionalRule() {
        return Optional.ofNullable(rule);
    }

    public StringFilter rule() {
        if (rule == null) {
            setRule(new StringFilter());
        }
        return rule;
    }

    public void setRule(StringFilter rule) {
        this.rule = rule;
    }

    public StringFilter getFingerprint() {
        return fingerprint;
    }

    public Optional<StringFilter> optionalFingerprint() {
        return Optional.ofNullable(fingerprint);
    }

    public StringFilter fingerprint() {
        if (fingerprint == null) {
            setFingerprint(new StringFilter());
        }
        return fingerprint;
    }

    public void setFingerprint(StringFilter fingerprint) {
        this.fingerprint = fingerprint;
    }

    public AlertSeverityFilter getSeverity() {
        return severity;
    }

    public Optional<AlertSeverityFilter> optionalSeverity() {
        return Optional.ofNullable(severity);
    }

    public AlertSeverityFilter severity() {
        if (severity == null) {
            setSeverity(new AlertSeverityFilter());
        }
        return severity;
    }

    public void setSeverity(AlertSeverityFilter severity) {
        this.severity = severity;
    }

    public AlertStateFilter getState() {
        return state;
    }

    public Optional<AlertStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public AlertStateFilter state() {
        if (state == null) {
            setState(new AlertStateFilter());
        }
        return state;
    }

    public void setState(AlertStateFilter state) {
        this.state = state;
    }

    public InstantFilter getFirstSeenAt() {
        return firstSeenAt;
    }

    public Optional<InstantFilter> optionalFirstSeenAt() {
        return Optional.ofNullable(firstSeenAt);
    }

    public InstantFilter firstSeenAt() {
        if (firstSeenAt == null) {
            setFirstSeenAt(new InstantFilter());
        }
        return firstSeenAt;
    }

    public void setFirstSeenAt(InstantFilter firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public InstantFilter getLastSeenAt() {
        return lastSeenAt;
    }

    public Optional<InstantFilter> optionalLastSeenAt() {
        return Optional.ofNullable(lastSeenAt);
    }

    public InstantFilter lastSeenAt() {
        if (lastSeenAt == null) {
            setLastSeenAt(new InstantFilter());
        }
        return lastSeenAt;
    }

    public void setLastSeenAt(InstantFilter lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public IntegerFilter getOccurrences() {
        return occurrences;
    }

    public Optional<IntegerFilter> optionalOccurrences() {
        return Optional.ofNullable(occurrences);
    }

    public IntegerFilter occurrences() {
        if (occurrences == null) {
            setOccurrences(new IntegerFilter());
        }
        return occurrences;
    }

    public void setOccurrences(IntegerFilter occurrences) {
        this.occurrences = occurrences;
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

    public BooleanFilter getOverdue() {
        return overdue;
    }

    public Optional<BooleanFilter> optionalOverdue() {
        return Optional.ofNullable(overdue);
    }

    public BooleanFilter overdue() {
        if (overdue == null) {
            setOverdue(new BooleanFilter());
        }
        return overdue;
    }

    public void setOverdue(BooleanFilter overdue) {
        this.overdue = overdue;
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

    public BooleanFilter getTicketRequested() {
        return ticketRequested;
    }

    public Optional<BooleanFilter> optionalTicketRequested() {
        return Optional.ofNullable(ticketRequested);
    }

    public BooleanFilter ticketRequested() {
        if (ticketRequested == null) {
            setTicketRequested(new BooleanFilter());
        }
        return ticketRequested;
    }

    public void setTicketRequested(BooleanFilter ticketRequested) {
        this.ticketRequested = ticketRequested;
    }

    public StringFilter getAckComment() {
        return ackComment;
    }

    public Optional<StringFilter> optionalAckComment() {
        return Optional.ofNullable(ackComment);
    }

    public StringFilter ackComment() {
        if (ackComment == null) {
            setAckComment(new StringFilter());
        }
        return ackComment;
    }

    public void setAckComment(StringFilter ackComment) {
        this.ackComment = ackComment;
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

    public UUIDFilter getLinkId() {
        return linkId;
    }

    public Optional<UUIDFilter> optionalLinkId() {
        return Optional.ofNullable(linkId);
    }

    public UUIDFilter linkId() {
        if (linkId == null) {
            setLinkId(new UUIDFilter());
        }
        return linkId;
    }

    public void setLinkId(UUIDFilter linkId) {
        this.linkId = linkId;
    }

    public UUIDFilter getCorrelationRootId() {
        return correlationRootId;
    }

    public Optional<UUIDFilter> optionalCorrelationRootId() {
        return Optional.ofNullable(correlationRootId);
    }

    public UUIDFilter correlationRootId() {
        if (correlationRootId == null) {
            setCorrelationRootId(new UUIDFilter());
        }
        return correlationRootId;
    }

    public void setCorrelationRootId(UUIDFilter correlationRootId) {
        this.correlationRootId = correlationRootId;
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
        final AlertCriteria that = (AlertCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(source, that.source) &&
            Objects.equals(rule, that.rule) &&
            Objects.equals(fingerprint, that.fingerprint) &&
            Objects.equals(severity, that.severity) &&
            Objects.equals(state, that.state) &&
            Objects.equals(firstSeenAt, that.firstSeenAt) &&
            Objects.equals(lastSeenAt, that.lastSeenAt) &&
            Objects.equals(occurrences, that.occurrences) &&
            Objects.equals(ackDeadline, that.ackDeadline) &&
            Objects.equals(overdue, that.overdue) &&
            Objects.equals(acknowledgedBy, that.acknowledgedBy) &&
            Objects.equals(acknowledgedAt, that.acknowledgedAt) &&
            Objects.equals(ticketRequested, that.ticketRequested) &&
            Objects.equals(ackComment, that.ackComment) &&
            Objects.equals(ticketId, that.ticketId) &&
            Objects.equals(resolvedAt, that.resolvedAt) &&
            Objects.equals(linkId, that.linkId) &&
            Objects.equals(correlationRootId, that.correlationRootId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            reference,
            source,
            rule,
            fingerprint,
            severity,
            state,
            firstSeenAt,
            lastSeenAt,
            occurrences,
            ackDeadline,
            overdue,
            acknowledgedBy,
            acknowledgedAt,
            ticketRequested,
            ackComment,
            ticketId,
            resolvedAt,
            linkId,
            correlationRootId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AlertCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalReference().map(f -> "reference=" + f + ", ").orElse("") +
            optionalSource().map(f -> "source=" + f + ", ").orElse("") +
            optionalRule().map(f -> "rule=" + f + ", ").orElse("") +
            optionalFingerprint().map(f -> "fingerprint=" + f + ", ").orElse("") +
            optionalSeverity().map(f -> "severity=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalFirstSeenAt().map(f -> "firstSeenAt=" + f + ", ").orElse("") +
            optionalLastSeenAt().map(f -> "lastSeenAt=" + f + ", ").orElse("") +
            optionalOccurrences().map(f -> "occurrences=" + f + ", ").orElse("") +
            optionalAckDeadline().map(f -> "ackDeadline=" + f + ", ").orElse("") +
            optionalOverdue().map(f -> "overdue=" + f + ", ").orElse("") +
            optionalAcknowledgedBy().map(f -> "acknowledgedBy=" + f + ", ").orElse("") +
            optionalAcknowledgedAt().map(f -> "acknowledgedAt=" + f + ", ").orElse("") +
            optionalTicketRequested().map(f -> "ticketRequested=" + f + ", ").orElse("") +
            optionalAckComment().map(f -> "ackComment=" + f + ", ").orElse("") +
            optionalTicketId().map(f -> "ticketId=" + f + ", ").orElse("") +
            optionalResolvedAt().map(f -> "resolvedAt=" + f + ", ").orElse("") +
            optionalLinkId().map(f -> "linkId=" + f + ", ").orElse("") +
            optionalCorrelationRootId().map(f -> "correlationRootId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
