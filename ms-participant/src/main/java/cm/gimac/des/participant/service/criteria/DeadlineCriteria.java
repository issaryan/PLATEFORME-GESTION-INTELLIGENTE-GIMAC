package cm.gimac.des.participant.service.criteria;

import cm.gimac.des.participant.domain.enumeration.DeadlineState;
import cm.gimac.des.participant.domain.enumeration.DeadlineThreshold;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.participant.domain.Deadline} entity. This class is used
 * in {@link cm.gimac.des.participant.web.rest.DeadlineResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /deadlines?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeadlineCriteria implements Serializable, Criteria {

    /**
     * Class for filtering DeadlineThreshold
     */
    public static class DeadlineThresholdFilter extends Filter<DeadlineThreshold> {

        public DeadlineThresholdFilter() {}

        public DeadlineThresholdFilter(DeadlineThresholdFilter filter) {
            super(filter);
        }

        @Override
        public DeadlineThresholdFilter copy() {
            return new DeadlineThresholdFilter(this);
        }
    }

    /**
     * Class for filtering DeadlineState
     */
    public static class DeadlineStateFilter extends Filter<DeadlineState> {

        public DeadlineStateFilter() {}

        public DeadlineStateFilter(DeadlineStateFilter filter) {
            super(filter);
        }

        @Override
        public DeadlineStateFilter copy() {
            return new DeadlineStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private DeadlineThresholdFilter threshold;

    private LocalDateFilter dueOn;

    private DeadlineStateFilter state;

    private StringFilter triggeredAction;

    private InstantFilter triggeredAt;

    private BooleanFilter acknowledgementRequired;

    private UUIDFilter notificationId;

    private InstantFilter acknowledgedAt;

    private UUIDFilter institutionId;

    private UUIDFilter legalDocumentId;

    private UUIDFilter conventionId;

    private Boolean distinct;

    public DeadlineCriteria() {}

    public DeadlineCriteria(DeadlineCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.threshold = other.optionalThreshold().map(DeadlineThresholdFilter::copy).orElse(null);
        this.dueOn = other.optionalDueOn().map(LocalDateFilter::copy).orElse(null);
        this.state = other.optionalState().map(DeadlineStateFilter::copy).orElse(null);
        this.triggeredAction = other.optionalTriggeredAction().map(StringFilter::copy).orElse(null);
        this.triggeredAt = other.optionalTriggeredAt().map(InstantFilter::copy).orElse(null);
        this.acknowledgementRequired = other.optionalAcknowledgementRequired().map(BooleanFilter::copy).orElse(null);
        this.notificationId = other.optionalNotificationId().map(UUIDFilter::copy).orElse(null);
        this.acknowledgedAt = other.optionalAcknowledgedAt().map(InstantFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.legalDocumentId = other.optionalLegalDocumentId().map(UUIDFilter::copy).orElse(null);
        this.conventionId = other.optionalConventionId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DeadlineCriteria copy() {
        return new DeadlineCriteria(this);
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

    public DeadlineThresholdFilter getThreshold() {
        return threshold;
    }

    public Optional<DeadlineThresholdFilter> optionalThreshold() {
        return Optional.ofNullable(threshold);
    }

    public DeadlineThresholdFilter threshold() {
        if (threshold == null) {
            setThreshold(new DeadlineThresholdFilter());
        }
        return threshold;
    }

    public void setThreshold(DeadlineThresholdFilter threshold) {
        this.threshold = threshold;
    }

    public LocalDateFilter getDueOn() {
        return dueOn;
    }

    public Optional<LocalDateFilter> optionalDueOn() {
        return Optional.ofNullable(dueOn);
    }

    public LocalDateFilter dueOn() {
        if (dueOn == null) {
            setDueOn(new LocalDateFilter());
        }
        return dueOn;
    }

    public void setDueOn(LocalDateFilter dueOn) {
        this.dueOn = dueOn;
    }

    public DeadlineStateFilter getState() {
        return state;
    }

    public Optional<DeadlineStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public DeadlineStateFilter state() {
        if (state == null) {
            setState(new DeadlineStateFilter());
        }
        return state;
    }

    public void setState(DeadlineStateFilter state) {
        this.state = state;
    }

    public StringFilter getTriggeredAction() {
        return triggeredAction;
    }

    public Optional<StringFilter> optionalTriggeredAction() {
        return Optional.ofNullable(triggeredAction);
    }

    public StringFilter triggeredAction() {
        if (triggeredAction == null) {
            setTriggeredAction(new StringFilter());
        }
        return triggeredAction;
    }

    public void setTriggeredAction(StringFilter triggeredAction) {
        this.triggeredAction = triggeredAction;
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

    public BooleanFilter getAcknowledgementRequired() {
        return acknowledgementRequired;
    }

    public Optional<BooleanFilter> optionalAcknowledgementRequired() {
        return Optional.ofNullable(acknowledgementRequired);
    }

    public BooleanFilter acknowledgementRequired() {
        if (acknowledgementRequired == null) {
            setAcknowledgementRequired(new BooleanFilter());
        }
        return acknowledgementRequired;
    }

    public void setAcknowledgementRequired(BooleanFilter acknowledgementRequired) {
        this.acknowledgementRequired = acknowledgementRequired;
    }

    public UUIDFilter getNotificationId() {
        return notificationId;
    }

    public Optional<UUIDFilter> optionalNotificationId() {
        return Optional.ofNullable(notificationId);
    }

    public UUIDFilter notificationId() {
        if (notificationId == null) {
            setNotificationId(new UUIDFilter());
        }
        return notificationId;
    }

    public void setNotificationId(UUIDFilter notificationId) {
        this.notificationId = notificationId;
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

    public UUIDFilter getLegalDocumentId() {
        return legalDocumentId;
    }

    public Optional<UUIDFilter> optionalLegalDocumentId() {
        return Optional.ofNullable(legalDocumentId);
    }

    public UUIDFilter legalDocumentId() {
        if (legalDocumentId == null) {
            setLegalDocumentId(new UUIDFilter());
        }
        return legalDocumentId;
    }

    public void setLegalDocumentId(UUIDFilter legalDocumentId) {
        this.legalDocumentId = legalDocumentId;
    }

    public UUIDFilter getConventionId() {
        return conventionId;
    }

    public Optional<UUIDFilter> optionalConventionId() {
        return Optional.ofNullable(conventionId);
    }

    public UUIDFilter conventionId() {
        if (conventionId == null) {
            setConventionId(new UUIDFilter());
        }
        return conventionId;
    }

    public void setConventionId(UUIDFilter conventionId) {
        this.conventionId = conventionId;
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
        final DeadlineCriteria that = (DeadlineCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(threshold, that.threshold) &&
            Objects.equals(dueOn, that.dueOn) &&
            Objects.equals(state, that.state) &&
            Objects.equals(triggeredAction, that.triggeredAction) &&
            Objects.equals(triggeredAt, that.triggeredAt) &&
            Objects.equals(acknowledgementRequired, that.acknowledgementRequired) &&
            Objects.equals(notificationId, that.notificationId) &&
            Objects.equals(acknowledgedAt, that.acknowledgedAt) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(legalDocumentId, that.legalDocumentId) &&
            Objects.equals(conventionId, that.conventionId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            threshold,
            dueOn,
            state,
            triggeredAction,
            triggeredAt,
            acknowledgementRequired,
            notificationId,
            acknowledgedAt,
            institutionId,
            legalDocumentId,
            conventionId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DeadlineCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalThreshold().map(f -> "threshold=" + f + ", ").orElse("") +
            optionalDueOn().map(f -> "dueOn=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalTriggeredAction().map(f -> "triggeredAction=" + f + ", ").orElse("") +
            optionalTriggeredAt().map(f -> "triggeredAt=" + f + ", ").orElse("") +
            optionalAcknowledgementRequired().map(f -> "acknowledgementRequired=" + f + ", ").orElse("") +
            optionalNotificationId().map(f -> "notificationId=" + f + ", ").orElse("") +
            optionalAcknowledgedAt().map(f -> "acknowledgedAt=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalLegalDocumentId().map(f -> "legalDocumentId=" + f + ", ").orElse("") +
            optionalConventionId().map(f -> "conventionId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
