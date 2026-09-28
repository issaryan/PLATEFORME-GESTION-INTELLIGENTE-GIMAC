package cm.gimac.des.notification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChannelPreferenceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ChannelPreferenceDTO.class);
        ChannelPreferenceDTO channelPreferenceDTO1 = new ChannelPreferenceDTO();
        channelPreferenceDTO1.setId(UUID.randomUUID());
        ChannelPreferenceDTO channelPreferenceDTO2 = new ChannelPreferenceDTO();
        assertThat(channelPreferenceDTO1).isNotEqualTo(channelPreferenceDTO2);
        channelPreferenceDTO2.setId(channelPreferenceDTO1.getId());
        assertThat(channelPreferenceDTO1).isEqualTo(channelPreferenceDTO2);
        channelPreferenceDTO2.setId(UUID.randomUUID());
        assertThat(channelPreferenceDTO1).isNotEqualTo(channelPreferenceDTO2);
        channelPreferenceDTO1.setId(null);
        assertThat(channelPreferenceDTO1).isNotEqualTo(channelPreferenceDTO2);
    }
}
