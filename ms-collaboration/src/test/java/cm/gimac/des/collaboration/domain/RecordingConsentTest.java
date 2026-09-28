package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.ConferenceTestSamples.*;
import static cm.gimac.des.collaboration.domain.RecordingConsentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RecordingConsentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RecordingConsent.class);
        RecordingConsent recordingConsent1 = getRecordingConsentSample1();
        RecordingConsent recordingConsent2 = new RecordingConsent();
        assertThat(recordingConsent1).isNotEqualTo(recordingConsent2);

        recordingConsent2.setId(recordingConsent1.getId());
        assertThat(recordingConsent1).isEqualTo(recordingConsent2);

        recordingConsent2 = getRecordingConsentSample2();
        assertThat(recordingConsent1).isNotEqualTo(recordingConsent2);
    }

    @Test
    void conferenceTest() {
        RecordingConsent recordingConsent = getRecordingConsentRandomSampleGenerator();
        Conference conferenceBack = getConferenceRandomSampleGenerator();

        recordingConsent.setConference(conferenceBack);
        assertThat(recordingConsent.getConference()).isEqualTo(conferenceBack);

        recordingConsent.conference(null);
        assertThat(recordingConsent.getConference()).isNull();
    }
}
