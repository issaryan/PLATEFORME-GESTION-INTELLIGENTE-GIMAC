package cm.gimac.des.collaboration.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class MessageMetadataCriteriaTest {

    @Test
    void newMessageMetadataCriteriaHasAllFiltersNullTest() {
        var messageMetadataCriteria = new MessageMetadataCriteria();
        assertThat(messageMetadataCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void messageMetadataCriteriaFluentMethodsCreatesFiltersTest() {
        var messageMetadataCriteria = new MessageMetadataCriteria();

        setAllFilters(messageMetadataCriteria);

        assertThat(messageMetadataCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void messageMetadataCriteriaCopyCreatesNullFilterTest() {
        var messageMetadataCriteria = new MessageMetadataCriteria();
        var copy = messageMetadataCriteria.copy();

        assertThat(messageMetadataCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(messageMetadataCriteria)
        );
    }

    @Test
    void messageMetadataCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var messageMetadataCriteria = new MessageMetadataCriteria();
        setAllFilters(messageMetadataCriteria);

        var copy = messageMetadataCriteria.copy();

        assertThat(messageMetadataCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(messageMetadataCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var messageMetadataCriteria = new MessageMetadataCriteria();

        assertThat(messageMetadataCriteria).hasToString("MessageMetadataCriteria{}");
    }

    private static void setAllFilters(MessageMetadataCriteria messageMetadataCriteria) {
        messageMetadataCriteria.id();
        messageMetadataCriteria.matrixEventId();
        messageMetadataCriteria.authorId();
        messageMetadataCriteria.terminalId();
        messageMetadataCriteria.sentAt();
        messageMetadataCriteria.sessionId();
        messageMetadataCriteria.ratchetIndex();
        messageMetadataCriteria.ciphertextSize();
        messageMetadataCriteria.attachmentRefs();
        messageMetadataCriteria.roomId();
        messageMetadataCriteria.distinct();
    }

    private static Condition<MessageMetadataCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getMatrixEventId()) &&
                condition.apply(criteria.getAuthorId()) &&
                condition.apply(criteria.getTerminalId()) &&
                condition.apply(criteria.getSentAt()) &&
                condition.apply(criteria.getSessionId()) &&
                condition.apply(criteria.getRatchetIndex()) &&
                condition.apply(criteria.getCiphertextSize()) &&
                condition.apply(criteria.getAttachmentRefs()) &&
                condition.apply(criteria.getRoomId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<MessageMetadataCriteria> copyFiltersAre(
        MessageMetadataCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getMatrixEventId(), copy.getMatrixEventId()) &&
                condition.apply(criteria.getAuthorId(), copy.getAuthorId()) &&
                condition.apply(criteria.getTerminalId(), copy.getTerminalId()) &&
                condition.apply(criteria.getSentAt(), copy.getSentAt()) &&
                condition.apply(criteria.getSessionId(), copy.getSessionId()) &&
                condition.apply(criteria.getRatchetIndex(), copy.getRatchetIndex()) &&
                condition.apply(criteria.getCiphertextSize(), copy.getCiphertextSize()) &&
                condition.apply(criteria.getAttachmentRefs(), copy.getAttachmentRefs()) &&
                condition.apply(criteria.getRoomId(), copy.getRoomId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
