package cm.gimac.des.participant.service.criteria;

import cm.gimac.des.participant.domain.enumeration.CorrespondentFunction;
import cm.gimac.des.participant.domain.enumeration.Reachability;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.participant.domain.Correspondent} entity. This class is used
 * in {@link cm.gimac.des.participant.web.rest.CorrespondentResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /correspondents?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CorrespondentCriteria implements Serializable, Criteria {

    /**
     * Class for filtering CorrespondentFunction
     */
    public static class CorrespondentFunctionFilter extends Filter<CorrespondentFunction> {

        public CorrespondentFunctionFilter() {}

        public CorrespondentFunctionFilter(CorrespondentFunctionFilter filter) {
            super(filter);
        }

        @Override
        public CorrespondentFunctionFilter copy() {
            return new CorrespondentFunctionFilter(this);
        }
    }

    /**
     * Class for filtering Reachability
     */
    public static class ReachabilityFilter extends Filter<Reachability> {

        public ReachabilityFilter() {}

        public ReachabilityFilter(ReachabilityFilter filter) {
            super(filter);
        }

        @Override
        public ReachabilityFilter copy() {
            return new ReachabilityFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter fullName;

    private CorrespondentFunctionFilter duty;

    private StringFilter channels;

    private StringFilter email;

    private StringFilter phone;

    private StringFilter publicKeyRef;

    private ReachabilityFilter reachability;

    private InstantFilter lastReachabilityCheckAt;

    private BooleanFilter onCall247;

    private UUIDFilter userId;

    private BooleanFilter active;

    private UUIDFilter proposedBy;

    private UUIDFilter validatedBy;

    private InstantFilter validatedAt;

    private UUIDFilter institutionId;

    private Boolean distinct;

    public CorrespondentCriteria() {}

    public CorrespondentCriteria(CorrespondentCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.fullName = other.optionalFullName().map(StringFilter::copy).orElse(null);
        this.duty = other.optionalDuty().map(CorrespondentFunctionFilter::copy).orElse(null);
        this.channels = other.optionalChannels().map(StringFilter::copy).orElse(null);
        this.email = other.optionalEmail().map(StringFilter::copy).orElse(null);
        this.phone = other.optionalPhone().map(StringFilter::copy).orElse(null);
        this.publicKeyRef = other.optionalPublicKeyRef().map(StringFilter::copy).orElse(null);
        this.reachability = other.optionalReachability().map(ReachabilityFilter::copy).orElse(null);
        this.lastReachabilityCheckAt = other.optionalLastReachabilityCheckAt().map(InstantFilter::copy).orElse(null);
        this.onCall247 = other.optionalOnCall247().map(BooleanFilter::copy).orElse(null);
        this.userId = other.optionalUserId().map(UUIDFilter::copy).orElse(null);
        this.active = other.optionalActive().map(BooleanFilter::copy).orElse(null);
        this.proposedBy = other.optionalProposedBy().map(UUIDFilter::copy).orElse(null);
        this.validatedBy = other.optionalValidatedBy().map(UUIDFilter::copy).orElse(null);
        this.validatedAt = other.optionalValidatedAt().map(InstantFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CorrespondentCriteria copy() {
        return new CorrespondentCriteria(this);
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

    public StringFilter getFullName() {
        return fullName;
    }

    public Optional<StringFilter> optionalFullName() {
        return Optional.ofNullable(fullName);
    }

    public StringFilter fullName() {
        if (fullName == null) {
            setFullName(new StringFilter());
        }
        return fullName;
    }

    public void setFullName(StringFilter fullName) {
        this.fullName = fullName;
    }

    public CorrespondentFunctionFilter getDuty() {
        return duty;
    }

    public Optional<CorrespondentFunctionFilter> optionalDuty() {
        return Optional.ofNullable(duty);
    }

    public CorrespondentFunctionFilter duty() {
        if (duty == null) {
            setDuty(new CorrespondentFunctionFilter());
        }
        return duty;
    }

    public void setDuty(CorrespondentFunctionFilter duty) {
        this.duty = duty;
    }

    public StringFilter getChannels() {
        return channels;
    }

    public Optional<StringFilter> optionalChannels() {
        return Optional.ofNullable(channels);
    }

    public StringFilter channels() {
        if (channels == null) {
            setChannels(new StringFilter());
        }
        return channels;
    }

    public void setChannels(StringFilter channels) {
        this.channels = channels;
    }

    public StringFilter getEmail() {
        return email;
    }

    public Optional<StringFilter> optionalEmail() {
        return Optional.ofNullable(email);
    }

    public StringFilter email() {
        if (email == null) {
            setEmail(new StringFilter());
        }
        return email;
    }

    public void setEmail(StringFilter email) {
        this.email = email;
    }

    public StringFilter getPhone() {
        return phone;
    }

    public Optional<StringFilter> optionalPhone() {
        return Optional.ofNullable(phone);
    }

    public StringFilter phone() {
        if (phone == null) {
            setPhone(new StringFilter());
        }
        return phone;
    }

    public void setPhone(StringFilter phone) {
        this.phone = phone;
    }

    public StringFilter getPublicKeyRef() {
        return publicKeyRef;
    }

    public Optional<StringFilter> optionalPublicKeyRef() {
        return Optional.ofNullable(publicKeyRef);
    }

    public StringFilter publicKeyRef() {
        if (publicKeyRef == null) {
            setPublicKeyRef(new StringFilter());
        }
        return publicKeyRef;
    }

    public void setPublicKeyRef(StringFilter publicKeyRef) {
        this.publicKeyRef = publicKeyRef;
    }

    public ReachabilityFilter getReachability() {
        return reachability;
    }

    public Optional<ReachabilityFilter> optionalReachability() {
        return Optional.ofNullable(reachability);
    }

    public ReachabilityFilter reachability() {
        if (reachability == null) {
            setReachability(new ReachabilityFilter());
        }
        return reachability;
    }

    public void setReachability(ReachabilityFilter reachability) {
        this.reachability = reachability;
    }

    public InstantFilter getLastReachabilityCheckAt() {
        return lastReachabilityCheckAt;
    }

    public Optional<InstantFilter> optionalLastReachabilityCheckAt() {
        return Optional.ofNullable(lastReachabilityCheckAt);
    }

    public InstantFilter lastReachabilityCheckAt() {
        if (lastReachabilityCheckAt == null) {
            setLastReachabilityCheckAt(new InstantFilter());
        }
        return lastReachabilityCheckAt;
    }

    public void setLastReachabilityCheckAt(InstantFilter lastReachabilityCheckAt) {
        this.lastReachabilityCheckAt = lastReachabilityCheckAt;
    }

    public BooleanFilter getOnCall247() {
        return onCall247;
    }

    public Optional<BooleanFilter> optionalOnCall247() {
        return Optional.ofNullable(onCall247);
    }

    public BooleanFilter onCall247() {
        if (onCall247 == null) {
            setOnCall247(new BooleanFilter());
        }
        return onCall247;
    }

    public void setOnCall247(BooleanFilter onCall247) {
        this.onCall247 = onCall247;
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

    public UUIDFilter getProposedBy() {
        return proposedBy;
    }

    public Optional<UUIDFilter> optionalProposedBy() {
        return Optional.ofNullable(proposedBy);
    }

    public UUIDFilter proposedBy() {
        if (proposedBy == null) {
            setProposedBy(new UUIDFilter());
        }
        return proposedBy;
    }

    public void setProposedBy(UUIDFilter proposedBy) {
        this.proposedBy = proposedBy;
    }

    public UUIDFilter getValidatedBy() {
        return validatedBy;
    }

    public Optional<UUIDFilter> optionalValidatedBy() {
        return Optional.ofNullable(validatedBy);
    }

    public UUIDFilter validatedBy() {
        if (validatedBy == null) {
            setValidatedBy(new UUIDFilter());
        }
        return validatedBy;
    }

    public void setValidatedBy(UUIDFilter validatedBy) {
        this.validatedBy = validatedBy;
    }

    public InstantFilter getValidatedAt() {
        return validatedAt;
    }

    public Optional<InstantFilter> optionalValidatedAt() {
        return Optional.ofNullable(validatedAt);
    }

    public InstantFilter validatedAt() {
        if (validatedAt == null) {
            setValidatedAt(new InstantFilter());
        }
        return validatedAt;
    }

    public void setValidatedAt(InstantFilter validatedAt) {
        this.validatedAt = validatedAt;
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
        final CorrespondentCriteria that = (CorrespondentCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(fullName, that.fullName) &&
            Objects.equals(duty, that.duty) &&
            Objects.equals(channels, that.channels) &&
            Objects.equals(email, that.email) &&
            Objects.equals(phone, that.phone) &&
            Objects.equals(publicKeyRef, that.publicKeyRef) &&
            Objects.equals(reachability, that.reachability) &&
            Objects.equals(lastReachabilityCheckAt, that.lastReachabilityCheckAt) &&
            Objects.equals(onCall247, that.onCall247) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(active, that.active) &&
            Objects.equals(proposedBy, that.proposedBy) &&
            Objects.equals(validatedBy, that.validatedBy) &&
            Objects.equals(validatedAt, that.validatedAt) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            fullName,
            duty,
            channels,
            email,
            phone,
            publicKeyRef,
            reachability,
            lastReachabilityCheckAt,
            onCall247,
            userId,
            active,
            proposedBy,
            validatedBy,
            validatedAt,
            institutionId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CorrespondentCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalFullName().map(f -> "fullName=" + f + ", ").orElse("") +
            optionalDuty().map(f -> "duty=" + f + ", ").orElse("") +
            optionalChannels().map(f -> "channels=" + f + ", ").orElse("") +
            optionalEmail().map(f -> "email=" + f + ", ").orElse("") +
            optionalPhone().map(f -> "phone=" + f + ", ").orElse("") +
            optionalPublicKeyRef().map(f -> "publicKeyRef=" + f + ", ").orElse("") +
            optionalReachability().map(f -> "reachability=" + f + ", ").orElse("") +
            optionalLastReachabilityCheckAt().map(f -> "lastReachabilityCheckAt=" + f + ", ").orElse("") +
            optionalOnCall247().map(f -> "onCall247=" + f + ", ").orElse("") +
            optionalUserId().map(f -> "userId=" + f + ", ").orElse("") +
            optionalActive().map(f -> "active=" + f + ", ").orElse("") +
            optionalProposedBy().map(f -> "proposedBy=" + f + ", ").orElse("") +
            optionalValidatedBy().map(f -> "validatedBy=" + f + ", ").orElse("") +
            optionalValidatedAt().map(f -> "validatedAt=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
