package cm.gimac.des.incident.service.criteria;

import cm.gimac.des.incident.domain.enumeration.TicketState;
import cm.gimac.des.incident.domain.enumeration.TicketState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.incident.domain.Transition} entity. This class is used
 * in {@link cm.gimac.des.incident.web.rest.TransitionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /transitions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TransitionCriteria implements Serializable, Criteria {

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

    private LongFilter chainIndex;

    private TicketStateFilter fromState;

    private TicketStateFilter toState;

    private UUIDFilter actorId;

    private InstantFilter occurredAt;

    private StringFilter justification;

    private StringFilter previousHash;

    private StringFilter hash;

    private UUIDFilter ticketId;

    private Boolean distinct;

    public TransitionCriteria() {}

    public TransitionCriteria(TransitionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.chainIndex = other.optionalChainIndex().map(LongFilter::copy).orElse(null);
        this.fromState = other.optionalFromState().map(TicketStateFilter::copy).orElse(null);
        this.toState = other.optionalToState().map(TicketStateFilter::copy).orElse(null);
        this.actorId = other.optionalActorId().map(UUIDFilter::copy).orElse(null);
        this.occurredAt = other.optionalOccurredAt().map(InstantFilter::copy).orElse(null);
        this.justification = other.optionalJustification().map(StringFilter::copy).orElse(null);
        this.previousHash = other.optionalPreviousHash().map(StringFilter::copy).orElse(null);
        this.hash = other.optionalHash().map(StringFilter::copy).orElse(null);
        this.ticketId = other.optionalTicketId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public TransitionCriteria copy() {
        return new TransitionCriteria(this);
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

    public LongFilter getChainIndex() {
        return chainIndex;
    }

    public Optional<LongFilter> optionalChainIndex() {
        return Optional.ofNullable(chainIndex);
    }

    public LongFilter chainIndex() {
        if (chainIndex == null) {
            setChainIndex(new LongFilter());
        }
        return chainIndex;
    }

    public void setChainIndex(LongFilter chainIndex) {
        this.chainIndex = chainIndex;
    }

    public TicketStateFilter getFromState() {
        return fromState;
    }

    public Optional<TicketStateFilter> optionalFromState() {
        return Optional.ofNullable(fromState);
    }

    public TicketStateFilter fromState() {
        if (fromState == null) {
            setFromState(new TicketStateFilter());
        }
        return fromState;
    }

    public void setFromState(TicketStateFilter fromState) {
        this.fromState = fromState;
    }

    public TicketStateFilter getToState() {
        return toState;
    }

    public Optional<TicketStateFilter> optionalToState() {
        return Optional.ofNullable(toState);
    }

    public TicketStateFilter toState() {
        if (toState == null) {
            setToState(new TicketStateFilter());
        }
        return toState;
    }

    public void setToState(TicketStateFilter toState) {
        this.toState = toState;
    }

    public UUIDFilter getActorId() {
        return actorId;
    }

    public Optional<UUIDFilter> optionalActorId() {
        return Optional.ofNullable(actorId);
    }

    public UUIDFilter actorId() {
        if (actorId == null) {
            setActorId(new UUIDFilter());
        }
        return actorId;
    }

    public void setActorId(UUIDFilter actorId) {
        this.actorId = actorId;
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

    public StringFilter getJustification() {
        return justification;
    }

    public Optional<StringFilter> optionalJustification() {
        return Optional.ofNullable(justification);
    }

    public StringFilter justification() {
        if (justification == null) {
            setJustification(new StringFilter());
        }
        return justification;
    }

    public void setJustification(StringFilter justification) {
        this.justification = justification;
    }

    public StringFilter getPreviousHash() {
        return previousHash;
    }

    public Optional<StringFilter> optionalPreviousHash() {
        return Optional.ofNullable(previousHash);
    }

    public StringFilter previousHash() {
        if (previousHash == null) {
            setPreviousHash(new StringFilter());
        }
        return previousHash;
    }

    public void setPreviousHash(StringFilter previousHash) {
        this.previousHash = previousHash;
    }

    public StringFilter getHash() {
        return hash;
    }

    public Optional<StringFilter> optionalHash() {
        return Optional.ofNullable(hash);
    }

    public StringFilter hash() {
        if (hash == null) {
            setHash(new StringFilter());
        }
        return hash;
    }

    public void setHash(StringFilter hash) {
        this.hash = hash;
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
        final TransitionCriteria that = (TransitionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(chainIndex, that.chainIndex) &&
            Objects.equals(fromState, that.fromState) &&
            Objects.equals(toState, that.toState) &&
            Objects.equals(actorId, that.actorId) &&
            Objects.equals(occurredAt, that.occurredAt) &&
            Objects.equals(justification, that.justification) &&
            Objects.equals(previousHash, that.previousHash) &&
            Objects.equals(hash, that.hash) &&
            Objects.equals(ticketId, that.ticketId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chainIndex, fromState, toState, actorId, occurredAt, justification, previousHash, hash, ticketId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TransitionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalChainIndex().map(f -> "chainIndex=" + f + ", ").orElse("") +
            optionalFromState().map(f -> "fromState=" + f + ", ").orElse("") +
            optionalToState().map(f -> "toState=" + f + ", ").orElse("") +
            optionalActorId().map(f -> "actorId=" + f + ", ").orElse("") +
            optionalOccurredAt().map(f -> "occurredAt=" + f + ", ").orElse("") +
            optionalJustification().map(f -> "justification=" + f + ", ").orElse("") +
            optionalPreviousHash().map(f -> "previousHash=" + f + ", ").orElse("") +
            optionalHash().map(f -> "hash=" + f + ", ").orElse("") +
            optionalTicketId().map(f -> "ticketId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
