package cm.gimac.des.monitoring.service.criteria;

import cm.gimac.des.monitoring.domain.enumeration.LinkScope;
import cm.gimac.des.monitoring.domain.enumeration.LinkState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.monitoring.domain.SupervisedLink} entity. This class is used
 * in {@link cm.gimac.des.monitoring.web.rest.SupervisedLinkResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /supervised-links?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SupervisedLinkCriteria implements Serializable, Criteria {

    /**
     * Class for filtering LinkScope
     */
    public static class LinkScopeFilter extends Filter<LinkScope> {

        public LinkScopeFilter() {}

        public LinkScopeFilter(LinkScopeFilter filter) {
            super(filter);
        }

        @Override
        public LinkScopeFilter copy() {
            return new LinkScopeFilter(this);
        }
    }

    /**
     * Class for filtering LinkState
     */
    public static class LinkStateFilter extends Filter<LinkState> {

        public LinkStateFilter() {}

        public LinkStateFilter(LinkStateFilter filter) {
            super(filter);
        }

        @Override
        public LinkStateFilter copy() {
            return new LinkStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter code;

    private StringFilter label;

    private LinkScopeFilter scope;

    private UUIDFilter institutionId;

    private StringFilter type;

    private LinkStateFilter state;

    private InstantFilter stateSince;

    private InstantFilter lastMeasureAt;

    private DurationFilter measurePeriod;

    private InstantFilter maintenanceFrom;

    private InstantFilter maintenanceUntil;

    private UUIDFilter parentPlatformId;

    private Boolean distinct;

    public SupervisedLinkCriteria() {}

    public SupervisedLinkCriteria(SupervisedLinkCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.code = other.optionalCode().map(StringFilter::copy).orElse(null);
        this.label = other.optionalLabel().map(StringFilter::copy).orElse(null);
        this.scope = other.optionalScope().map(LinkScopeFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.type = other.optionalType().map(StringFilter::copy).orElse(null);
        this.state = other.optionalState().map(LinkStateFilter::copy).orElse(null);
        this.stateSince = other.optionalStateSince().map(InstantFilter::copy).orElse(null);
        this.lastMeasureAt = other.optionalLastMeasureAt().map(InstantFilter::copy).orElse(null);
        this.measurePeriod = other.optionalMeasurePeriod().map(DurationFilter::copy).orElse(null);
        this.maintenanceFrom = other.optionalMaintenanceFrom().map(InstantFilter::copy).orElse(null);
        this.maintenanceUntil = other.optionalMaintenanceUntil().map(InstantFilter::copy).orElse(null);
        this.parentPlatformId = other.optionalParentPlatformId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public SupervisedLinkCriteria copy() {
        return new SupervisedLinkCriteria(this);
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

    public StringFilter getCode() {
        return code;
    }

    public Optional<StringFilter> optionalCode() {
        return Optional.ofNullable(code);
    }

    public StringFilter code() {
        if (code == null) {
            setCode(new StringFilter());
        }
        return code;
    }

    public void setCode(StringFilter code) {
        this.code = code;
    }

    public StringFilter getLabel() {
        return label;
    }

    public Optional<StringFilter> optionalLabel() {
        return Optional.ofNullable(label);
    }

    public StringFilter label() {
        if (label == null) {
            setLabel(new StringFilter());
        }
        return label;
    }

    public void setLabel(StringFilter label) {
        this.label = label;
    }

    public LinkScopeFilter getScope() {
        return scope;
    }

    public Optional<LinkScopeFilter> optionalScope() {
        return Optional.ofNullable(scope);
    }

    public LinkScopeFilter scope() {
        if (scope == null) {
            setScope(new LinkScopeFilter());
        }
        return scope;
    }

    public void setScope(LinkScopeFilter scope) {
        this.scope = scope;
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

    public StringFilter getType() {
        return type;
    }

    public Optional<StringFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public StringFilter type() {
        if (type == null) {
            setType(new StringFilter());
        }
        return type;
    }

    public void setType(StringFilter type) {
        this.type = type;
    }

    public LinkStateFilter getState() {
        return state;
    }

    public Optional<LinkStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public LinkStateFilter state() {
        if (state == null) {
            setState(new LinkStateFilter());
        }
        return state;
    }

    public void setState(LinkStateFilter state) {
        this.state = state;
    }

    public InstantFilter getStateSince() {
        return stateSince;
    }

    public Optional<InstantFilter> optionalStateSince() {
        return Optional.ofNullable(stateSince);
    }

    public InstantFilter stateSince() {
        if (stateSince == null) {
            setStateSince(new InstantFilter());
        }
        return stateSince;
    }

    public void setStateSince(InstantFilter stateSince) {
        this.stateSince = stateSince;
    }

    public InstantFilter getLastMeasureAt() {
        return lastMeasureAt;
    }

    public Optional<InstantFilter> optionalLastMeasureAt() {
        return Optional.ofNullable(lastMeasureAt);
    }

    public InstantFilter lastMeasureAt() {
        if (lastMeasureAt == null) {
            setLastMeasureAt(new InstantFilter());
        }
        return lastMeasureAt;
    }

    public void setLastMeasureAt(InstantFilter lastMeasureAt) {
        this.lastMeasureAt = lastMeasureAt;
    }

    public DurationFilter getMeasurePeriod() {
        return measurePeriod;
    }

    public Optional<DurationFilter> optionalMeasurePeriod() {
        return Optional.ofNullable(measurePeriod);
    }

    public DurationFilter measurePeriod() {
        if (measurePeriod == null) {
            setMeasurePeriod(new DurationFilter());
        }
        return measurePeriod;
    }

    public void setMeasurePeriod(DurationFilter measurePeriod) {
        this.measurePeriod = measurePeriod;
    }

    public InstantFilter getMaintenanceFrom() {
        return maintenanceFrom;
    }

    public Optional<InstantFilter> optionalMaintenanceFrom() {
        return Optional.ofNullable(maintenanceFrom);
    }

    public InstantFilter maintenanceFrom() {
        if (maintenanceFrom == null) {
            setMaintenanceFrom(new InstantFilter());
        }
        return maintenanceFrom;
    }

    public void setMaintenanceFrom(InstantFilter maintenanceFrom) {
        this.maintenanceFrom = maintenanceFrom;
    }

    public InstantFilter getMaintenanceUntil() {
        return maintenanceUntil;
    }

    public Optional<InstantFilter> optionalMaintenanceUntil() {
        return Optional.ofNullable(maintenanceUntil);
    }

    public InstantFilter maintenanceUntil() {
        if (maintenanceUntil == null) {
            setMaintenanceUntil(new InstantFilter());
        }
        return maintenanceUntil;
    }

    public void setMaintenanceUntil(InstantFilter maintenanceUntil) {
        this.maintenanceUntil = maintenanceUntil;
    }

    public UUIDFilter getParentPlatformId() {
        return parentPlatformId;
    }

    public Optional<UUIDFilter> optionalParentPlatformId() {
        return Optional.ofNullable(parentPlatformId);
    }

    public UUIDFilter parentPlatformId() {
        if (parentPlatformId == null) {
            setParentPlatformId(new UUIDFilter());
        }
        return parentPlatformId;
    }

    public void setParentPlatformId(UUIDFilter parentPlatformId) {
        this.parentPlatformId = parentPlatformId;
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
        final SupervisedLinkCriteria that = (SupervisedLinkCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(code, that.code) &&
            Objects.equals(label, that.label) &&
            Objects.equals(scope, that.scope) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(type, that.type) &&
            Objects.equals(state, that.state) &&
            Objects.equals(stateSince, that.stateSince) &&
            Objects.equals(lastMeasureAt, that.lastMeasureAt) &&
            Objects.equals(measurePeriod, that.measurePeriod) &&
            Objects.equals(maintenanceFrom, that.maintenanceFrom) &&
            Objects.equals(maintenanceUntil, that.maintenanceUntil) &&
            Objects.equals(parentPlatformId, that.parentPlatformId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            code,
            label,
            scope,
            institutionId,
            type,
            state,
            stateSince,
            lastMeasureAt,
            measurePeriod,
            maintenanceFrom,
            maintenanceUntil,
            parentPlatformId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SupervisedLinkCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalCode().map(f -> "code=" + f + ", ").orElse("") +
            optionalLabel().map(f -> "label=" + f + ", ").orElse("") +
            optionalScope().map(f -> "scope=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalStateSince().map(f -> "stateSince=" + f + ", ").orElse("") +
            optionalLastMeasureAt().map(f -> "lastMeasureAt=" + f + ", ").orElse("") +
            optionalMeasurePeriod().map(f -> "measurePeriod=" + f + ", ").orElse("") +
            optionalMaintenanceFrom().map(f -> "maintenanceFrom=" + f + ", ").orElse("") +
            optionalMaintenanceUntil().map(f -> "maintenanceUntil=" + f + ", ").orElse("") +
            optionalParentPlatformId().map(f -> "parentPlatformId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
