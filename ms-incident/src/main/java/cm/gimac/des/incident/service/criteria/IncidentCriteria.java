package cm.gimac.des.incident.service.criteria;

import cm.gimac.des.incident.domain.enumeration.Severity;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.incident.domain.Incident} entity. This class is used
 * in {@link cm.gimac.des.incident.web.rest.IncidentResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /incidents?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IncidentCriteria implements Serializable, Criteria {

    /**
     * Class for filtering Severity
     */
    public static class SeverityFilter extends Filter<Severity> {

        public SeverityFilter() {}

        public SeverityFilter(SeverityFilter filter) {
            super(filter);
        }

        @Override
        public SeverityFilter copy() {
            return new SeverityFilter(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private IntegerFilter impact;

    private IntegerFilter urgency;

    private IntegerFilter score;

    private SeverityFilter severity;

    private BooleanFilter securityIncident;

    private BooleanFilter dataCompromiseSuspected;

    private BooleanFilter regulatoryFlag;

    private InstantFilter qualifiedAt;

    private UUIDFilter qualifiedBy;

    private StringFilter rootCause;

    private StringFilter rootCauseCategory;

    private StringFilter rootCauseProofRef;

    private StringFilter resolutionTest;

    private BooleanFilter resolutionTestPassed;

    private UUIDFilter resolutionTestAuthorId;

    private InstantFilter resolutionTestAt;

    private StringFilter resolutionTestProofRef;

    private StringFilter lessonsLearnedRef;

    private StringFilter lessonsLearnedWaiver;

    private InstantFilter reportDueAt;

    private InstantFilter reportGeneratedAt;

    private StringFilter reportRef;

    private StringFilter reportDigest;

    private UUIDFilter complianceVisaBy;

    private InstantFilter complianceVisaAt;

    private InstantFilter criteriaValidatedAt;

    private UUIDFilter validatorId;

    private DurationFilter rawDuration;

    private DurationFilter frozenDuration;

    private DurationFilter imputableDelay;

    private DurationFilter mttr;

    private BooleanFilter takeChargeMet;

    private BooleanFilter restorationMet;

    private UUIDFilter ticketId;

    private UUIDFilter matrixId;

    private Boolean distinct;

    public IncidentCriteria() {}

    public IncidentCriteria(IncidentCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.impact = other.optionalImpact().map(IntegerFilter::copy).orElse(null);
        this.urgency = other.optionalUrgency().map(IntegerFilter::copy).orElse(null);
        this.score = other.optionalScore().map(IntegerFilter::copy).orElse(null);
        this.severity = other.optionalSeverity().map(SeverityFilter::copy).orElse(null);
        this.securityIncident = other.optionalSecurityIncident().map(BooleanFilter::copy).orElse(null);
        this.dataCompromiseSuspected = other.optionalDataCompromiseSuspected().map(BooleanFilter::copy).orElse(null);
        this.regulatoryFlag = other.optionalRegulatoryFlag().map(BooleanFilter::copy).orElse(null);
        this.qualifiedAt = other.optionalQualifiedAt().map(InstantFilter::copy).orElse(null);
        this.qualifiedBy = other.optionalQualifiedBy().map(UUIDFilter::copy).orElse(null);
        this.rootCause = other.optionalRootCause().map(StringFilter::copy).orElse(null);
        this.rootCauseCategory = other.optionalRootCauseCategory().map(StringFilter::copy).orElse(null);
        this.rootCauseProofRef = other.optionalRootCauseProofRef().map(StringFilter::copy).orElse(null);
        this.resolutionTest = other.optionalResolutionTest().map(StringFilter::copy).orElse(null);
        this.resolutionTestPassed = other.optionalResolutionTestPassed().map(BooleanFilter::copy).orElse(null);
        this.resolutionTestAuthorId = other.optionalResolutionTestAuthorId().map(UUIDFilter::copy).orElse(null);
        this.resolutionTestAt = other.optionalResolutionTestAt().map(InstantFilter::copy).orElse(null);
        this.resolutionTestProofRef = other.optionalResolutionTestProofRef().map(StringFilter::copy).orElse(null);
        this.lessonsLearnedRef = other.optionalLessonsLearnedRef().map(StringFilter::copy).orElse(null);
        this.lessonsLearnedWaiver = other.optionalLessonsLearnedWaiver().map(StringFilter::copy).orElse(null);
        this.reportDueAt = other.optionalReportDueAt().map(InstantFilter::copy).orElse(null);
        this.reportGeneratedAt = other.optionalReportGeneratedAt().map(InstantFilter::copy).orElse(null);
        this.reportRef = other.optionalReportRef().map(StringFilter::copy).orElse(null);
        this.reportDigest = other.optionalReportDigest().map(StringFilter::copy).orElse(null);
        this.complianceVisaBy = other.optionalComplianceVisaBy().map(UUIDFilter::copy).orElse(null);
        this.complianceVisaAt = other.optionalComplianceVisaAt().map(InstantFilter::copy).orElse(null);
        this.criteriaValidatedAt = other.optionalCriteriaValidatedAt().map(InstantFilter::copy).orElse(null);
        this.validatorId = other.optionalValidatorId().map(UUIDFilter::copy).orElse(null);
        this.rawDuration = other.optionalRawDuration().map(DurationFilter::copy).orElse(null);
        this.frozenDuration = other.optionalFrozenDuration().map(DurationFilter::copy).orElse(null);
        this.imputableDelay = other.optionalImputableDelay().map(DurationFilter::copy).orElse(null);
        this.mttr = other.optionalMttr().map(DurationFilter::copy).orElse(null);
        this.takeChargeMet = other.optionalTakeChargeMet().map(BooleanFilter::copy).orElse(null);
        this.restorationMet = other.optionalRestorationMet().map(BooleanFilter::copy).orElse(null);
        this.ticketId = other.optionalTicketId().map(UUIDFilter::copy).orElse(null);
        this.matrixId = other.optionalMatrixId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public IncidentCriteria copy() {
        return new IncidentCriteria(this);
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

    public IntegerFilter getImpact() {
        return impact;
    }

    public Optional<IntegerFilter> optionalImpact() {
        return Optional.ofNullable(impact);
    }

    public IntegerFilter impact() {
        if (impact == null) {
            setImpact(new IntegerFilter());
        }
        return impact;
    }

    public void setImpact(IntegerFilter impact) {
        this.impact = impact;
    }

    public IntegerFilter getUrgency() {
        return urgency;
    }

    public Optional<IntegerFilter> optionalUrgency() {
        return Optional.ofNullable(urgency);
    }

    public IntegerFilter urgency() {
        if (urgency == null) {
            setUrgency(new IntegerFilter());
        }
        return urgency;
    }

    public void setUrgency(IntegerFilter urgency) {
        this.urgency = urgency;
    }

    public IntegerFilter getScore() {
        return score;
    }

    public Optional<IntegerFilter> optionalScore() {
        return Optional.ofNullable(score);
    }

    public IntegerFilter score() {
        if (score == null) {
            setScore(new IntegerFilter());
        }
        return score;
    }

    public void setScore(IntegerFilter score) {
        this.score = score;
    }

    public SeverityFilter getSeverity() {
        return severity;
    }

    public Optional<SeverityFilter> optionalSeverity() {
        return Optional.ofNullable(severity);
    }

    public SeverityFilter severity() {
        if (severity == null) {
            setSeverity(new SeverityFilter());
        }
        return severity;
    }

    public void setSeverity(SeverityFilter severity) {
        this.severity = severity;
    }

    public BooleanFilter getSecurityIncident() {
        return securityIncident;
    }

    public Optional<BooleanFilter> optionalSecurityIncident() {
        return Optional.ofNullable(securityIncident);
    }

    public BooleanFilter securityIncident() {
        if (securityIncident == null) {
            setSecurityIncident(new BooleanFilter());
        }
        return securityIncident;
    }

    public void setSecurityIncident(BooleanFilter securityIncident) {
        this.securityIncident = securityIncident;
    }

    public BooleanFilter getDataCompromiseSuspected() {
        return dataCompromiseSuspected;
    }

    public Optional<BooleanFilter> optionalDataCompromiseSuspected() {
        return Optional.ofNullable(dataCompromiseSuspected);
    }

    public BooleanFilter dataCompromiseSuspected() {
        if (dataCompromiseSuspected == null) {
            setDataCompromiseSuspected(new BooleanFilter());
        }
        return dataCompromiseSuspected;
    }

    public void setDataCompromiseSuspected(BooleanFilter dataCompromiseSuspected) {
        this.dataCompromiseSuspected = dataCompromiseSuspected;
    }

    public BooleanFilter getRegulatoryFlag() {
        return regulatoryFlag;
    }

    public Optional<BooleanFilter> optionalRegulatoryFlag() {
        return Optional.ofNullable(regulatoryFlag);
    }

    public BooleanFilter regulatoryFlag() {
        if (regulatoryFlag == null) {
            setRegulatoryFlag(new BooleanFilter());
        }
        return regulatoryFlag;
    }

    public void setRegulatoryFlag(BooleanFilter regulatoryFlag) {
        this.regulatoryFlag = regulatoryFlag;
    }

    public InstantFilter getQualifiedAt() {
        return qualifiedAt;
    }

    public Optional<InstantFilter> optionalQualifiedAt() {
        return Optional.ofNullable(qualifiedAt);
    }

    public InstantFilter qualifiedAt() {
        if (qualifiedAt == null) {
            setQualifiedAt(new InstantFilter());
        }
        return qualifiedAt;
    }

    public void setQualifiedAt(InstantFilter qualifiedAt) {
        this.qualifiedAt = qualifiedAt;
    }

    public UUIDFilter getQualifiedBy() {
        return qualifiedBy;
    }

    public Optional<UUIDFilter> optionalQualifiedBy() {
        return Optional.ofNullable(qualifiedBy);
    }

    public UUIDFilter qualifiedBy() {
        if (qualifiedBy == null) {
            setQualifiedBy(new UUIDFilter());
        }
        return qualifiedBy;
    }

    public void setQualifiedBy(UUIDFilter qualifiedBy) {
        this.qualifiedBy = qualifiedBy;
    }

    public StringFilter getRootCause() {
        return rootCause;
    }

    public Optional<StringFilter> optionalRootCause() {
        return Optional.ofNullable(rootCause);
    }

    public StringFilter rootCause() {
        if (rootCause == null) {
            setRootCause(new StringFilter());
        }
        return rootCause;
    }

    public void setRootCause(StringFilter rootCause) {
        this.rootCause = rootCause;
    }

    public StringFilter getRootCauseCategory() {
        return rootCauseCategory;
    }

    public Optional<StringFilter> optionalRootCauseCategory() {
        return Optional.ofNullable(rootCauseCategory);
    }

    public StringFilter rootCauseCategory() {
        if (rootCauseCategory == null) {
            setRootCauseCategory(new StringFilter());
        }
        return rootCauseCategory;
    }

    public void setRootCauseCategory(StringFilter rootCauseCategory) {
        this.rootCauseCategory = rootCauseCategory;
    }

    public StringFilter getRootCauseProofRef() {
        return rootCauseProofRef;
    }

    public Optional<StringFilter> optionalRootCauseProofRef() {
        return Optional.ofNullable(rootCauseProofRef);
    }

    public StringFilter rootCauseProofRef() {
        if (rootCauseProofRef == null) {
            setRootCauseProofRef(new StringFilter());
        }
        return rootCauseProofRef;
    }

    public void setRootCauseProofRef(StringFilter rootCauseProofRef) {
        this.rootCauseProofRef = rootCauseProofRef;
    }

    public StringFilter getResolutionTest() {
        return resolutionTest;
    }

    public Optional<StringFilter> optionalResolutionTest() {
        return Optional.ofNullable(resolutionTest);
    }

    public StringFilter resolutionTest() {
        if (resolutionTest == null) {
            setResolutionTest(new StringFilter());
        }
        return resolutionTest;
    }

    public void setResolutionTest(StringFilter resolutionTest) {
        this.resolutionTest = resolutionTest;
    }

    public BooleanFilter getResolutionTestPassed() {
        return resolutionTestPassed;
    }

    public Optional<BooleanFilter> optionalResolutionTestPassed() {
        return Optional.ofNullable(resolutionTestPassed);
    }

    public BooleanFilter resolutionTestPassed() {
        if (resolutionTestPassed == null) {
            setResolutionTestPassed(new BooleanFilter());
        }
        return resolutionTestPassed;
    }

    public void setResolutionTestPassed(BooleanFilter resolutionTestPassed) {
        this.resolutionTestPassed = resolutionTestPassed;
    }

    public UUIDFilter getResolutionTestAuthorId() {
        return resolutionTestAuthorId;
    }

    public Optional<UUIDFilter> optionalResolutionTestAuthorId() {
        return Optional.ofNullable(resolutionTestAuthorId);
    }

    public UUIDFilter resolutionTestAuthorId() {
        if (resolutionTestAuthorId == null) {
            setResolutionTestAuthorId(new UUIDFilter());
        }
        return resolutionTestAuthorId;
    }

    public void setResolutionTestAuthorId(UUIDFilter resolutionTestAuthorId) {
        this.resolutionTestAuthorId = resolutionTestAuthorId;
    }

    public InstantFilter getResolutionTestAt() {
        return resolutionTestAt;
    }

    public Optional<InstantFilter> optionalResolutionTestAt() {
        return Optional.ofNullable(resolutionTestAt);
    }

    public InstantFilter resolutionTestAt() {
        if (resolutionTestAt == null) {
            setResolutionTestAt(new InstantFilter());
        }
        return resolutionTestAt;
    }

    public void setResolutionTestAt(InstantFilter resolutionTestAt) {
        this.resolutionTestAt = resolutionTestAt;
    }

    public StringFilter getResolutionTestProofRef() {
        return resolutionTestProofRef;
    }

    public Optional<StringFilter> optionalResolutionTestProofRef() {
        return Optional.ofNullable(resolutionTestProofRef);
    }

    public StringFilter resolutionTestProofRef() {
        if (resolutionTestProofRef == null) {
            setResolutionTestProofRef(new StringFilter());
        }
        return resolutionTestProofRef;
    }

    public void setResolutionTestProofRef(StringFilter resolutionTestProofRef) {
        this.resolutionTestProofRef = resolutionTestProofRef;
    }

    public StringFilter getLessonsLearnedRef() {
        return lessonsLearnedRef;
    }

    public Optional<StringFilter> optionalLessonsLearnedRef() {
        return Optional.ofNullable(lessonsLearnedRef);
    }

    public StringFilter lessonsLearnedRef() {
        if (lessonsLearnedRef == null) {
            setLessonsLearnedRef(new StringFilter());
        }
        return lessonsLearnedRef;
    }

    public void setLessonsLearnedRef(StringFilter lessonsLearnedRef) {
        this.lessonsLearnedRef = lessonsLearnedRef;
    }

    public StringFilter getLessonsLearnedWaiver() {
        return lessonsLearnedWaiver;
    }

    public Optional<StringFilter> optionalLessonsLearnedWaiver() {
        return Optional.ofNullable(lessonsLearnedWaiver);
    }

    public StringFilter lessonsLearnedWaiver() {
        if (lessonsLearnedWaiver == null) {
            setLessonsLearnedWaiver(new StringFilter());
        }
        return lessonsLearnedWaiver;
    }

    public void setLessonsLearnedWaiver(StringFilter lessonsLearnedWaiver) {
        this.lessonsLearnedWaiver = lessonsLearnedWaiver;
    }

    public InstantFilter getReportDueAt() {
        return reportDueAt;
    }

    public Optional<InstantFilter> optionalReportDueAt() {
        return Optional.ofNullable(reportDueAt);
    }

    public InstantFilter reportDueAt() {
        if (reportDueAt == null) {
            setReportDueAt(new InstantFilter());
        }
        return reportDueAt;
    }

    public void setReportDueAt(InstantFilter reportDueAt) {
        this.reportDueAt = reportDueAt;
    }

    public InstantFilter getReportGeneratedAt() {
        return reportGeneratedAt;
    }

    public Optional<InstantFilter> optionalReportGeneratedAt() {
        return Optional.ofNullable(reportGeneratedAt);
    }

    public InstantFilter reportGeneratedAt() {
        if (reportGeneratedAt == null) {
            setReportGeneratedAt(new InstantFilter());
        }
        return reportGeneratedAt;
    }

    public void setReportGeneratedAt(InstantFilter reportGeneratedAt) {
        this.reportGeneratedAt = reportGeneratedAt;
    }

    public StringFilter getReportRef() {
        return reportRef;
    }

    public Optional<StringFilter> optionalReportRef() {
        return Optional.ofNullable(reportRef);
    }

    public StringFilter reportRef() {
        if (reportRef == null) {
            setReportRef(new StringFilter());
        }
        return reportRef;
    }

    public void setReportRef(StringFilter reportRef) {
        this.reportRef = reportRef;
    }

    public StringFilter getReportDigest() {
        return reportDigest;
    }

    public Optional<StringFilter> optionalReportDigest() {
        return Optional.ofNullable(reportDigest);
    }

    public StringFilter reportDigest() {
        if (reportDigest == null) {
            setReportDigest(new StringFilter());
        }
        return reportDigest;
    }

    public void setReportDigest(StringFilter reportDigest) {
        this.reportDigest = reportDigest;
    }

    public UUIDFilter getComplianceVisaBy() {
        return complianceVisaBy;
    }

    public Optional<UUIDFilter> optionalComplianceVisaBy() {
        return Optional.ofNullable(complianceVisaBy);
    }

    public UUIDFilter complianceVisaBy() {
        if (complianceVisaBy == null) {
            setComplianceVisaBy(new UUIDFilter());
        }
        return complianceVisaBy;
    }

    public void setComplianceVisaBy(UUIDFilter complianceVisaBy) {
        this.complianceVisaBy = complianceVisaBy;
    }

    public InstantFilter getComplianceVisaAt() {
        return complianceVisaAt;
    }

    public Optional<InstantFilter> optionalComplianceVisaAt() {
        return Optional.ofNullable(complianceVisaAt);
    }

    public InstantFilter complianceVisaAt() {
        if (complianceVisaAt == null) {
            setComplianceVisaAt(new InstantFilter());
        }
        return complianceVisaAt;
    }

    public void setComplianceVisaAt(InstantFilter complianceVisaAt) {
        this.complianceVisaAt = complianceVisaAt;
    }

    public InstantFilter getCriteriaValidatedAt() {
        return criteriaValidatedAt;
    }

    public Optional<InstantFilter> optionalCriteriaValidatedAt() {
        return Optional.ofNullable(criteriaValidatedAt);
    }

    public InstantFilter criteriaValidatedAt() {
        if (criteriaValidatedAt == null) {
            setCriteriaValidatedAt(new InstantFilter());
        }
        return criteriaValidatedAt;
    }

    public void setCriteriaValidatedAt(InstantFilter criteriaValidatedAt) {
        this.criteriaValidatedAt = criteriaValidatedAt;
    }

    public UUIDFilter getValidatorId() {
        return validatorId;
    }

    public Optional<UUIDFilter> optionalValidatorId() {
        return Optional.ofNullable(validatorId);
    }

    public UUIDFilter validatorId() {
        if (validatorId == null) {
            setValidatorId(new UUIDFilter());
        }
        return validatorId;
    }

    public void setValidatorId(UUIDFilter validatorId) {
        this.validatorId = validatorId;
    }

    public DurationFilter getRawDuration() {
        return rawDuration;
    }

    public Optional<DurationFilter> optionalRawDuration() {
        return Optional.ofNullable(rawDuration);
    }

    public DurationFilter rawDuration() {
        if (rawDuration == null) {
            setRawDuration(new DurationFilter());
        }
        return rawDuration;
    }

    public void setRawDuration(DurationFilter rawDuration) {
        this.rawDuration = rawDuration;
    }

    public DurationFilter getFrozenDuration() {
        return frozenDuration;
    }

    public Optional<DurationFilter> optionalFrozenDuration() {
        return Optional.ofNullable(frozenDuration);
    }

    public DurationFilter frozenDuration() {
        if (frozenDuration == null) {
            setFrozenDuration(new DurationFilter());
        }
        return frozenDuration;
    }

    public void setFrozenDuration(DurationFilter frozenDuration) {
        this.frozenDuration = frozenDuration;
    }

    public DurationFilter getImputableDelay() {
        return imputableDelay;
    }

    public Optional<DurationFilter> optionalImputableDelay() {
        return Optional.ofNullable(imputableDelay);
    }

    public DurationFilter imputableDelay() {
        if (imputableDelay == null) {
            setImputableDelay(new DurationFilter());
        }
        return imputableDelay;
    }

    public void setImputableDelay(DurationFilter imputableDelay) {
        this.imputableDelay = imputableDelay;
    }

    public DurationFilter getMttr() {
        return mttr;
    }

    public Optional<DurationFilter> optionalMttr() {
        return Optional.ofNullable(mttr);
    }

    public DurationFilter mttr() {
        if (mttr == null) {
            setMttr(new DurationFilter());
        }
        return mttr;
    }

    public void setMttr(DurationFilter mttr) {
        this.mttr = mttr;
    }

    public BooleanFilter getTakeChargeMet() {
        return takeChargeMet;
    }

    public Optional<BooleanFilter> optionalTakeChargeMet() {
        return Optional.ofNullable(takeChargeMet);
    }

    public BooleanFilter takeChargeMet() {
        if (takeChargeMet == null) {
            setTakeChargeMet(new BooleanFilter());
        }
        return takeChargeMet;
    }

    public void setTakeChargeMet(BooleanFilter takeChargeMet) {
        this.takeChargeMet = takeChargeMet;
    }

    public BooleanFilter getRestorationMet() {
        return restorationMet;
    }

    public Optional<BooleanFilter> optionalRestorationMet() {
        return Optional.ofNullable(restorationMet);
    }

    public BooleanFilter restorationMet() {
        if (restorationMet == null) {
            setRestorationMet(new BooleanFilter());
        }
        return restorationMet;
    }

    public void setRestorationMet(BooleanFilter restorationMet) {
        this.restorationMet = restorationMet;
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

    public UUIDFilter getMatrixId() {
        return matrixId;
    }

    public Optional<UUIDFilter> optionalMatrixId() {
        return Optional.ofNullable(matrixId);
    }

    public UUIDFilter matrixId() {
        if (matrixId == null) {
            setMatrixId(new UUIDFilter());
        }
        return matrixId;
    }

    public void setMatrixId(UUIDFilter matrixId) {
        this.matrixId = matrixId;
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
        final IncidentCriteria that = (IncidentCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(impact, that.impact) &&
            Objects.equals(urgency, that.urgency) &&
            Objects.equals(score, that.score) &&
            Objects.equals(severity, that.severity) &&
            Objects.equals(securityIncident, that.securityIncident) &&
            Objects.equals(dataCompromiseSuspected, that.dataCompromiseSuspected) &&
            Objects.equals(regulatoryFlag, that.regulatoryFlag) &&
            Objects.equals(qualifiedAt, that.qualifiedAt) &&
            Objects.equals(qualifiedBy, that.qualifiedBy) &&
            Objects.equals(rootCause, that.rootCause) &&
            Objects.equals(rootCauseCategory, that.rootCauseCategory) &&
            Objects.equals(rootCauseProofRef, that.rootCauseProofRef) &&
            Objects.equals(resolutionTest, that.resolutionTest) &&
            Objects.equals(resolutionTestPassed, that.resolutionTestPassed) &&
            Objects.equals(resolutionTestAuthorId, that.resolutionTestAuthorId) &&
            Objects.equals(resolutionTestAt, that.resolutionTestAt) &&
            Objects.equals(resolutionTestProofRef, that.resolutionTestProofRef) &&
            Objects.equals(lessonsLearnedRef, that.lessonsLearnedRef) &&
            Objects.equals(lessonsLearnedWaiver, that.lessonsLearnedWaiver) &&
            Objects.equals(reportDueAt, that.reportDueAt) &&
            Objects.equals(reportGeneratedAt, that.reportGeneratedAt) &&
            Objects.equals(reportRef, that.reportRef) &&
            Objects.equals(reportDigest, that.reportDigest) &&
            Objects.equals(complianceVisaBy, that.complianceVisaBy) &&
            Objects.equals(complianceVisaAt, that.complianceVisaAt) &&
            Objects.equals(criteriaValidatedAt, that.criteriaValidatedAt) &&
            Objects.equals(validatorId, that.validatorId) &&
            Objects.equals(rawDuration, that.rawDuration) &&
            Objects.equals(frozenDuration, that.frozenDuration) &&
            Objects.equals(imputableDelay, that.imputableDelay) &&
            Objects.equals(mttr, that.mttr) &&
            Objects.equals(takeChargeMet, that.takeChargeMet) &&
            Objects.equals(restorationMet, that.restorationMet) &&
            Objects.equals(ticketId, that.ticketId) &&
            Objects.equals(matrixId, that.matrixId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            impact,
            urgency,
            score,
            severity,
            securityIncident,
            dataCompromiseSuspected,
            regulatoryFlag,
            qualifiedAt,
            qualifiedBy,
            rootCause,
            rootCauseCategory,
            rootCauseProofRef,
            resolutionTest,
            resolutionTestPassed,
            resolutionTestAuthorId,
            resolutionTestAt,
            resolutionTestProofRef,
            lessonsLearnedRef,
            lessonsLearnedWaiver,
            reportDueAt,
            reportGeneratedAt,
            reportRef,
            reportDigest,
            complianceVisaBy,
            complianceVisaAt,
            criteriaValidatedAt,
            validatorId,
            rawDuration,
            frozenDuration,
            imputableDelay,
            mttr,
            takeChargeMet,
            restorationMet,
            ticketId,
            matrixId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IncidentCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalImpact().map(f -> "impact=" + f + ", ").orElse("") +
            optionalUrgency().map(f -> "urgency=" + f + ", ").orElse("") +
            optionalScore().map(f -> "score=" + f + ", ").orElse("") +
            optionalSeverity().map(f -> "severity=" + f + ", ").orElse("") +
            optionalSecurityIncident().map(f -> "securityIncident=" + f + ", ").orElse("") +
            optionalDataCompromiseSuspected().map(f -> "dataCompromiseSuspected=" + f + ", ").orElse("") +
            optionalRegulatoryFlag().map(f -> "regulatoryFlag=" + f + ", ").orElse("") +
            optionalQualifiedAt().map(f -> "qualifiedAt=" + f + ", ").orElse("") +
            optionalQualifiedBy().map(f -> "qualifiedBy=" + f + ", ").orElse("") +
            optionalRootCause().map(f -> "rootCause=" + f + ", ").orElse("") +
            optionalRootCauseCategory().map(f -> "rootCauseCategory=" + f + ", ").orElse("") +
            optionalRootCauseProofRef().map(f -> "rootCauseProofRef=" + f + ", ").orElse("") +
            optionalResolutionTest().map(f -> "resolutionTest=" + f + ", ").orElse("") +
            optionalResolutionTestPassed().map(f -> "resolutionTestPassed=" + f + ", ").orElse("") +
            optionalResolutionTestAuthorId().map(f -> "resolutionTestAuthorId=" + f + ", ").orElse("") +
            optionalResolutionTestAt().map(f -> "resolutionTestAt=" + f + ", ").orElse("") +
            optionalResolutionTestProofRef().map(f -> "resolutionTestProofRef=" + f + ", ").orElse("") +
            optionalLessonsLearnedRef().map(f -> "lessonsLearnedRef=" + f + ", ").orElse("") +
            optionalLessonsLearnedWaiver().map(f -> "lessonsLearnedWaiver=" + f + ", ").orElse("") +
            optionalReportDueAt().map(f -> "reportDueAt=" + f + ", ").orElse("") +
            optionalReportGeneratedAt().map(f -> "reportGeneratedAt=" + f + ", ").orElse("") +
            optionalReportRef().map(f -> "reportRef=" + f + ", ").orElse("") +
            optionalReportDigest().map(f -> "reportDigest=" + f + ", ").orElse("") +
            optionalComplianceVisaBy().map(f -> "complianceVisaBy=" + f + ", ").orElse("") +
            optionalComplianceVisaAt().map(f -> "complianceVisaAt=" + f + ", ").orElse("") +
            optionalCriteriaValidatedAt().map(f -> "criteriaValidatedAt=" + f + ", ").orElse("") +
            optionalValidatorId().map(f -> "validatorId=" + f + ", ").orElse("") +
            optionalRawDuration().map(f -> "rawDuration=" + f + ", ").orElse("") +
            optionalFrozenDuration().map(f -> "frozenDuration=" + f + ", ").orElse("") +
            optionalImputableDelay().map(f -> "imputableDelay=" + f + ", ").orElse("") +
            optionalMttr().map(f -> "mttr=" + f + ", ").orElse("") +
            optionalTakeChargeMet().map(f -> "takeChargeMet=" + f + ", ").orElse("") +
            optionalRestorationMet().map(f -> "restorationMet=" + f + ", ").orElse("") +
            optionalTicketId().map(f -> "ticketId=" + f + ", ").orElse("") +
            optionalMatrixId().map(f -> "matrixId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
