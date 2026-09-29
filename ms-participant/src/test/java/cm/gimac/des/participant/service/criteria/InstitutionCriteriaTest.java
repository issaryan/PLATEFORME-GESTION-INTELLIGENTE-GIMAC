package cm.gimac.des.participant.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class InstitutionCriteriaTest {

    @Test
    void newInstitutionCriteriaHasAllFiltersNullTest() {
        var institutionCriteria = new InstitutionCriteria();
        assertThat(institutionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void institutionCriteriaFluentMethodsCreatesFiltersTest() {
        var institutionCriteria = new InstitutionCriteria();

        setAllFilters(institutionCriteria);

        assertThat(institutionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void institutionCriteriaCopyCreatesNullFilterTest() {
        var institutionCriteria = new InstitutionCriteria();
        var copy = institutionCriteria.copy();

        assertThat(institutionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(institutionCriteria)
        );
    }

    @Test
    void institutionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var institutionCriteria = new InstitutionCriteria();
        setAllFilters(institutionCriteria);

        var copy = institutionCriteria.copy();

        assertThat(institutionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(institutionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var institutionCriteria = new InstitutionCriteria();

        assertThat(institutionCriteria).hasToString("InstitutionCriteria{}");
    }

    private static void setAllFilters(InstitutionCriteria institutionCriteria) {
        institutionCriteria.id();
        institutionCriteria.name();
        institutionCriteria.acronym();
        institutionCriteria.participantCode();
        institutionCriteria.identificationCode();
        institutionCriteria.beacBankCode();
        institutionCriteria.country();
        institutionCriteria.category();
        institutionCriteria.membershipMode();
        institutionCriteria.settlementAccountRef();
        institutionCriteria.ringFencedAccountRef();
        institutionCriteria.joinedOn();
        institutionCriteria.status();
        institutionCriteria.complianceState();
        institutionCriteria.attestationIssuedAt();
        institutionCriteria.goLiveAuthorizedAt();
        institutionCriteria.openLinkCount();
        institutionCriteria.openSuspenseCount();
        institutionCriteria.createdAt();
        institutionCriteria.contactedOn();
        institutionCriteria.contactOrigin();
        institutionCriteria.intendedProject();
        institutionCriteria.ddrmOfficerId();
        institutionCriteria.ddrmOfficerName();
        institutionCriteria.admittedAt();
        institutionCriteria.admittedById();
        institutionCriteria.closureReason();
        institutionCriteria.closedAt();
        institutionCriteria.settlementBankId();
        institutionCriteria.sponsorBankId();
        institutionCriteria.distinct();
    }

    private static Condition<InstitutionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getName()) &&
                condition.apply(criteria.getAcronym()) &&
                condition.apply(criteria.getParticipantCode()) &&
                condition.apply(criteria.getIdentificationCode()) &&
                condition.apply(criteria.getBeacBankCode()) &&
                condition.apply(criteria.getCountry()) &&
                condition.apply(criteria.getCategory()) &&
                condition.apply(criteria.getMembershipMode()) &&
                condition.apply(criteria.getSettlementAccountRef()) &&
                condition.apply(criteria.getRingFencedAccountRef()) &&
                condition.apply(criteria.getJoinedOn()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getComplianceState()) &&
                condition.apply(criteria.getAttestationIssuedAt()) &&
                condition.apply(criteria.getGoLiveAuthorizedAt()) &&
                condition.apply(criteria.getOpenLinkCount()) &&
                condition.apply(criteria.getOpenSuspenseCount()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getContactedOn()) &&
                condition.apply(criteria.getContactOrigin()) &&
                condition.apply(criteria.getIntendedProject()) &&
                condition.apply(criteria.getDdrmOfficerId()) &&
                condition.apply(criteria.getDdrmOfficerName()) &&
                condition.apply(criteria.getAdmittedAt()) &&
                condition.apply(criteria.getAdmittedById()) &&
                condition.apply(criteria.getClosureReason()) &&
                condition.apply(criteria.getClosedAt()) &&
                condition.apply(criteria.getSettlementBankId()) &&
                condition.apply(criteria.getSponsorBankId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<InstitutionCriteria> copyFiltersAre(InstitutionCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getName(), copy.getName()) &&
                condition.apply(criteria.getAcronym(), copy.getAcronym()) &&
                condition.apply(criteria.getParticipantCode(), copy.getParticipantCode()) &&
                condition.apply(criteria.getIdentificationCode(), copy.getIdentificationCode()) &&
                condition.apply(criteria.getBeacBankCode(), copy.getBeacBankCode()) &&
                condition.apply(criteria.getCountry(), copy.getCountry()) &&
                condition.apply(criteria.getCategory(), copy.getCategory()) &&
                condition.apply(criteria.getMembershipMode(), copy.getMembershipMode()) &&
                condition.apply(criteria.getSettlementAccountRef(), copy.getSettlementAccountRef()) &&
                condition.apply(criteria.getRingFencedAccountRef(), copy.getRingFencedAccountRef()) &&
                condition.apply(criteria.getJoinedOn(), copy.getJoinedOn()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getComplianceState(), copy.getComplianceState()) &&
                condition.apply(criteria.getAttestationIssuedAt(), copy.getAttestationIssuedAt()) &&
                condition.apply(criteria.getGoLiveAuthorizedAt(), copy.getGoLiveAuthorizedAt()) &&
                condition.apply(criteria.getOpenLinkCount(), copy.getOpenLinkCount()) &&
                condition.apply(criteria.getOpenSuspenseCount(), copy.getOpenSuspenseCount()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getContactedOn(), copy.getContactedOn()) &&
                condition.apply(criteria.getContactOrigin(), copy.getContactOrigin()) &&
                condition.apply(criteria.getIntendedProject(), copy.getIntendedProject()) &&
                condition.apply(criteria.getDdrmOfficerId(), copy.getDdrmOfficerId()) &&
                condition.apply(criteria.getDdrmOfficerName(), copy.getDdrmOfficerName()) &&
                condition.apply(criteria.getAdmittedAt(), copy.getAdmittedAt()) &&
                condition.apply(criteria.getAdmittedById(), copy.getAdmittedById()) &&
                condition.apply(criteria.getClosureReason(), copy.getClosureReason()) &&
                condition.apply(criteria.getClosedAt(), copy.getClosedAt()) &&
                condition.apply(criteria.getSettlementBankId(), copy.getSettlementBankId()) &&
                condition.apply(criteria.getSponsorBankId(), copy.getSponsorBankId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
