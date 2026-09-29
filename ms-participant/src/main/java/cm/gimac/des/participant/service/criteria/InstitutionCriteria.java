package cm.gimac.des.participant.service.criteria;

import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link cm.gimac.des.participant.domain.Institution} entity. This class is used
 * in {@link cm.gimac.des.participant.web.rest.InstitutionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /institutions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class InstitutionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering InstitutionCategory
     */
    public static class InstitutionCategoryFilter extends Filter<InstitutionCategory> {

        public InstitutionCategoryFilter() {}

        public InstitutionCategoryFilter(InstitutionCategoryFilter filter) {
            super(filter);
        }

        @Override
        public InstitutionCategoryFilter copy() {
            return new InstitutionCategoryFilter(this);
        }
    }

    /**
     * Class for filtering MembershipMode
     */
    public static class MembershipModeFilter extends Filter<MembershipMode> {

        public MembershipModeFilter() {}

        public MembershipModeFilter(MembershipModeFilter filter) {
            super(filter);
        }

        @Override
        public MembershipModeFilter copy() {
            return new MembershipModeFilter(this);
        }
    }

    /**
     * Class for filtering InstitutionStatus
     */
    public static class InstitutionStatusFilter extends Filter<InstitutionStatus> {

        public InstitutionStatusFilter() {}

        public InstitutionStatusFilter(InstitutionStatusFilter filter) {
            super(filter);
        }

        @Override
        public InstitutionStatusFilter copy() {
            return new InstitutionStatusFilter(this);
        }
    }

    /**
     * Class for filtering ComplianceState
     */
    public static class ComplianceStateFilter extends Filter<ComplianceState> {

        public ComplianceStateFilter() {}

        public ComplianceStateFilter(ComplianceStateFilter filter) {
            super(filter);
        }

        @Override
        public ComplianceStateFilter copy() {
            return new ComplianceStateFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private StringFilter name;

    private StringFilter acronym;

    private StringFilter participantCode;

    private StringFilter identificationCode;

    private StringFilter beacBankCode;

    private StringFilter country;

    private InstitutionCategoryFilter category;

    private MembershipModeFilter membershipMode;

    private StringFilter settlementAccountRef;

    private StringFilter ringFencedAccountRef;

    private LocalDateFilter joinedOn;

    private InstitutionStatusFilter status;

    private ComplianceStateFilter complianceState;

    private InstantFilter attestationIssuedAt;

    private InstantFilter goLiveAuthorizedAt;

    private IntegerFilter openLinkCount;

    private IntegerFilter openSuspenseCount;

    private InstantFilter createdAt;

    private LocalDateFilter contactedOn;

    private StringFilter contactOrigin;

    private StringFilter intendedProject;

    private UUIDFilter ddrmOfficerId;

    private StringFilter ddrmOfficerName;

    private InstantFilter admittedAt;

    private UUIDFilter admittedById;

    private StringFilter closureReason;

    private InstantFilter closedAt;

    private UUIDFilter settlementBankId;

    private UUIDFilter sponsorBankId;

    private Boolean distinct;

    public InstitutionCriteria() {}

    public InstitutionCriteria(InstitutionCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.name = other.optionalName().map(StringFilter::copy).orElse(null);
        this.acronym = other.optionalAcronym().map(StringFilter::copy).orElse(null);
        this.participantCode = other.optionalParticipantCode().map(StringFilter::copy).orElse(null);
        this.identificationCode = other.optionalIdentificationCode().map(StringFilter::copy).orElse(null);
        this.beacBankCode = other.optionalBeacBankCode().map(StringFilter::copy).orElse(null);
        this.country = other.optionalCountry().map(StringFilter::copy).orElse(null);
        this.category = other.optionalCategory().map(InstitutionCategoryFilter::copy).orElse(null);
        this.membershipMode = other.optionalMembershipMode().map(MembershipModeFilter::copy).orElse(null);
        this.settlementAccountRef = other.optionalSettlementAccountRef().map(StringFilter::copy).orElse(null);
        this.ringFencedAccountRef = other.optionalRingFencedAccountRef().map(StringFilter::copy).orElse(null);
        this.joinedOn = other.optionalJoinedOn().map(LocalDateFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(InstitutionStatusFilter::copy).orElse(null);
        this.complianceState = other.optionalComplianceState().map(ComplianceStateFilter::copy).orElse(null);
        this.attestationIssuedAt = other.optionalAttestationIssuedAt().map(InstantFilter::copy).orElse(null);
        this.goLiveAuthorizedAt = other.optionalGoLiveAuthorizedAt().map(InstantFilter::copy).orElse(null);
        this.openLinkCount = other.optionalOpenLinkCount().map(IntegerFilter::copy).orElse(null);
        this.openSuspenseCount = other.optionalOpenSuspenseCount().map(IntegerFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.contactedOn = other.optionalContactedOn().map(LocalDateFilter::copy).orElse(null);
        this.contactOrigin = other.optionalContactOrigin().map(StringFilter::copy).orElse(null);
        this.intendedProject = other.optionalIntendedProject().map(StringFilter::copy).orElse(null);
        this.ddrmOfficerId = other.optionalDdrmOfficerId().map(UUIDFilter::copy).orElse(null);
        this.ddrmOfficerName = other.optionalDdrmOfficerName().map(StringFilter::copy).orElse(null);
        this.admittedAt = other.optionalAdmittedAt().map(InstantFilter::copy).orElse(null);
        this.admittedById = other.optionalAdmittedById().map(UUIDFilter::copy).orElse(null);
        this.closureReason = other.optionalClosureReason().map(StringFilter::copy).orElse(null);
        this.closedAt = other.optionalClosedAt().map(InstantFilter::copy).orElse(null);
        this.settlementBankId = other.optionalSettlementBankId().map(UUIDFilter::copy).orElse(null);
        this.sponsorBankId = other.optionalSponsorBankId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public InstitutionCriteria copy() {
        return new InstitutionCriteria(this);
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

    public StringFilter getName() {
        return name;
    }

    public Optional<StringFilter> optionalName() {
        return Optional.ofNullable(name);
    }

    public StringFilter name() {
        if (name == null) {
            setName(new StringFilter());
        }
        return name;
    }

    public void setName(StringFilter name) {
        this.name = name;
    }

    public StringFilter getAcronym() {
        return acronym;
    }

    public Optional<StringFilter> optionalAcronym() {
        return Optional.ofNullable(acronym);
    }

    public StringFilter acronym() {
        if (acronym == null) {
            setAcronym(new StringFilter());
        }
        return acronym;
    }

    public void setAcronym(StringFilter acronym) {
        this.acronym = acronym;
    }

    public StringFilter getParticipantCode() {
        return participantCode;
    }

    public Optional<StringFilter> optionalParticipantCode() {
        return Optional.ofNullable(participantCode);
    }

    public StringFilter participantCode() {
        if (participantCode == null) {
            setParticipantCode(new StringFilter());
        }
        return participantCode;
    }

    public void setParticipantCode(StringFilter participantCode) {
        this.participantCode = participantCode;
    }

    public StringFilter getIdentificationCode() {
        return identificationCode;
    }

    public Optional<StringFilter> optionalIdentificationCode() {
        return Optional.ofNullable(identificationCode);
    }

    public StringFilter identificationCode() {
        if (identificationCode == null) {
            setIdentificationCode(new StringFilter());
        }
        return identificationCode;
    }

    public void setIdentificationCode(StringFilter identificationCode) {
        this.identificationCode = identificationCode;
    }

    public StringFilter getBeacBankCode() {
        return beacBankCode;
    }

    public Optional<StringFilter> optionalBeacBankCode() {
        return Optional.ofNullable(beacBankCode);
    }

    public StringFilter beacBankCode() {
        if (beacBankCode == null) {
            setBeacBankCode(new StringFilter());
        }
        return beacBankCode;
    }

    public void setBeacBankCode(StringFilter beacBankCode) {
        this.beacBankCode = beacBankCode;
    }

    public StringFilter getCountry() {
        return country;
    }

    public Optional<StringFilter> optionalCountry() {
        return Optional.ofNullable(country);
    }

    public StringFilter country() {
        if (country == null) {
            setCountry(new StringFilter());
        }
        return country;
    }

    public void setCountry(StringFilter country) {
        this.country = country;
    }

    public InstitutionCategoryFilter getCategory() {
        return category;
    }

    public Optional<InstitutionCategoryFilter> optionalCategory() {
        return Optional.ofNullable(category);
    }

    public InstitutionCategoryFilter category() {
        if (category == null) {
            setCategory(new InstitutionCategoryFilter());
        }
        return category;
    }

    public void setCategory(InstitutionCategoryFilter category) {
        this.category = category;
    }

    public MembershipModeFilter getMembershipMode() {
        return membershipMode;
    }

    public Optional<MembershipModeFilter> optionalMembershipMode() {
        return Optional.ofNullable(membershipMode);
    }

    public MembershipModeFilter membershipMode() {
        if (membershipMode == null) {
            setMembershipMode(new MembershipModeFilter());
        }
        return membershipMode;
    }

    public void setMembershipMode(MembershipModeFilter membershipMode) {
        this.membershipMode = membershipMode;
    }

    public StringFilter getSettlementAccountRef() {
        return settlementAccountRef;
    }

    public Optional<StringFilter> optionalSettlementAccountRef() {
        return Optional.ofNullable(settlementAccountRef);
    }

    public StringFilter settlementAccountRef() {
        if (settlementAccountRef == null) {
            setSettlementAccountRef(new StringFilter());
        }
        return settlementAccountRef;
    }

    public void setSettlementAccountRef(StringFilter settlementAccountRef) {
        this.settlementAccountRef = settlementAccountRef;
    }

    public StringFilter getRingFencedAccountRef() {
        return ringFencedAccountRef;
    }

    public Optional<StringFilter> optionalRingFencedAccountRef() {
        return Optional.ofNullable(ringFencedAccountRef);
    }

    public StringFilter ringFencedAccountRef() {
        if (ringFencedAccountRef == null) {
            setRingFencedAccountRef(new StringFilter());
        }
        return ringFencedAccountRef;
    }

    public void setRingFencedAccountRef(StringFilter ringFencedAccountRef) {
        this.ringFencedAccountRef = ringFencedAccountRef;
    }

    public LocalDateFilter getJoinedOn() {
        return joinedOn;
    }

    public Optional<LocalDateFilter> optionalJoinedOn() {
        return Optional.ofNullable(joinedOn);
    }

    public LocalDateFilter joinedOn() {
        if (joinedOn == null) {
            setJoinedOn(new LocalDateFilter());
        }
        return joinedOn;
    }

    public void setJoinedOn(LocalDateFilter joinedOn) {
        this.joinedOn = joinedOn;
    }

    public InstitutionStatusFilter getStatus() {
        return status;
    }

    public Optional<InstitutionStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public InstitutionStatusFilter status() {
        if (status == null) {
            setStatus(new InstitutionStatusFilter());
        }
        return status;
    }

    public void setStatus(InstitutionStatusFilter status) {
        this.status = status;
    }

    public ComplianceStateFilter getComplianceState() {
        return complianceState;
    }

    public Optional<ComplianceStateFilter> optionalComplianceState() {
        return Optional.ofNullable(complianceState);
    }

    public ComplianceStateFilter complianceState() {
        if (complianceState == null) {
            setComplianceState(new ComplianceStateFilter());
        }
        return complianceState;
    }

    public void setComplianceState(ComplianceStateFilter complianceState) {
        this.complianceState = complianceState;
    }

    public InstantFilter getAttestationIssuedAt() {
        return attestationIssuedAt;
    }

    public Optional<InstantFilter> optionalAttestationIssuedAt() {
        return Optional.ofNullable(attestationIssuedAt);
    }

    public InstantFilter attestationIssuedAt() {
        if (attestationIssuedAt == null) {
            setAttestationIssuedAt(new InstantFilter());
        }
        return attestationIssuedAt;
    }

    public void setAttestationIssuedAt(InstantFilter attestationIssuedAt) {
        this.attestationIssuedAt = attestationIssuedAt;
    }

    public InstantFilter getGoLiveAuthorizedAt() {
        return goLiveAuthorizedAt;
    }

    public Optional<InstantFilter> optionalGoLiveAuthorizedAt() {
        return Optional.ofNullable(goLiveAuthorizedAt);
    }

    public InstantFilter goLiveAuthorizedAt() {
        if (goLiveAuthorizedAt == null) {
            setGoLiveAuthorizedAt(new InstantFilter());
        }
        return goLiveAuthorizedAt;
    }

    public void setGoLiveAuthorizedAt(InstantFilter goLiveAuthorizedAt) {
        this.goLiveAuthorizedAt = goLiveAuthorizedAt;
    }

    public IntegerFilter getOpenLinkCount() {
        return openLinkCount;
    }

    public Optional<IntegerFilter> optionalOpenLinkCount() {
        return Optional.ofNullable(openLinkCount);
    }

    public IntegerFilter openLinkCount() {
        if (openLinkCount == null) {
            setOpenLinkCount(new IntegerFilter());
        }
        return openLinkCount;
    }

    public void setOpenLinkCount(IntegerFilter openLinkCount) {
        this.openLinkCount = openLinkCount;
    }

    public IntegerFilter getOpenSuspenseCount() {
        return openSuspenseCount;
    }

    public Optional<IntegerFilter> optionalOpenSuspenseCount() {
        return Optional.ofNullable(openSuspenseCount);
    }

    public IntegerFilter openSuspenseCount() {
        if (openSuspenseCount == null) {
            setOpenSuspenseCount(new IntegerFilter());
        }
        return openSuspenseCount;
    }

    public void setOpenSuspenseCount(IntegerFilter openSuspenseCount) {
        this.openSuspenseCount = openSuspenseCount;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateFilter getContactedOn() {
        return contactedOn;
    }

    public Optional<LocalDateFilter> optionalContactedOn() {
        return Optional.ofNullable(contactedOn);
    }

    public LocalDateFilter contactedOn() {
        if (contactedOn == null) {
            setContactedOn(new LocalDateFilter());
        }
        return contactedOn;
    }

    public void setContactedOn(LocalDateFilter contactedOn) {
        this.contactedOn = contactedOn;
    }

    public StringFilter getContactOrigin() {
        return contactOrigin;
    }

    public Optional<StringFilter> optionalContactOrigin() {
        return Optional.ofNullable(contactOrigin);
    }

    public StringFilter contactOrigin() {
        if (contactOrigin == null) {
            setContactOrigin(new StringFilter());
        }
        return contactOrigin;
    }

    public void setContactOrigin(StringFilter contactOrigin) {
        this.contactOrigin = contactOrigin;
    }

    public StringFilter getIntendedProject() {
        return intendedProject;
    }

    public Optional<StringFilter> optionalIntendedProject() {
        return Optional.ofNullable(intendedProject);
    }

    public StringFilter intendedProject() {
        if (intendedProject == null) {
            setIntendedProject(new StringFilter());
        }
        return intendedProject;
    }

    public void setIntendedProject(StringFilter intendedProject) {
        this.intendedProject = intendedProject;
    }

    public UUIDFilter getDdrmOfficerId() {
        return ddrmOfficerId;
    }

    public Optional<UUIDFilter> optionalDdrmOfficerId() {
        return Optional.ofNullable(ddrmOfficerId);
    }

    public UUIDFilter ddrmOfficerId() {
        if (ddrmOfficerId == null) {
            setDdrmOfficerId(new UUIDFilter());
        }
        return ddrmOfficerId;
    }

    public void setDdrmOfficerId(UUIDFilter ddrmOfficerId) {
        this.ddrmOfficerId = ddrmOfficerId;
    }

    public StringFilter getDdrmOfficerName() {
        return ddrmOfficerName;
    }

    public Optional<StringFilter> optionalDdrmOfficerName() {
        return Optional.ofNullable(ddrmOfficerName);
    }

    public StringFilter ddrmOfficerName() {
        if (ddrmOfficerName == null) {
            setDdrmOfficerName(new StringFilter());
        }
        return ddrmOfficerName;
    }

    public void setDdrmOfficerName(StringFilter ddrmOfficerName) {
        this.ddrmOfficerName = ddrmOfficerName;
    }

    public InstantFilter getAdmittedAt() {
        return admittedAt;
    }

    public Optional<InstantFilter> optionalAdmittedAt() {
        return Optional.ofNullable(admittedAt);
    }

    public InstantFilter admittedAt() {
        if (admittedAt == null) {
            setAdmittedAt(new InstantFilter());
        }
        return admittedAt;
    }

    public void setAdmittedAt(InstantFilter admittedAt) {
        this.admittedAt = admittedAt;
    }

    public UUIDFilter getAdmittedById() {
        return admittedById;
    }

    public Optional<UUIDFilter> optionalAdmittedById() {
        return Optional.ofNullable(admittedById);
    }

    public UUIDFilter admittedById() {
        if (admittedById == null) {
            setAdmittedById(new UUIDFilter());
        }
        return admittedById;
    }

    public void setAdmittedById(UUIDFilter admittedById) {
        this.admittedById = admittedById;
    }

    public StringFilter getClosureReason() {
        return closureReason;
    }

    public Optional<StringFilter> optionalClosureReason() {
        return Optional.ofNullable(closureReason);
    }

    public StringFilter closureReason() {
        if (closureReason == null) {
            setClosureReason(new StringFilter());
        }
        return closureReason;
    }

    public void setClosureReason(StringFilter closureReason) {
        this.closureReason = closureReason;
    }

    public InstantFilter getClosedAt() {
        return closedAt;
    }

    public Optional<InstantFilter> optionalClosedAt() {
        return Optional.ofNullable(closedAt);
    }

    public InstantFilter closedAt() {
        if (closedAt == null) {
            setClosedAt(new InstantFilter());
        }
        return closedAt;
    }

    public void setClosedAt(InstantFilter closedAt) {
        this.closedAt = closedAt;
    }

    public UUIDFilter getSettlementBankId() {
        return settlementBankId;
    }

    public Optional<UUIDFilter> optionalSettlementBankId() {
        return Optional.ofNullable(settlementBankId);
    }

    public UUIDFilter settlementBankId() {
        if (settlementBankId == null) {
            setSettlementBankId(new UUIDFilter());
        }
        return settlementBankId;
    }

    public void setSettlementBankId(UUIDFilter settlementBankId) {
        this.settlementBankId = settlementBankId;
    }

    public UUIDFilter getSponsorBankId() {
        return sponsorBankId;
    }

    public Optional<UUIDFilter> optionalSponsorBankId() {
        return Optional.ofNullable(sponsorBankId);
    }

    public UUIDFilter sponsorBankId() {
        if (sponsorBankId == null) {
            setSponsorBankId(new UUIDFilter());
        }
        return sponsorBankId;
    }

    public void setSponsorBankId(UUIDFilter sponsorBankId) {
        this.sponsorBankId = sponsorBankId;
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
        final InstitutionCriteria that = (InstitutionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(acronym, that.acronym) &&
            Objects.equals(participantCode, that.participantCode) &&
            Objects.equals(identificationCode, that.identificationCode) &&
            Objects.equals(beacBankCode, that.beacBankCode) &&
            Objects.equals(country, that.country) &&
            Objects.equals(category, that.category) &&
            Objects.equals(membershipMode, that.membershipMode) &&
            Objects.equals(settlementAccountRef, that.settlementAccountRef) &&
            Objects.equals(ringFencedAccountRef, that.ringFencedAccountRef) &&
            Objects.equals(joinedOn, that.joinedOn) &&
            Objects.equals(status, that.status) &&
            Objects.equals(complianceState, that.complianceState) &&
            Objects.equals(attestationIssuedAt, that.attestationIssuedAt) &&
            Objects.equals(goLiveAuthorizedAt, that.goLiveAuthorizedAt) &&
            Objects.equals(openLinkCount, that.openLinkCount) &&
            Objects.equals(openSuspenseCount, that.openSuspenseCount) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(contactedOn, that.contactedOn) &&
            Objects.equals(contactOrigin, that.contactOrigin) &&
            Objects.equals(intendedProject, that.intendedProject) &&
            Objects.equals(ddrmOfficerId, that.ddrmOfficerId) &&
            Objects.equals(ddrmOfficerName, that.ddrmOfficerName) &&
            Objects.equals(admittedAt, that.admittedAt) &&
            Objects.equals(admittedById, that.admittedById) &&
            Objects.equals(closureReason, that.closureReason) &&
            Objects.equals(closedAt, that.closedAt) &&
            Objects.equals(settlementBankId, that.settlementBankId) &&
            Objects.equals(sponsorBankId, that.sponsorBankId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            name,
            acronym,
            participantCode,
            identificationCode,
            beacBankCode,
            country,
            category,
            membershipMode,
            settlementAccountRef,
            ringFencedAccountRef,
            joinedOn,
            status,
            complianceState,
            attestationIssuedAt,
            goLiveAuthorizedAt,
            openLinkCount,
            openSuspenseCount,
            createdAt,
            contactedOn,
            contactOrigin,
            intendedProject,
            ddrmOfficerId,
            ddrmOfficerName,
            admittedAt,
            admittedById,
            closureReason,
            closedAt,
            settlementBankId,
            sponsorBankId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "InstitutionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalName().map(f -> "name=" + f + ", ").orElse("") +
            optionalAcronym().map(f -> "acronym=" + f + ", ").orElse("") +
            optionalParticipantCode().map(f -> "participantCode=" + f + ", ").orElse("") +
            optionalIdentificationCode().map(f -> "identificationCode=" + f + ", ").orElse("") +
            optionalBeacBankCode().map(f -> "beacBankCode=" + f + ", ").orElse("") +
            optionalCountry().map(f -> "country=" + f + ", ").orElse("") +
            optionalCategory().map(f -> "category=" + f + ", ").orElse("") +
            optionalMembershipMode().map(f -> "membershipMode=" + f + ", ").orElse("") +
            optionalSettlementAccountRef().map(f -> "settlementAccountRef=" + f + ", ").orElse("") +
            optionalRingFencedAccountRef().map(f -> "ringFencedAccountRef=" + f + ", ").orElse("") +
            optionalJoinedOn().map(f -> "joinedOn=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalComplianceState().map(f -> "complianceState=" + f + ", ").orElse("") +
            optionalAttestationIssuedAt().map(f -> "attestationIssuedAt=" + f + ", ").orElse("") +
            optionalGoLiveAuthorizedAt().map(f -> "goLiveAuthorizedAt=" + f + ", ").orElse("") +
            optionalOpenLinkCount().map(f -> "openLinkCount=" + f + ", ").orElse("") +
            optionalOpenSuspenseCount().map(f -> "openSuspenseCount=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalContactedOn().map(f -> "contactedOn=" + f + ", ").orElse("") +
            optionalContactOrigin().map(f -> "contactOrigin=" + f + ", ").orElse("") +
            optionalIntendedProject().map(f -> "intendedProject=" + f + ", ").orElse("") +
            optionalDdrmOfficerId().map(f -> "ddrmOfficerId=" + f + ", ").orElse("") +
            optionalDdrmOfficerName().map(f -> "ddrmOfficerName=" + f + ", ").orElse("") +
            optionalAdmittedAt().map(f -> "admittedAt=" + f + ", ").orElse("") +
            optionalAdmittedById().map(f -> "admittedById=" + f + ", ").orElse("") +
            optionalClosureReason().map(f -> "closureReason=" + f + ", ").orElse("") +
            optionalClosedAt().map(f -> "closedAt=" + f + ", ").orElse("") +
            optionalSettlementBankId().map(f -> "settlementBankId=" + f + ", ").orElse("") +
            optionalSponsorBankId().map(f -> "sponsorBankId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
