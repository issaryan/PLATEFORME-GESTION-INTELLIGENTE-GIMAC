package cm.gimac.des.homologation.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class LiaisonCriteriaTest {

    @Test
    void newLiaisonCriteriaHasAllFiltersNullTest() {
        var liaisonCriteria = new LiaisonCriteria();
        assertThat(liaisonCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void liaisonCriteriaFluentMethodsCreatesFiltersTest() {
        var liaisonCriteria = new LiaisonCriteria();

        setAllFilters(liaisonCriteria);

        assertThat(liaisonCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void liaisonCriteriaCopyCreatesNullFilterTest() {
        var liaisonCriteria = new LiaisonCriteria();
        var copy = liaisonCriteria.copy();

        assertThat(liaisonCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(liaisonCriteria)
        );
    }

    @Test
    void liaisonCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var liaisonCriteria = new LiaisonCriteria();
        setAllFilters(liaisonCriteria);

        var copy = liaisonCriteria.copy();

        assertThat(liaisonCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(liaisonCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var liaisonCriteria = new LiaisonCriteria();

        assertThat(liaisonCriteria).hasToString("LiaisonCriteria{}");
    }

    private static void setAllFilters(LiaisonCriteria liaisonCriteria) {
        liaisonCriteria.id();
        liaisonCriteria.liaisonCode();
        liaisonCriteria.institutionId();
        liaisonCriteria.projectType();
        liaisonCriteria.openedOn();
        liaisonCriteria.projectLeadId();
        liaisonCriteria.state();
        liaisonCriteria.campaignRef();
        liaisonCriteria.readyForCertificationAt();
        liaisonCriteria.commissionedOn();
        liaisonCriteria.stateReason();
        liaisonCriteria.catalogId();
        liaisonCriteria.campaignSyncId();
        liaisonCriteria.distinct();
    }

    private static Condition<LiaisonCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getLiaisonCode()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getProjectType()) &&
                condition.apply(criteria.getOpenedOn()) &&
                condition.apply(criteria.getProjectLeadId()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getCampaignRef()) &&
                condition.apply(criteria.getReadyForCertificationAt()) &&
                condition.apply(criteria.getCommissionedOn()) &&
                condition.apply(criteria.getStateReason()) &&
                condition.apply(criteria.getCatalogId()) &&
                condition.apply(criteria.getCampaignSyncId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<LiaisonCriteria> copyFiltersAre(LiaisonCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getLiaisonCode(), copy.getLiaisonCode()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getProjectType(), copy.getProjectType()) &&
                condition.apply(criteria.getOpenedOn(), copy.getOpenedOn()) &&
                condition.apply(criteria.getProjectLeadId(), copy.getProjectLeadId()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getCampaignRef(), copy.getCampaignRef()) &&
                condition.apply(criteria.getReadyForCertificationAt(), copy.getReadyForCertificationAt()) &&
                condition.apply(criteria.getCommissionedOn(), copy.getCommissionedOn()) &&
                condition.apply(criteria.getStateReason(), copy.getStateReason()) &&
                condition.apply(criteria.getCatalogId(), copy.getCatalogId()) &&
                condition.apply(criteria.getCampaignSyncId(), copy.getCampaignSyncId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
