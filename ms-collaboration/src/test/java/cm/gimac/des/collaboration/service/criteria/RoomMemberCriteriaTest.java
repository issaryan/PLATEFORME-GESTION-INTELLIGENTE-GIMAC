package cm.gimac.des.collaboration.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class RoomMemberCriteriaTest {

    @Test
    void newRoomMemberCriteriaHasAllFiltersNullTest() {
        var roomMemberCriteria = new RoomMemberCriteria();
        assertThat(roomMemberCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void roomMemberCriteriaFluentMethodsCreatesFiltersTest() {
        var roomMemberCriteria = new RoomMemberCriteria();

        setAllFilters(roomMemberCriteria);

        assertThat(roomMemberCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void roomMemberCriteriaCopyCreatesNullFilterTest() {
        var roomMemberCriteria = new RoomMemberCriteria();
        var copy = roomMemberCriteria.copy();

        assertThat(roomMemberCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(roomMemberCriteria)
        );
    }

    @Test
    void roomMemberCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var roomMemberCriteria = new RoomMemberCriteria();
        setAllFilters(roomMemberCriteria);

        var copy = roomMemberCriteria.copy();

        assertThat(roomMemberCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(roomMemberCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var roomMemberCriteria = new RoomMemberCriteria();

        assertThat(roomMemberCriteria).hasToString("RoomMemberCriteria{}");
    }

    private static void setAllFilters(RoomMemberCriteria roomMemberCriteria) {
        roomMemberCriteria.id();
        roomMemberCriteria.userId();
        roomMemberCriteria.role();
        roomMemberCriteria.invitedAt();
        roomMemberCriteria.acknowledgedAt();
        roomMemberCriteria.replacesUserId();
        roomMemberCriteria.joinedAt();
        roomMemberCriteria.leftAt();
        roomMemberCriteria.roomId();
        roomMemberCriteria.distinct();
    }

    private static Condition<RoomMemberCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getRole()) &&
                condition.apply(criteria.getInvitedAt()) &&
                condition.apply(criteria.getAcknowledgedAt()) &&
                condition.apply(criteria.getReplacesUserId()) &&
                condition.apply(criteria.getJoinedAt()) &&
                condition.apply(criteria.getLeftAt()) &&
                condition.apply(criteria.getRoomId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<RoomMemberCriteria> copyFiltersAre(RoomMemberCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getRole(), copy.getRole()) &&
                condition.apply(criteria.getInvitedAt(), copy.getInvitedAt()) &&
                condition.apply(criteria.getAcknowledgedAt(), copy.getAcknowledgedAt()) &&
                condition.apply(criteria.getReplacesUserId(), copy.getReplacesUserId()) &&
                condition.apply(criteria.getJoinedAt(), copy.getJoinedAt()) &&
                condition.apply(criteria.getLeftAt(), copy.getLeftAt()) &&
                condition.apply(criteria.getRoomId(), copy.getRoomId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
