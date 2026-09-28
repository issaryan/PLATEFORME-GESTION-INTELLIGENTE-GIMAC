package cm.gimac.des.certification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CommissioningFileCriteriaTest {

    @Test
    void newCommissioningFileCriteriaHasAllFiltersNullTest() {
        var commissioningFileCriteria = new CommissioningFileCriteria();
        assertThat(commissioningFileCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void commissioningFileCriteriaFluentMethodsCreatesFiltersTest() {
        var commissioningFileCriteria = new CommissioningFileCriteria();

        setAllFilters(commissioningFileCriteria);

        assertThat(commissioningFileCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void commissioningFileCriteriaCopyCreatesNullFilterTest() {
        var commissioningFileCriteria = new CommissioningFileCriteria();
        var copy = commissioningFileCriteria.copy();

        assertThat(commissioningFileCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(commissioningFileCriteria)
        );
    }

    @Test
    void commissioningFileCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var commissioningFileCriteria = new CommissioningFileCriteria();
        setAllFilters(commissioningFileCriteria);

        var copy = commissioningFileCriteria.copy();

        assertThat(commissioningFileCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(commissioningFileCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var commissioningFileCriteria = new CommissioningFileCriteria();

        assertThat(commissioningFileCriteria).hasToString("CommissioningFileCriteria{}");
    }

    private static void setAllFilters(CommissioningFileCriteria commissioningFileCriteria) {
        commissioningFileCriteria.id();
        commissioningFileCriteria.fileCode();
        commissioningFileCriteria.liaisonId();
        commissioningFileCriteria.institutionId();
        commissioningFileCriteria.projectLeadId();
        commissioningFileCriteria.state();
        commissioningFileCriteria.basisDigest();
        commissioningFileCriteria.basisSealedAt();
        commissioningFileCriteria.openedAt();
        commissioningFileCriteria.correctionReason();
        commissioningFileCriteria.manifestDigest();
        commissioningFileCriteria.sealedAt();
        commissioningFileCriteria.goLiveAuthorizedAt();
        commissioningFileCriteria.changeRecordId();
        commissioningFileCriteria.attestationId();
        commissioningFileCriteria.commissioningReportId();
        commissioningFileCriteria.quitusId();
        commissioningFileCriteria.piecesId();
        commissioningFileCriteria.distinct();
    }

    private static Condition<CommissioningFileCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getFileCode()) &&
                condition.apply(criteria.getLiaisonId()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getProjectLeadId()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getBasisDigest()) &&
                condition.apply(criteria.getBasisSealedAt()) &&
                condition.apply(criteria.getOpenedAt()) &&
                condition.apply(criteria.getCorrectionReason()) &&
                condition.apply(criteria.getManifestDigest()) &&
                condition.apply(criteria.getSealedAt()) &&
                condition.apply(criteria.getGoLiveAuthorizedAt()) &&
                condition.apply(criteria.getChangeRecordId()) &&
                condition.apply(criteria.getAttestationId()) &&
                condition.apply(criteria.getCommissioningReportId()) &&
                condition.apply(criteria.getQuitusId()) &&
                condition.apply(criteria.getPiecesId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CommissioningFileCriteria> copyFiltersAre(
        CommissioningFileCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getFileCode(), copy.getFileCode()) &&
                condition.apply(criteria.getLiaisonId(), copy.getLiaisonId()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getProjectLeadId(), copy.getProjectLeadId()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getBasisDigest(), copy.getBasisDigest()) &&
                condition.apply(criteria.getBasisSealedAt(), copy.getBasisSealedAt()) &&
                condition.apply(criteria.getOpenedAt(), copy.getOpenedAt()) &&
                condition.apply(criteria.getCorrectionReason(), copy.getCorrectionReason()) &&
                condition.apply(criteria.getManifestDigest(), copy.getManifestDigest()) &&
                condition.apply(criteria.getSealedAt(), copy.getSealedAt()) &&
                condition.apply(criteria.getGoLiveAuthorizedAt(), copy.getGoLiveAuthorizedAt()) &&
                condition.apply(criteria.getChangeRecordId(), copy.getChangeRecordId()) &&
                condition.apply(criteria.getAttestationId(), copy.getAttestationId()) &&
                condition.apply(criteria.getCommissioningReportId(), copy.getCommissioningReportId()) &&
                condition.apply(criteria.getQuitusId(), copy.getQuitusId()) &&
                condition.apply(criteria.getPiecesId(), copy.getPiecesId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
