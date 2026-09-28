package cm.gimac.des.participant.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.participant.domain.EscalationContact} entity. This class is used
 * in {@link cm.gimac.des.participant.web.rest.EscalationContactResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /escalation-contacts?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EscalationContactCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private IntegerFilter severity;

    private IntegerFilter escalationRank;

    private DurationFilter ackDelay;

    private BooleanFilter active;

    private UUIDFilter institutionId;

    private UUIDFilter correspondentId;

    private UUIDFilter deputyId;

    private Boolean distinct;

    public EscalationContactCriteria() {}

    public EscalationContactCriteria(EscalationContactCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.severity = other.optionalSeverity().map(IntegerFilter::copy).orElse(null);
        this.escalationRank = other.optionalEscalationRank().map(IntegerFilter::copy).orElse(null);
        this.ackDelay = other.optionalAckDelay().map(DurationFilter::copy).orElse(null);
        this.active = other.optionalActive().map(BooleanFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.correspondentId = other.optionalCorrespondentId().map(UUIDFilter::copy).orElse(null);
        this.deputyId = other.optionalDeputyId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public EscalationContactCriteria copy() {
        return new EscalationContactCriteria(this);
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

    public IntegerFilter getSeverity() {
        return severity;
    }

    public Optional<IntegerFilter> optionalSeverity() {
        return Optional.ofNullable(severity);
    }

    public IntegerFilter severity() {
        if (severity == null) {
            setSeverity(new IntegerFilter());
        }
        return severity;
    }

    public void setSeverity(IntegerFilter severity) {
        this.severity = severity;
    }

    public IntegerFilter getEscalationRank() {
        return escalationRank;
    }

    public Optional<IntegerFilter> optionalEscalationRank() {
        return Optional.ofNullable(escalationRank);
    }

    public IntegerFilter escalationRank() {
        if (escalationRank == null) {
            setEscalationRank(new IntegerFilter());
        }
        return escalationRank;
    }

    public void setEscalationRank(IntegerFilter escalationRank) {
        this.escalationRank = escalationRank;
    }

    public DurationFilter getAckDelay() {
        return ackDelay;
    }

    public Optional<DurationFilter> optionalAckDelay() {
        return Optional.ofNullable(ackDelay);
    }

    public DurationFilter ackDelay() {
        if (ackDelay == null) {
            setAckDelay(new DurationFilter());
        }
        return ackDelay;
    }

    public void setAckDelay(DurationFilter ackDelay) {
        this.ackDelay = ackDelay;
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

    public UUIDFilter getCorrespondentId() {
        return correspondentId;
    }

    public Optional<UUIDFilter> optionalCorrespondentId() {
        return Optional.ofNullable(correspondentId);
    }

    public UUIDFilter correspondentId() {
        if (correspondentId == null) {
            setCorrespondentId(new UUIDFilter());
        }
        return correspondentId;
    }

    public void setCorrespondentId(UUIDFilter correspondentId) {
        this.correspondentId = correspondentId;
    }

    public UUIDFilter getDeputyId() {
        return deputyId;
    }

    public Optional<UUIDFilter> optionalDeputyId() {
        return Optional.ofNullable(deputyId);
    }

    public UUIDFilter deputyId() {
        if (deputyId == null) {
            setDeputyId(new UUIDFilter());
        }
        return deputyId;
    }

    public void setDeputyId(UUIDFilter deputyId) {
        this.deputyId = deputyId;
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
        final EscalationContactCriteria that = (EscalationContactCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(severity, that.severity) &&
            Objects.equals(escalationRank, that.escalationRank) &&
            Objects.equals(ackDelay, that.ackDelay) &&
            Objects.equals(active, that.active) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(correspondentId, that.correspondentId) &&
            Objects.equals(deputyId, that.deputyId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, severity, escalationRank, ackDelay, active, institutionId, correspondentId, deputyId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EscalationContactCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalSeverity().map(f -> "severity=" + f + ", ").orElse("") +
            optionalEscalationRank().map(f -> "escalationRank=" + f + ", ").orElse("") +
            optionalAckDelay().map(f -> "ackDelay=" + f + ", ").orElse("") +
            optionalActive().map(f -> "active=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalCorrespondentId().map(f -> "correspondentId=" + f + ", ").orElse("") +
            optionalDeputyId().map(f -> "deputyId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
