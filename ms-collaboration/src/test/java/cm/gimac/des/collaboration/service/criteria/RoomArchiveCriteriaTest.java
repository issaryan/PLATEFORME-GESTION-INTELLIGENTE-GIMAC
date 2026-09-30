package cm.gimac.des.collaboration.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class RoomArchiveCriteriaTest {

    @Test
    void newRoomArchiveCriteriaHasAllFiltersNullTest() {
        var roomArchiveCriteria = new RoomArchiveCriteria();
        assertThat(roomArchiveCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void roomArchiveCriteriaFluentMethodsCreatesFiltersTest() {
        var roomArchiveCriteria = new RoomArchiveCriteria();

        setAllFilters(roomArchiveCriteria);

        assertThat(roomArchiveCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void roomArchiveCriteriaCopyCreatesNullFilterTest() {
        var roomArchiveCriteria = new RoomArchiveCriteria();
        var copy = roomArchiveCriteria.copy();

        assertThat(roomArchiveCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(roomArchiveCriteria)
        );
    }

    @Test
    void roomArchiveCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var roomArchiveCriteria = new RoomArchiveCriteria();
        setAllFilters(roomArchiveCriteria);

        var copy = roomArchiveCriteria.copy();

        assertThat(roomArchiveCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(roomArchiveCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var roomArchiveCriteria = new RoomArchiveCriteria();

        assertThat(roomArchiveCriteria).hasToString("RoomArchiveCriteria{}");
    }

    private static void setAllFilters(RoomArchiveCriteria roomArchiveCriteria) {
        roomArchiveCriteria.id();
        roomArchiveCriteria.crisisCellReference();
        roomArchiveCriteria.ticketReference();
        roomArchiveCriteria.messageCount();
        roomArchiveCriteria.undecryptableCount();
        roomArchiveCriteria.maskedSequences();
        roomArchiveCriteria.chainHead();
        roomArchiveCriteria.contentDigest();
        roomArchiveCriteria.documentRef();
        roomArchiveCriteria.retentionYears();
        roomArchiveCriteria.state();
        roomArchiveCriteria.startedAt();
        roomArchiveCriteria.sealedAt();
        roomArchiveCriteria.roomId();
        roomArchiveCriteria.distinct();
    }

    private static Condition<RoomArchiveCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getCrisisCellReference()) &&
                condition.apply(criteria.getTicketReference()) &&
                condition.apply(criteria.getMessageCount()) &&
                condition.apply(criteria.getUndecryptableCount()) &&
                condition.apply(criteria.getMaskedSequences()) &&
                condition.apply(criteria.getChainHead()) &&
                condition.apply(criteria.getContentDigest()) &&
                condition.apply(criteria.getDocumentRef()) &&
                condition.apply(criteria.getRetentionYears()) &&
                condition.apply(criteria.getState()) &&
                condition.apply(criteria.getStartedAt()) &&
                condition.apply(criteria.getSealedAt()) &&
                condition.apply(criteria.getRoomId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<RoomArchiveCriteria> copyFiltersAre(RoomArchiveCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getCrisisCellReference(), copy.getCrisisCellReference()) &&
                condition.apply(criteria.getTicketReference(), copy.getTicketReference()) &&
                condition.apply(criteria.getMessageCount(), copy.getMessageCount()) &&
                condition.apply(criteria.getUndecryptableCount(), copy.getUndecryptableCount()) &&
                condition.apply(criteria.getMaskedSequences(), copy.getMaskedSequences()) &&
                condition.apply(criteria.getChainHead(), copy.getChainHead()) &&
                condition.apply(criteria.getContentDigest(), copy.getContentDigest()) &&
                condition.apply(criteria.getDocumentRef(), copy.getDocumentRef()) &&
                condition.apply(criteria.getRetentionYears(), copy.getRetentionYears()) &&
                condition.apply(criteria.getState(), copy.getState()) &&
                condition.apply(criteria.getStartedAt(), copy.getStartedAt()) &&
                condition.apply(criteria.getSealedAt(), copy.getSealedAt()) &&
                condition.apply(criteria.getRoomId(), copy.getRoomId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
