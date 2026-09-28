package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.ConferenceTestSamples.*;
import static cm.gimac.des.collaboration.domain.RecordingSegmentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RecordingSegmentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RecordingSegment.class);
        RecordingSegment recordingSegment1 = getRecordingSegmentSample1();
        RecordingSegment recordingSegment2 = new RecordingSegment();
        assertThat(recordingSegment1).isNotEqualTo(recordingSegment2);

        recordingSegment2.setId(recordingSegment1.getId());
        assertThat(recordingSegment1).isEqualTo(recordingSegment2);

        recordingSegment2 = getRecordingSegmentSample2();
        assertThat(recordingSegment1).isNotEqualTo(recordingSegment2);
    }

    @Test
    void conferenceTest() {
        RecordingSegment recordingSegment = getRecordingSegmentRandomSampleGenerator();
        Conference conferenceBack = getConferenceRandomSampleGenerator();

        recordingSegment.setConference(conferenceBack);
        assertThat(recordingSegment.getConference()).isEqualTo(conferenceBack);

        recordingSegment.conference(null);
        assertThat(recordingSegment.getConference()).isNull();
    }
}
