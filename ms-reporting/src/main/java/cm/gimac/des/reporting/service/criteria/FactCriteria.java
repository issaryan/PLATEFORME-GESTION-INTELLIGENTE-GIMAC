package cm.gimac.des.reporting.service.criteria;

import cm.gimac.des.reporting.domain.enumeration.FactDomain;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.reporting.domain.Fact} entity. This class is used
 * in {@link cm.gimac.des.reporting.web.rest.FactResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /facts?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FactCriteria implements Serializable, Criteria {

    /**
     * Class for filtering FactDomain
     */
    public static class FactDomainFilter extends Filter<FactDomain> {

        public FactDomainFilter() {}

        public FactDomainFilter(FactDomainFilter filter) {
            super(filter);
        }

        @Override
        public FactDomainFilter copy() {
            return new FactDomainFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private FactDomainFilter domain;

    private LocalDateFilter timeKey;

    private UUIDFilter institutionId;

    private StringFilter serviceCode;

    private StringFilter severity;

    private UUIDFilter sourceEventId;

    private InstantFilter recordedAt;

    private BooleanFilter superseded;

    private BooleanFilter compacted;

    private UUIDFilter statementId;

    private Boolean distinct;

    public FactCriteria() {}

    public FactCriteria(FactCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.domain = other.optionalDomain().map(FactDomainFilter::copy).orElse(null);
        this.timeKey = other.optionalTimeKey().map(LocalDateFilter::copy).orElse(null);
        this.institutionId = other.optionalInstitutionId().map(UUIDFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.severity = other.optionalSeverity().map(StringFilter::copy).orElse(null);
        this.sourceEventId = other.optionalSourceEventId().map(UUIDFilter::copy).orElse(null);
        this.recordedAt = other.optionalRecordedAt().map(InstantFilter::copy).orElse(null);
        this.superseded = other.optionalSuperseded().map(BooleanFilter::copy).orElse(null);
        this.compacted = other.optionalCompacted().map(BooleanFilter::copy).orElse(null);
        this.statementId = other.optionalStatementId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public FactCriteria copy() {
        return new FactCriteria(this);
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

    public FactDomainFilter getDomain() {
        return domain;
    }

    public Optional<FactDomainFilter> optionalDomain() {
        return Optional.ofNullable(domain);
    }

    public FactDomainFilter domain() {
        if (domain == null) {
            setDomain(new FactDomainFilter());
        }
        return domain;
    }

    public void setDomain(FactDomainFilter domain) {
        this.domain = domain;
    }

    public LocalDateFilter getTimeKey() {
        return timeKey;
    }

    public Optional<LocalDateFilter> optionalTimeKey() {
        return Optional.ofNullable(timeKey);
    }

    public LocalDateFilter timeKey() {
        if (timeKey == null) {
            setTimeKey(new LocalDateFilter());
        }
        return timeKey;
    }

    public void setTimeKey(LocalDateFilter timeKey) {
        this.timeKey = timeKey;
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

    public StringFilter getSeverity() {
        return severity;
    }

    public Optional<StringFilter> optionalSeverity() {
        return Optional.ofNullable(severity);
    }

    public StringFilter severity() {
        if (severity == null) {
            setSeverity(new StringFilter());
        }
        return severity;
    }

    public void setSeverity(StringFilter severity) {
        this.severity = severity;
    }

    public UUIDFilter getSourceEventId() {
        return sourceEventId;
    }

    public Optional<UUIDFilter> optionalSourceEventId() {
        return Optional.ofNullable(sourceEventId);
    }

    public UUIDFilter sourceEventId() {
        if (sourceEventId == null) {
            setSourceEventId(new UUIDFilter());
        }
        return sourceEventId;
    }

    public void setSourceEventId(UUIDFilter sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public InstantFilter getRecordedAt() {
        return recordedAt;
    }

    public Optional<InstantFilter> optionalRecordedAt() {
        return Optional.ofNullable(recordedAt);
    }

    public InstantFilter recordedAt() {
        if (recordedAt == null) {
            setRecordedAt(new InstantFilter());
        }
        return recordedAt;
    }

    public void setRecordedAt(InstantFilter recordedAt) {
        this.recordedAt = recordedAt;
    }

    public BooleanFilter getSuperseded() {
        return superseded;
    }

    public Optional<BooleanFilter> optionalSuperseded() {
        return Optional.ofNullable(superseded);
    }

    public BooleanFilter superseded() {
        if (superseded == null) {
            setSuperseded(new BooleanFilter());
        }
        return superseded;
    }

    public void setSuperseded(BooleanFilter superseded) {
        this.superseded = superseded;
    }

    public BooleanFilter getCompacted() {
        return compacted;
    }

    public Optional<BooleanFilter> optionalCompacted() {
        return Optional.ofNullable(compacted);
    }

    public BooleanFilter compacted() {
        if (compacted == null) {
            setCompacted(new BooleanFilter());
        }
        return compacted;
    }

    public void setCompacted(BooleanFilter compacted) {
        this.compacted = compacted;
    }

    public UUIDFilter getStatementId() {
        return statementId;
    }

    public Optional<UUIDFilter> optionalStatementId() {
        return Optional.ofNullable(statementId);
    }

    public UUIDFilter statementId() {
        if (statementId == null) {
            setStatementId(new UUIDFilter());
        }
        return statementId;
    }

    public void setStatementId(UUIDFilter statementId) {
        this.statementId = statementId;
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
        final FactCriteria that = (FactCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(domain, that.domain) &&
            Objects.equals(timeKey, that.timeKey) &&
            Objects.equals(institutionId, that.institutionId) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(severity, that.severity) &&
            Objects.equals(sourceEventId, that.sourceEventId) &&
            Objects.equals(recordedAt, that.recordedAt) &&
            Objects.equals(superseded, that.superseded) &&
            Objects.equals(compacted, that.compacted) &&
            Objects.equals(statementId, that.statementId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            domain,
            timeKey,
            institutionId,
            serviceCode,
            severity,
            sourceEventId,
            recordedAt,
            superseded,
            compacted,
            statementId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FactCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalDomain().map(f -> "domain=" + f + ", ").orElse("") +
            optionalTimeKey().map(f -> "timeKey=" + f + ", ").orElse("") +
            optionalInstitutionId().map(f -> "institutionId=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalSeverity().map(f -> "severity=" + f + ", ").orElse("") +
            optionalSourceEventId().map(f -> "sourceEventId=" + f + ", ").orElse("") +
            optionalRecordedAt().map(f -> "recordedAt=" + f + ", ").orElse("") +
            optionalSuperseded().map(f -> "superseded=" + f + ", ").orElse("") +
            optionalCompacted().map(f -> "compacted=" + f + ", ").orElse("") +
            optionalStatementId().map(f -> "statementId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
