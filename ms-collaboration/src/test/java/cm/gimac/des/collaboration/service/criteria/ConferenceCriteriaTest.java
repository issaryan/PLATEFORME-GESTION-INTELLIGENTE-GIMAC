package cm.gimac.des.collaboration.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ConferenceCriteriaTest {

    @Test
    void newConferenceCriteriaHasAllFiltersNullTest() {
        var conferenceCriteria = new ConferenceCriteria();
        assertThat(conferenceCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void conferenceCriteriaFluentMethodsCreatesFiltersTest() {
        var conferenceCriteria = new ConferenceCriteria();

        setAllFilters(conferenceCriteria);

        assertThat(conferenceCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void conferenceCriteriaCopyCreatesNullFilterTest() {
        var conferenceCriteria = new ConferenceCriteria();
        var copy = conferenceCriteria.copy();

        assertThat(conferenceCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(conferenceCriteria)
        );
    }

    @Test
    void conferenceCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var conferenceCriteria = new ConferenceCriteria();
        setAllFilters(conferenceCriteria);

        var copy = conferenceCriteria.copy();

        assertThat(conferenceCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(conferenceCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var conferenceCriteria = new ConferenceCriteria();

        assertThat(conferenceCriteria).hasToString("ConferenceCriteria{}");
    }

    private static void setAllFilters(ConferenceCriteria conferenceCriteria) {
        conferenceCriteria.id();
        conferenceCriteria.sfuRoom();
        conferenceCriteria.startedAt();
        conferenceCriteria.endedAt();
        conferenceCriteria.sframeRequired();
        conferenceCriteria.screenShare();
        conferenceCriteria.recording();
        conferenceCriteria.keyEpoch();
        conferenceCriteria.roomId();
        conferenceCriteria.distinct();
    }

    private static Condition<ConferenceCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getSfuRoom()) &&
                condition.apply(criteria.getStartedAt()) &&
                condition.apply(criteria.getEndedAt()) &&
                condition.apply(criteria.getSframeRequired()) &&
                condition.apply(criteria.getScreenShare()) &&
                condition.apply(criteria.getRecording()) &&
                condition.apply(criteria.getKeyEpoch()) &&
                condition.apply(criteria.getRoomId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ConferenceCriteria> copyFiltersAre(ConferenceCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getSfuRoom(), copy.getSfuRoom()) &&
                condition.apply(criteria.getStartedAt(), copy.getStartedAt()) &&
                condition.apply(criteria.getEndedAt(), copy.getEndedAt()) &&
                condition.apply(criteria.getSframeRequired(), copy.getSframeRequired()) &&
                condition.apply(criteria.getScreenShare(), copy.getScreenShare()) &&
                condition.apply(criteria.getRecording(), copy.getRecording()) &&
                condition.apply(criteria.getKeyEpoch(), copy.getKeyEpoch()) &&
                condition.apply(criteria.getRoomId(), copy.getRoomId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
