package cm.gimac.des.document.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DocumentLineageCriteriaTest {

    @Test
    void newDocumentLineageCriteriaHasAllFiltersNullTest() {
        var documentLineageCriteria = new DocumentLineageCriteria();
        assertThat(documentLineageCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void documentLineageCriteriaFluentMethodsCreatesFiltersTest() {
        var documentLineageCriteria = new DocumentLineageCriteria();

        setAllFilters(documentLineageCriteria);

        assertThat(documentLineageCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void documentLineageCriteriaCopyCreatesNullFilterTest() {
        var documentLineageCriteria = new DocumentLineageCriteria();
        var copy = documentLineageCriteria.copy();

        assertThat(documentLineageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(documentLineageCriteria)
        );
    }

    @Test
    void documentLineageCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var documentLineageCriteria = new DocumentLineageCriteria();
        setAllFilters(documentLineageCriteria);

        var copy = documentLineageCriteria.copy();

        assertThat(documentLineageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(documentLineageCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var documentLineageCriteria = new DocumentLineageCriteria();

        assertThat(documentLineageCriteria).hasToString("DocumentLineageCriteria{}");
    }

    private static void setAllFilters(DocumentLineageCriteria documentLineageCriteria) {
        documentLineageCriteria.id();
        documentLineageCriteria.type();
        documentLineageCriteria.ownerService();
        documentLineageCriteria.ownerId();
        documentLineageCriteria.institutionId();
        documentLineageCriteria.currentVersion();
        documentLineageCriteria.createdAt();
        documentLineageCriteria.currentDocumentId();
        documentLineageCriteria.distinct();
    }

    private static Condition<DocumentLineageCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getOwnerService()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getInstitutionId()) &&
                condition.apply(criteria.getCurrentVersion()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getCurrentDocumentId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<DocumentLineageCriteria> copyFiltersAre(
        DocumentLineageCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getOwnerService(), copy.getOwnerService()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getInstitutionId(), copy.getInstitutionId()) &&
                condition.apply(criteria.getCurrentVersion(), copy.getCurrentVersion()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getCurrentDocumentId(), copy.getCurrentDocumentId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
