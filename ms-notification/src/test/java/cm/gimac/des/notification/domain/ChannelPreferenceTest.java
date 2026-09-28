package cm.gimac.des.notification.domain;

import static cm.gimac.des.notification.domain.ChannelPreferenceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ChannelPreferenceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ChannelPreference.class);
        ChannelPreference channelPreference1 = getChannelPreferenceSample1();
        ChannelPreference channelPreference2 = new ChannelPreference();
        assertThat(channelPreference1).isNotEqualTo(channelPreference2);

        channelPreference2.setId(channelPreference1.getId());
        assertThat(channelPreference1).isEqualTo(channelPreference2);

        channelPreference2 = getChannelPreferenceSample2();
        assertThat(channelPreference1).isNotEqualTo(channelPreference2);
    }
}
