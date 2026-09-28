package cm.gimac.des.incident.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class IncidentCriteriaTest {

    @Test
    void newIncidentCriteriaHasAllFiltersNullTest() {
        var incidentCriteria = new IncidentCriteria();
        assertThat(incidentCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void incidentCriteriaFluentMethodsCreatesFiltersTest() {
        var incidentCriteria = new IncidentCriteria();

        setAllFilters(incidentCriteria);

        assertThat(incidentCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void incidentCriteriaCopyCreatesNullFilterTest() {
        var incidentCriteria = new IncidentCriteria();
        var copy = incidentCriteria.copy();

        assertThat(incidentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(incidentCriteria)
        );
    }

    @Test
    void incidentCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var incidentCriteria = new IncidentCriteria();
        setAllFilters(incidentCriteria);

        var copy = incidentCriteria.copy();

        assertThat(incidentCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(incidentCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var incidentCriteria = new IncidentCriteria();

        assertThat(incidentCriteria).hasToString("IncidentCriteria{}");
    }

    private static void setAllFilters(IncidentCriteria incidentCriteria) {
        incidentCriteria.id();
        incidentCriteria.impact();
        incidentCriteria.urgency();
        incidentCriteria.score();
        incidentCriteria.severity();
        incidentCriteria.securityIncident();
        incidentCriteria.dataCompromiseSuspected();
        incidentCriteria.regulatoryFlag();
        incidentCriteria.qualifiedAt();
        incidentCriteria.qualifiedBy();
        incidentCriteria.rootCause();
        incidentCriteria.rootCauseCategory();
        incidentCriteria.rootCauseProofRef();
        incidentCriteria.resolutionTest();
        incidentCriteria.resolutionTestPassed();
        incidentCriteria.resolutionTestAuthorId();
        incidentCriteria.resolutionTestAt();
        incidentCriteria.resolutionTestProofRef();
        incidentCriteria.lessonsLearnedRef();
        incidentCriteria.lessonsLearnedWaiver();
        incidentCriteria.reportDueAt();
        incidentCriteria.reportGeneratedAt();
        incidentCriteria.reportRef();
        incidentCriteria.reportDigest();
        incidentCriteria.complianceVisaBy();
        incidentCriteria.complianceVisaAt();
        incidentCriteria.criteriaValidatedAt();
        incidentCriteria.validatorId();
        incidentCriteria.rawDuration();
        incidentCriteria.frozenDuration();
        incidentCriteria.imputableDelay();
        incidentCriteria.mttr();
        incidentCriteria.takeChargeMet();
        incidentCriteria.restorationMet();
        incidentCriteria.ticketId();
        incidentCriteria.matrixId();
        incidentCriteria.distinct();
    }

    private static Condition<IncidentCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getImpact()) &&
                condition.apply(criteria.getUrgency()) &&
                condition.apply(criteria.getScore()) &&
                condition.apply(criteria.getSeverity()) &&
                condition.apply(criteria.getSecurityIncident()) &&
                condition.apply(criteria.getDataCompromiseSuspected()) &&
                condition.apply(criteria.getRegulatoryFlag()) &&
                condition.apply(criteria.getQualifiedAt()) &&
                condition.apply(criteria.getQualifiedBy()) &&
                condition.apply(criteria.getRootCause()) &&
                condition.apply(criteria.getRootCauseCategory()) &&
                condition.apply(criteria.getRootCauseProofRef()) &&
                condition.apply(criteria.getResolutionTest()) &&
                condition.apply(criteria.getResolutionTestPassed()) &&
                condition.apply(criteria.getResolutionTestAuthorId()) &&
                condition.apply(criteria.getResolutionTestAt()) &&
                condition.apply(criteria.getResolutionTestProofRef()) &&
                condition.apply(criteria.getLessonsLearnedRef()) &&
                condition.apply(criteria.getLessonsLearnedWaiver()) &&
                condition.apply(criteria.getReportDueAt()) &&
                condition.apply(criteria.getReportGeneratedAt()) &&
                condition.apply(criteria.getReportRef()) &&
                condition.apply(criteria.getReportDigest()) &&
                condition.apply(criteria.getComplianceVisaBy()) &&
                condition.apply(criteria.getComplianceVisaAt()) &&
                condition.apply(criteria.getCriteriaValidatedAt()) &&
                condition.apply(criteria.getValidatorId()) &&
                condition.apply(criteria.getRawDuration()) &&
                condition.apply(criteria.getFrozenDuration()) &&
                condition.apply(criteria.getImputableDelay()) &&
                condition.apply(criteria.getMttr()) &&
                condition.apply(criteria.getTakeChargeMet()) &&
                condition.apply(criteria.getRestorationMet()) &&
                condition.apply(criteria.getTicketId()) &&
                condition.apply(criteria.getMatrixId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<IncidentCriteria> copyFiltersAre(IncidentCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getImpact(), copy.getImpact()) &&
                condition.apply(criteria.getUrgency(), copy.getUrgency()) &&
                condition.apply(criteria.getScore(), copy.getScore()) &&
                condition.apply(criteria.getSeverity(), copy.getSeverity()) &&
                condition.apply(criteria.getSecurityIncident(), copy.getSecurityIncident()) &&
                condition.apply(criteria.getDataCompromiseSuspected(), copy.getDataCompromiseSuspected()) &&
                condition.apply(criteria.getRegulatoryFlag(), copy.getRegulatoryFlag()) &&
                condition.apply(criteria.getQualifiedAt(), copy.getQualifiedAt()) &&
                condition.apply(criteria.getQualifiedBy(), copy.getQualifiedBy()) &&
                condition.apply(criteria.getRootCause(), copy.getRootCause()) &&
                condition.apply(criteria.getRootCauseCategory(), copy.getRootCauseCategory()) &&
                condition.apply(criteria.getRootCauseProofRef(), copy.getRootCauseProofRef()) &&
                condition.apply(criteria.getResolutionTest(), copy.getResolutionTest()) &&
                condition.apply(criteria.getResolutionTestPassed(), copy.getResolutionTestPassed()) &&
                condition.apply(criteria.getResolutionTestAuthorId(), copy.getResolutionTestAuthorId()) &&
                condition.apply(criteria.getResolutionTestAt(), copy.getResolutionTestAt()) &&
                condition.apply(criteria.getResolutionTestProofRef(), copy.getResolutionTestProofRef()) &&
                condition.apply(criteria.getLessonsLearnedRef(), copy.getLessonsLearnedRef()) &&
                condition.apply(criteria.getLessonsLearnedWaiver(), copy.getLessonsLearnedWaiver()) &&
                condition.apply(criteria.getReportDueAt(), copy.getReportDueAt()) &&
                condition.apply(criteria.getReportGeneratedAt(), copy.getReportGeneratedAt()) &&
                condition.apply(criteria.getReportRef(), copy.getReportRef()) &&
                condition.apply(criteria.getReportDigest(), copy.getReportDigest()) &&
                condition.apply(criteria.getComplianceVisaBy(), copy.getComplianceVisaBy()) &&
                condition.apply(criteria.getComplianceVisaAt(), copy.getComplianceVisaAt()) &&
                condition.apply(criteria.getCriteriaValidatedAt(), copy.getCriteriaValidatedAt()) &&
                condition.apply(criteria.getValidatorId(), copy.getValidatorId()) &&
                condition.apply(criteria.getRawDuration(), copy.getRawDuration()) &&
                condition.apply(criteria.getFrozenDuration(), copy.getFrozenDuration()) &&
                condition.apply(criteria.getImputableDelay(), copy.getImputableDelay()) &&
                condition.apply(criteria.getMttr(), copy.getMttr()) &&
                condition.apply(criteria.getTakeChargeMet(), copy.getTakeChargeMet()) &&
                condition.apply(criteria.getRestorationMet(), copy.getRestorationMet()) &&
                condition.apply(criteria.getTicketId(), copy.getTicketId()) &&
                condition.apply(criteria.getMatrixId(), copy.getMatrixId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
