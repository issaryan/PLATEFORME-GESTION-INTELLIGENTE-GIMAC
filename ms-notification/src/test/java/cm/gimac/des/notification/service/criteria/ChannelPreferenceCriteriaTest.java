package cm.gimac.des.notification.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ChannelPreferenceCriteriaTest {

    @Test
    void newChannelPreferenceCriteriaHasAllFiltersNullTest() {
        var channelPreferenceCriteria = new ChannelPreferenceCriteria();
        assertThat(channelPreferenceCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void channelPreferenceCriteriaFluentMethodsCreatesFiltersTest() {
        var channelPreferenceCriteria = new ChannelPreferenceCriteria();

        setAllFilters(channelPreferenceCriteria);

        assertThat(channelPreferenceCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void channelPreferenceCriteriaCopyCreatesNullFilterTest() {
        var channelPreferenceCriteria = new ChannelPreferenceCriteria();
        var copy = channelPreferenceCriteria.copy();

        assertThat(channelPreferenceCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(channelPreferenceCriteria)
        );
    }

    @Test
    void channelPreferenceCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var channelPreferenceCriteria = new ChannelPreferenceCriteria();
        setAllFilters(channelPreferenceCriteria);

        var copy = channelPreferenceCriteria.copy();

        assertThat(channelPreferenceCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(channelPreferenceCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var channelPreferenceCriteria = new ChannelPreferenceCriteria();

        assertThat(channelPreferenceCriteria).hasToString("ChannelPreferenceCriteria{}");
    }

    private static void setAllFilters(ChannelPreferenceCriteria channelPreferenceCriteria) {
        channelPreferenceCriteria.id();
        channelPreferenceCriteria.userId();
        channelPreferenceCriteria.channel();
        channelPreferenceCriteria.minimumUrgency();
        channelPreferenceCriteria.windowStart();
        channelPreferenceCriteria.windowEnd();
        channelPreferenceCriteria.enabled();
        channelPreferenceCriteria.distinct();
    }

    private static Condition<ChannelPreferenceCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getChannel()) &&
                condition.apply(criteria.getMinimumUrgency()) &&
                condition.apply(criteria.getWindowStart()) &&
                condition.apply(criteria.getWindowEnd()) &&
                condition.apply(criteria.getEnabled()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ChannelPreferenceCriteria> copyFiltersAre(
        ChannelPreferenceCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getChannel(), copy.getChannel()) &&
                condition.apply(criteria.getMinimumUrgency(), copy.getMinimumUrgency()) &&
                condition.apply(criteria.getWindowStart(), copy.getWindowStart()) &&
                condition.apply(criteria.getWindowEnd(), copy.getWindowEnd()) &&
                condition.apply(criteria.getEnabled(), copy.getEnabled()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
