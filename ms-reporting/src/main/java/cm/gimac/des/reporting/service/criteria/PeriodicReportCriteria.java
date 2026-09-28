package cm.gimac.des.reporting.service.criteria;

import cm.gimac.des.reporting.domain.enumeration.Periodicity;
import cm.gimac.des.reporting.domain.enumeration.ReportState;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.reporting.domain.PeriodicReport} entity. This class is used
 * in {@link cm.gimac.des.reporting.web.rest.PeriodicReportResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /periodic-reports?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PeriodicReportCriteria implements Serializable, Criteria {

    /**
     * Class for filtering Periodicity
     */
    public static class PeriodicityFilter extends Filter<Periodicity> {

        public PeriodicityFilter() {}

        public PeriodicityFilter(PeriodicityFilter filter) {
            super(filter);
        }

        @Override
        public PeriodicityFilter copy() {
            return new PeriodicityFilter(this);
        }
    }

    /**
     * Class for filtering ReportState
     */
    public static class ReportStateFilter extends Filter<ReportState> {

        public ReportStateFilter() {}

        public ReportStateFilter(ReportStateFilter filter) {
            super(filter);
        }

        @Override
        public ReportStateFilter copy() {
            return new ReportStateFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter serviceCode;

    private PeriodicityFilter periodicity;

    private StringFilter period;

    private IntegerFilter reportVersion;

    private BooleanFilter manual;

    private StringFilter contentDigest;

    private InstantFilter generatedAt;

    private UUIDFilter signerId;

    private InstantFilter signedAt;

    private UUIDFilter documentRef;

    private InstantFilter notifiedAt;

    private ReportStateFilter state;

    private Boolean distinct;

    public PeriodicReportCriteria() {}

    public PeriodicReportCriteria(PeriodicReportCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.serviceCode = other.optionalServiceCode().map(StringFilter::copy).orElse(null);
        this.periodicity = other.optionalPeriodicity().map(PeriodicityFilter::copy).orElse(null);
        this.period = other.optionalPeriod().map(StringFilter::copy).orElse(null);
        this.reportVersion = other.optionalReportVersion().map(IntegerFilter::copy).orElse(null);
        this.manual = other.optionalManual().map(BooleanFilter::copy).orElse(null);
        this.contentDigest = other.optionalContentDigest().map(StringFilter::copy).orElse(null);
        this.generatedAt = other.optionalGeneratedAt().map(InstantFilter::copy).orElse(null);
        this.signerId = other.optionalSignerId().map(UUIDFilter::copy).orElse(null);
        this.signedAt = other.optionalSignedAt().map(InstantFilter::copy).orElse(null);
        this.documentRef = other.optionalDocumentRef().map(UUIDFilter::copy).orElse(null);
        this.notifiedAt = other.optionalNotifiedAt().map(InstantFilter::copy).orElse(null);
        this.state = other.optionalState().map(ReportStateFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public PeriodicReportCriteria copy() {
        return new PeriodicReportCriteria(this);
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

    public PeriodicityFilter getPeriodicity() {
        return periodicity;
    }

    public Optional<PeriodicityFilter> optionalPeriodicity() {
        return Optional.ofNullable(periodicity);
    }

    public PeriodicityFilter periodicity() {
        if (periodicity == null) {
            setPeriodicity(new PeriodicityFilter());
        }
        return periodicity;
    }

    public void setPeriodicity(PeriodicityFilter periodicity) {
        this.periodicity = periodicity;
    }

    public StringFilter getPeriod() {
        return period;
    }

    public Optional<StringFilter> optionalPeriod() {
        return Optional.ofNullable(period);
    }

    public StringFilter period() {
        if (period == null) {
            setPeriod(new StringFilter());
        }
        return period;
    }

    public void setPeriod(StringFilter period) {
        this.period = period;
    }

    public IntegerFilter getReportVersion() {
        return reportVersion;
    }

    public Optional<IntegerFilter> optionalReportVersion() {
        return Optional.ofNullable(reportVersion);
    }

    public IntegerFilter reportVersion() {
        if (reportVersion == null) {
            setReportVersion(new IntegerFilter());
        }
        return reportVersion;
    }

    public void setReportVersion(IntegerFilter reportVersion) {
        this.reportVersion = reportVersion;
    }

    public BooleanFilter getManual() {
        return manual;
    }

    public Optional<BooleanFilter> optionalManual() {
        return Optional.ofNullable(manual);
    }

    public BooleanFilter manual() {
        if (manual == null) {
            setManual(new BooleanFilter());
        }
        return manual;
    }

    public void setManual(BooleanFilter manual) {
        this.manual = manual;
    }

    public StringFilter getContentDigest() {
        return contentDigest;
    }

    public Optional<StringFilter> optionalContentDigest() {
        return Optional.ofNullable(contentDigest);
    }

    public StringFilter contentDigest() {
        if (contentDigest == null) {
            setContentDigest(new StringFilter());
        }
        return contentDigest;
    }

    public void setContentDigest(StringFilter contentDigest) {
        this.contentDigest = contentDigest;
    }

    public InstantFilter getGeneratedAt() {
        return generatedAt;
    }

    public Optional<InstantFilter> optionalGeneratedAt() {
        return Optional.ofNullable(generatedAt);
    }

    public InstantFilter generatedAt() {
        if (generatedAt == null) {
            setGeneratedAt(new InstantFilter());
        }
        return generatedAt;
    }

    public void setGeneratedAt(InstantFilter generatedAt) {
        this.generatedAt = generatedAt;
    }

    public UUIDFilter getSignerId() {
        return signerId;
    }

    public Optional<UUIDFilter> optionalSignerId() {
        return Optional.ofNullable(signerId);
    }

    public UUIDFilter signerId() {
        if (signerId == null) {
            setSignerId(new UUIDFilter());
        }
        return signerId;
    }

    public void setSignerId(UUIDFilter signerId) {
        this.signerId = signerId;
    }

    public InstantFilter getSignedAt() {
        return signedAt;
    }

    public Optional<InstantFilter> optionalSignedAt() {
        return Optional.ofNullable(signedAt);
    }

    public InstantFilter signedAt() {
        if (signedAt == null) {
            setSignedAt(new InstantFilter());
        }
        return signedAt;
    }

    public void setSignedAt(InstantFilter signedAt) {
        this.signedAt = signedAt;
    }

    public UUIDFilter getDocumentRef() {
        return documentRef;
    }

    public Optional<UUIDFilter> optionalDocumentRef() {
        return Optional.ofNullable(documentRef);
    }

    public UUIDFilter documentRef() {
        if (documentRef == null) {
            setDocumentRef(new UUIDFilter());
        }
        return documentRef;
    }

    public void setDocumentRef(UUIDFilter documentRef) {
        this.documentRef = documentRef;
    }

    public InstantFilter getNotifiedAt() {
        return notifiedAt;
    }

    public Optional<InstantFilter> optionalNotifiedAt() {
        return Optional.ofNullable(notifiedAt);
    }

    public InstantFilter notifiedAt() {
        if (notifiedAt == null) {
            setNotifiedAt(new InstantFilter());
        }
        return notifiedAt;
    }

    public void setNotifiedAt(InstantFilter notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public ReportStateFilter getState() {
        return state;
    }

    public Optional<ReportStateFilter> optionalState() {
        return Optional.ofNullable(state);
    }

    public ReportStateFilter state() {
        if (state == null) {
            setState(new ReportStateFilter());
        }
        return state;
    }

    public void setState(ReportStateFilter state) {
        this.state = state;
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
        final PeriodicReportCriteria that = (PeriodicReportCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(serviceCode, that.serviceCode) &&
            Objects.equals(periodicity, that.periodicity) &&
            Objects.equals(period, that.period) &&
            Objects.equals(reportVersion, that.reportVersion) &&
            Objects.equals(manual, that.manual) &&
            Objects.equals(contentDigest, that.contentDigest) &&
            Objects.equals(generatedAt, that.generatedAt) &&
            Objects.equals(signerId, that.signerId) &&
            Objects.equals(signedAt, that.signedAt) &&
            Objects.equals(documentRef, that.documentRef) &&
            Objects.equals(notifiedAt, that.notifiedAt) &&
            Objects.equals(state, that.state) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            serviceCode,
            periodicity,
            period,
            reportVersion,
            manual,
            contentDigest,
            generatedAt,
            signerId,
            signedAt,
            documentRef,
            notifiedAt,
            state,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PeriodicReportCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalServiceCode().map(f -> "serviceCode=" + f + ", ").orElse("") +
            optionalPeriodicity().map(f -> "periodicity=" + f + ", ").orElse("") +
            optionalPeriod().map(f -> "period=" + f + ", ").orElse("") +
            optionalReportVersion().map(f -> "reportVersion=" + f + ", ").orElse("") +
            optionalManual().map(f -> "manual=" + f + ", ").orElse("") +
            optionalContentDigest().map(f -> "contentDigest=" + f + ", ").orElse("") +
            optionalGeneratedAt().map(f -> "generatedAt=" + f + ", ").orElse("") +
            optionalSignerId().map(f -> "signerId=" + f + ", ").orElse("") +
            optionalSignedAt().map(f -> "signedAt=" + f + ", ").orElse("") +
            optionalDocumentRef().map(f -> "documentRef=" + f + ", ").orElse("") +
            optionalNotifiedAt().map(f -> "notifiedAt=" + f + ", ").orElse("") +
            optionalState().map(f -> "state=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
