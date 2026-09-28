package cm.gimac.des.pilotage.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class MeetingCriteriaTest {

    @Test
    void newMeetingCriteriaHasAllFiltersNullTest() {
        var meetingCriteria = new MeetingCriteria();
        assertThat(meetingCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void meetingCriteriaFluentMethodsCreatesFiltersTest() {
        var meetingCriteria = new MeetingCriteria();

        setAllFilters(meetingCriteria);

        assertThat(meetingCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void meetingCriteriaCopyCreatesNullFilterTest() {
        var meetingCriteria = new MeetingCriteria();
        var copy = meetingCriteria.copy();

        assertThat(meetingCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(meetingCriteria)
        );
    }

    @Test
    void meetingCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var meetingCriteria = new MeetingCriteria();
        setAllFilters(meetingCriteria);

        var copy = meetingCriteria.copy();

        assertThat(meetingCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(meetingCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var meetingCriteria = new MeetingCriteria();

        assertThat(meetingCriteria).hasToString("MeetingCriteria{}");
    }

    private static void setAllFilters(MeetingCriteria meetingCriteria) {
        meetingCriteria.id();
        meetingCriteria.instance();
        meetingCriteria.scheduledAt();
        meetingCriteria.heldAt();
        meetingCriteria.minutesRef();
        meetingCriteria.minutesDigest();
        meetingCriteria.state();
        meetingCriteria.distinct();
    }

    private static Condition<MeetingCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getInstance()) &&
                condition.apply(criteria.getScheduledAt()) &&
                condition.apply(criteria.getHeldAt()) &&
                condition.apply(criteria.getMinutesRef()) &&
                condition.apply(criteria.getMinutesDigest()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<MeetingCriteria> copyFiltersAre(MeetingCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getInstance(), copy.getInstance()) &&
                condition.apply(criteria.getScheduledAt(), copy.getScheduledAt()) &&
                condition.apply(criteria.getHeldAt(), copy.getHeldAt()) &&
                condition.apply(criteria.getMinutesRef(), copy.getMinutesRef()) &&
                condition.apply(criteria.getMinutesDigest(), copy.getMinutesDigest()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
