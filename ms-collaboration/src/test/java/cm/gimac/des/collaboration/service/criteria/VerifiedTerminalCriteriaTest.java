package cm.gimac.des.collaboration.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class VerifiedTerminalCriteriaTest {

    @Test
    void newVerifiedTerminalCriteriaHasAllFiltersNullTest() {
        var verifiedTerminalCriteria = new VerifiedTerminalCriteria();
        assertThat(verifiedTerminalCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void verifiedTerminalCriteriaFluentMethodsCreatesFiltersTest() {
        var verifiedTerminalCriteria = new VerifiedTerminalCriteria();

        setAllFilters(verifiedTerminalCriteria);

        assertThat(verifiedTerminalCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void verifiedTerminalCriteriaCopyCreatesNullFilterTest() {
        var verifiedTerminalCriteria = new VerifiedTerminalCriteria();
        var copy = verifiedTerminalCriteria.copy();

        assertThat(verifiedTerminalCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(verifiedTerminalCriteria)
        );
    }

    @Test
    void verifiedTerminalCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var verifiedTerminalCriteria = new VerifiedTerminalCriteria();
        setAllFilters(verifiedTerminalCriteria);

        var copy = verifiedTerminalCriteria.copy();

        assertThat(verifiedTerminalCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(verifiedTerminalCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var verifiedTerminalCriteria = new VerifiedTerminalCriteria();

        assertThat(verifiedTerminalCriteria).hasToString("VerifiedTerminalCriteria{}");
    }

    private static void setAllFilters(VerifiedTerminalCriteria verifiedTerminalCriteria) {
        verifiedTerminalCriteria.id();
        verifiedTerminalCriteria.userId();
        verifiedTerminalCriteria.terminalId();
        verifiedTerminalCriteria.identityKey();
        verifiedTerminalCriteria.agreementKey();
        verifiedTerminalCriteria.signedPreKey();
        verifiedTerminalCriteria.signedPreKeySignature();
        verifiedTerminalCriteria.oneTimePreKeys();
        verifiedTerminalCriteria.crossSignature();
        verifiedTerminalCriteria.state();
        verifiedTerminalCriteria.archiveTerminal();
        verifiedTerminalCriteria.registeredAt();
        verifiedTerminalCriteria.verifiedAt();
        verifiedTerminalCriteria.revokedAt();
        verifiedTerminalCriteria.distinct();
    }

    private static Condition<VerifiedTerminalCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getTerminalId()) &&
                condition.apply(criteria.getIdentityKey()) &&
                condition.apply(criteria.getAgreementKey()) &&
                condition.apply(criteria.getSignedPreKey()) &&
                condition.apply(criteria.getSignedPreKeySignature()) &&
                condition.apply(criteria.getOneTimePreKeys()) &&
                condition.apply(criteria.getCrossSignature()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getArchiveTerminal()) &&
                condition.apply(criteria.getRegisteredAt()) &&
                condition.apply(criteria.getVerifiedAt()) &&
                condition.apply(criteria.getRevokedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<VerifiedTerminalCriteria> copyFiltersAre(
        VerifiedTerminalCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getTerminalId(), copy.getTerminalId()) &&
                condition.apply(criteria.getIdentityKey(), copy.getIdentityKey()) &&
                condition.apply(criteria.getAgreementKey(), copy.getAgreementKey()) &&
                condition.apply(criteria.getSignedPreKey(), copy.getSignedPreKey()) &&
                condition.apply(criteria.getSignedPreKeySignature(), copy.getSignedPreKeySignature()) &&
                condition.apply(criteria.getOneTimePreKeys(), copy.getOneTimePreKeys()) &&
                condition.apply(criteria.getCrossSignature(), copy.getCrossSignature()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getArchiveTerminal(), copy.getArchiveTerminal()) &&
                condition.apply(criteria.getRegisteredAt(), copy.getRegisteredAt()) &&
                condition.apply(criteria.getVerifiedAt(), copy.getVerifiedAt()) &&
                condition.apply(criteria.getRevokedAt(), copy.getRevokedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
