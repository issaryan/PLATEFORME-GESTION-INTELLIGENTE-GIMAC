package cm.gimac.des.collaboration.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class RoomCriteriaTest {

    @Test
    void newRoomCriteriaHasAllFiltersNullTest() {
        var roomCriteria = new RoomCriteria();
        assertThat(roomCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void roomCriteriaFluentMethodsCreatesFiltersTest() {
        var roomCriteria = new RoomCriteria();

        setAllFilters(roomCriteria);

        assertThat(roomCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void roomCriteriaCopyCreatesNullFilterTest() {
        var roomCriteria = new RoomCriteria();
        var copy = roomCriteria.copy();

        assertThat(roomCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(roomCriteria)
        );
    }

    @Test
    void roomCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var roomCriteria = new RoomCriteria();
        setAllFilters(roomCriteria);

        var copy = roomCriteria.copy();

        assertThat(roomCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(roomCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var roomCriteria = new RoomCriteria();

        assertThat(roomCriteria).hasToString("RoomCriteria{}");
    }

    private static void setAllFilters(RoomCriteria roomCriteria) {
        roomCriteria.id();
        roomCriteria.code();
        roomCriteria.type();
        roomCriteria.attachmentKind();
        roomCriteria.attachmentReference();
        roomCriteria.attachmentService();
        roomCriteria.ticketReference();
        roomCriteria.matrixRoomId();
        roomCriteria.encryption();
        roomCriteria.createdAt();
        roomCriteria.state();
        roomCriteria.retention();
        roomCriteria.groupSessionEpoch();
        roomCriteria.frozenAt();
        roomCriteria.archivedAt();
        roomCriteria.purgeAfter();
        roomCriteria.roomArchiveId();
        roomCriteria.distinct();
    }

    private static Condition<RoomCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getCode()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getAttachmentKind()) &&
                condition.apply(criteria.getAttachmentReference()) &&
                condition.apply(criteria.getAttachmentService()) &&
                condition.apply(criteria.getTicketReference()) &&
                condition.apply(criteria.getMatrixRoomId()) &&
                condition.apply(criteria.getEncryption()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getRetention()) &&
                condition.apply(criteria.getGroupSessionEpoch()) &&
                condition.apply(criteria.getFrozenAt()) &&
                condition.apply(criteria.getArchivedAt()) &&
                condition.apply(criteria.getPurgeAfter()) &&
                condition.apply(criteria.getRoomArchiveId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<RoomCriteria> copyFiltersAre(RoomCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getCode(), copy.getCode()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getAttachmentKind(), copy.getAttachmentKind()) &&
                condition.apply(criteria.getAttachmentReference(), copy.getAttachmentReference()) &&
                condition.apply(criteria.getAttachmentService(), copy.getAttachmentService()) &&
                condition.apply(criteria.getTicketReference(), copy.getTicketReference()) &&
                condition.apply(criteria.getMatrixRoomId(), copy.getMatrixRoomId()) &&
                condition.apply(criteria.getEncryption(), copy.getEncryption()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getRetention(), copy.getRetention()) &&
                condition.apply(criteria.getGroupSessionEpoch(), copy.getGroupSessionEpoch()) &&
                condition.apply(criteria.getFrozenAt(), copy.getFrozenAt()) &&
                condition.apply(criteria.getArchivedAt(), copy.getArchivedAt()) &&
                condition.apply(criteria.getPurgeAfter(), copy.getPurgeAfter()) &&
                condition.apply(criteria.getRoomArchiveId(), copy.getRoomArchiveId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
