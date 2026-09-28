package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.ConferenceTestSamples.*;
import static cm.gimac.des.collaboration.domain.RoomTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ConferenceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Conference.class);
        Conference conference1 = getConferenceSample1();
        Conference conference2 = new Conference();
        assertThat(conference1).isNotEqualTo(conference2);

        conference2.setId(conference1.getId());
        assertThat(conference1).isEqualTo(conference2);

        conference2 = getConferenceSample2();
        assertThat(conference1).isNotEqualTo(conference2);
    }

    @Test
    void roomTest() {
        Conference conference = getConferenceRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        conference.setRoom(roomBack);
        assertThat(conference.getRoom()).isEqualTo(roomBack);

        conference.room(null);
        assertThat(conference.getRoom()).isNull();
    }
}
