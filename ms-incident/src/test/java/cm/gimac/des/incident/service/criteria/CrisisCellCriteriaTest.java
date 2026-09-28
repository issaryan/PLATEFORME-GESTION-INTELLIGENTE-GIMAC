package cm.gimac.des.incident.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CrisisCellCriteriaTest {

    @Test
    void newCrisisCellCriteriaHasAllFiltersNullTest() {
        var crisisCellCriteria = new CrisisCellCriteria();
        assertThat(crisisCellCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void crisisCellCriteriaFluentMethodsCreatesFiltersTest() {
        var crisisCellCriteria = new CrisisCellCriteria();

        setAllFilters(crisisCellCriteria);

        assertThat(crisisCellCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void crisisCellCriteriaCopyCreatesNullFilterTest() {
        var crisisCellCriteria = new CrisisCellCriteria();
        var copy = crisisCellCriteria.copy();

        assertThat(crisisCellCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(crisisCellCriteria)
        );
    }

    @Test
    void crisisCellCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var crisisCellCriteria = new CrisisCellCriteria();
        setAllFilters(crisisCellCriteria);

        var copy = crisisCellCriteria.copy();

        assertThat(crisisCellCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(crisisCellCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var crisisCellCriteria = new CrisisCellCriteria();

        assertThat(crisisCellCriteria).hasToString("CrisisCellCriteria{}");
    }

    private static void setAllFilters(CrisisCellCriteria crisisCellCriteria) {
        crisisCellCriteria.id();
        crisisCellCriteria.reference();
        crisisCellCriteria.level();
        crisisCellCriteria.state();
        crisisCellCriteria.automaticActivation();
        crisisCellCriteria.activationReason();
        crisisCellCriteria.convenedBy();
        crisisCellCriteria.convenedAt();
        crisisCellCriteria.quorumDeadline();
        crisisCellCriteria.quorumThreshold();
        crisisCellCriteria.constitutedAt();
        crisisCellCriteria.roomRef();
        crisisCellCriteria.postIncidentReportDueAt();
        crisisCellCriteria.postIncidentReportRef();
        crisisCellCriteria.postIncidentReportDigest();
        crisisCellCriteria.postIncidentReportSignedBy();
        crisisCellCriteria.postIncidentReportSignedAt();
        crisisCellCriteria.closedBy();
        crisisCellCriteria.closedAt();
        crisisCellCriteria.archiveRef();
        crisisCellCriteria.archivedAt();
        crisisCellCriteria.incidentId();
        crisisCellCriteria.distinct();
    }

    private static Condition<CrisisCellCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getReference()) &&
                condition.apply(criteria.getLevel()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getAutomaticActivation()) &&
                condition.apply(criteria.getActivationReason()) &&
                condition.apply(criteria.getConvenedBy()) &&
                condition.apply(criteria.getConvenedAt()) &&
                condition.apply(criteria.getQuorumDeadline()) &&
                condition.apply(criteria.getQuorumThreshold()) &&
                condition.apply(criteria.getConstitutedAt()) &&
                condition.apply(criteria.getRoomRef()) &&
                condition.apply(criteria.getPostIncidentReportDueAt()) &&
                condition.apply(criteria.getPostIncidentReportRef()) &&
                condition.apply(criteria.getPostIncidentReportDigest()) &&
                condition.apply(criteria.getPostIncidentReportSignedBy()) &&
                condition.apply(criteria.getPostIncidentReportSignedAt()) &&
                condition.apply(criteria.getClosedBy()) &&
                condition.apply(criteria.getClosedAt()) &&
                condition.apply(criteria.getArchiveRef()) &&
                condition.apply(criteria.getArchivedAt()) &&
                condition.apply(criteria.getIncidentId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CrisisCellCriteria> copyFiltersAre(CrisisCellCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getReference(), copy.getReference()) &&
                condition.apply(criteria.getLevel(), copy.getLevel()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getAutomaticActivation(), copy.getAutomaticActivation()) &&
                condition.apply(criteria.getActivationReason(), copy.getActivationReason()) &&
                condition.apply(criteria.getConvenedBy(), copy.getConvenedBy()) &&
                condition.apply(criteria.getConvenedAt(), copy.getConvenedAt()) &&
                condition.apply(criteria.getQuorumDeadline(), copy.getQuorumDeadline()) &&
                condition.apply(criteria.getQuorumThreshold(), copy.getQuorumThreshold()) &&
                condition.apply(criteria.getConstitutedAt(), copy.getConstitutedAt()) &&
                condition.apply(criteria.getRoomRef(), copy.getRoomRef()) &&
                condition.apply(criteria.getPostIncidentReportDueAt(), copy.getPostIncidentReportDueAt()) &&
                condition.apply(criteria.getPostIncidentReportRef(), copy.getPostIncidentReportRef()) &&
                condition.apply(criteria.getPostIncidentReportDigest(), copy.getPostIncidentReportDigest()) &&
                condition.apply(criteria.getPostIncidentReportSignedBy(), copy.getPostIncidentReportSignedBy()) &&
                condition.apply(criteria.getPostIncidentReportSignedAt(), copy.getPostIncidentReportSignedAt()) &&
                condition.apply(criteria.getClosedBy(), copy.getClosedBy()) &&
                condition.apply(criteria.getClosedAt(), copy.getClosedAt()) &&
                condition.apply(criteria.getArchiveRef(), copy.getArchiveRef()) &&
                condition.apply(criteria.getArchivedAt(), copy.getArchivedAt()) &&
                condition.apply(criteria.getIncidentId(), copy.getIncidentId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
