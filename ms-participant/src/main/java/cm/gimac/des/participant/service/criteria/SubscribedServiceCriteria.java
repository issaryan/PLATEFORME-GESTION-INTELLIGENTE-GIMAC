package cm.gimac.des.participant.service.criteria;

import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.participant.domain.SubscribedService} entity. This class is used
 * in {@link cm.gimac.des.participant.web.rest.SubscribedServiceResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /subscribed-services?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SubscribedServiceCriteria implements Serializable, Criteria {

    /**
     * Class for filtering SubscriptionState
     */
    public static class SubscriptionStateFilter extends Filter<SubscriptionState> {

        public SubscriptionStateFilter() {}

        public SubscriptionStateFilter(SubscriptionStateFilter filter) {
            super(filter);
        }

        @Override
        public SubscriptionStateFilter copy() {
            return new SubscriptionStateFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter serviceCode;

    private LocalDateFilter subscribedOn;

    private LocalDateFilter terminatedOn;

    private SubscriptionStateFilter state;

    private UUIDFilter institutionId;

    private Boolean distinct;

    public SubscribedServiceCriteria() {}

    public SubscribedServiceCriteria(SubscribedServiceCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.subscribedOn = other.optionalSubscribedOn().map(LocalDateFilter::copy).orElse(null);
        this.terminatedOn = other.optionalTerminatedOn().map(LocalDateFilter::copy).orElse(null);
        this.state = other.optionalState().map(SubscriptionStateFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public SubscribedServiceCriteria copy() {
        return new SubscribedServiceCriteria(this);
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

    public StringFilter getServiceCode() {
        return serviceCode;
    }

    public Optional<StringFilter> optionalServiceCode() {
        return Optional.ofNullable(serviceCode);
    }

    public StringFilter serviceCode() {
        if (serviceCode == null) {
            setServiceCode(new StringFilter());
        }
        return serviceCode;
    }

    public void setServiceCode(StringFilter serviceCode) {
        this.serviceCode = serviceCode;
    }

    public LocalDateFilter getSubscribedOn() {
        return subscribedOn;
    }

    public Optional<LocalDateFilter> optionalSubscribedOn() {
        return Optional.ofNullable(subscribedOn);
    }

    public LocalDateFilter subscribedOn() {
        if (subscribedOn == null) {
            setSubscribedOn(new LocalDateFilter());
        }
        return subscribedOn;
    }

    public void setSubscribedOn(LocalDateFilter subscribedOn) {
        this.subscribedOn = subscribedOn;
    }

    public LocalDateFilter getTerminatedOn() {
        return terminatedOn;
    }

    public Optional<LocalDateFilter> optionalTerminatedOn() {
        return Optional.ofNullable(terminatedOn);
    }

    public LocalDateFilter terminatedOn() {
        if (terminatedOn == null) {
            setTerminatedOn(new LocalDateFilter());
        }
        return terminatedOn;
    }

    public void setTerminatedOn(LocalDateFilter terminatedOn) {
        this.terminatedOn = terminatedOn;
    }

    public SubscriptionStateFilter getState() {
        return state;
    }

    public Optional<SubscriptionStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public SubscriptionStateFilter state() {
        if (state == null) {
            setState(new SubscriptionStateFilter());
        }
        return state;
    }

    public void setState(SubscriptionStateFilter state) {
        this.state = state;
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
        final SubscribedServiceCriteria that = (SubscribedServiceCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(subscribedOn, that.subscribedOn) &&
            Objects.equals(terminatedOn, that.terminatedOn) &&
            Objects.equals(state, that.state) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serviceCode, subscribedOn, terminatedOn, state, institutionId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SubscribedServiceCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalSubscribedOn().map(f -> "subscribedOn=" + f + ", ").orElse("") +
            optionalTerminatedOn().map(f -> "terminatedOn=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
