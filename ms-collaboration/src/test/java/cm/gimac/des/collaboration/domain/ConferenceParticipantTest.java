package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.ConferenceParticipantTestSamples.*;
import static cm.gimac.des.collaboration.domain.ConferenceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ConferenceParticipantTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConferenceParticipant.class);
        ConferenceParticipant conferenceParticipant1 = getConferenceParticipantSample1();
        ConferenceParticipant conferenceParticipant2 = new ConferenceParticipant();
        assertThat(conferenceParticipant1).isNotEqualTo(conferenceParticipant2);

        conferenceParticipant2.setId(conferenceParticipant1.getId());
        assertThat(conferenceParticipant1).isEqualTo(conferenceParticipant2);

        conferenceParticipant2 = getConferenceParticipantSample2();
        assertThat(conferenceParticipant1).isNotEqualTo(conferenceParticipant2);
    }

    @Test
    void conferenceTest() {
        ConferenceParticipant conferenceParticipant = getConferenceParticipantRandomSampleGenerator();
        Conference conferenceBack = getConferenceRandomSampleGenerator();

        conferenceParticipant.setConference(conferenceBack);
        assertThat(conferenceParticipant.getConference()).isEqualTo(conferenceBack);

        conferenceParticipant.conference(null);
        assertThat(conferenceParticipant.getConference()).isNull();
    }
}
