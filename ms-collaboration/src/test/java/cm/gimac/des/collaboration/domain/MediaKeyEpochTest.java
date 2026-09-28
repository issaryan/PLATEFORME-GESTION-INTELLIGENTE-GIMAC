package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.ConferenceTestSamples.*;
import static cm.gimac.des.collaboration.domain.MediaKeyEpochTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MediaKeyEpochTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MediaKeyEpoch.class);
        MediaKeyEpoch mediaKeyEpoch1 = getMediaKeyEpochSample1();
        MediaKeyEpoch mediaKeyEpoch2 = new MediaKeyEpoch();
        assertThat(mediaKeyEpoch1).isNotEqualTo(mediaKeyEpoch2);

        mediaKeyEpoch2.setId(mediaKeyEpoch1.getId());
        assertThat(mediaKeyEpoch1).isEqualTo(mediaKeyEpoch2);

        mediaKeyEpoch2 = getMediaKeyEpochSample2();
        assertThat(mediaKeyEpoch1).isNotEqualTo(mediaKeyEpoch2);
    }

    @Test
    void conferenceTest() {
        MediaKeyEpoch mediaKeyEpoch = getMediaKeyEpochRandomSampleGenerator();
        Conference conferenceBack = getConferenceRandomSampleGenerator();

        mediaKeyEpoch.setConference(conferenceBack);
        assertThat(mediaKeyEpoch.getConference()).isEqualTo(conferenceBack);

        mediaKeyEpoch.conference(null);
        assertThat(mediaKeyEpoch.getConference()).isNull();
    }
}
