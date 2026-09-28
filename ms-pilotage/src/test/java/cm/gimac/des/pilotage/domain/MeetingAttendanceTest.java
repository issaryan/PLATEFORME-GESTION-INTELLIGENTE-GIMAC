package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.MeetingAttendanceTestSamples.*;
import static cm.gimac.des.pilotage.domain.MeetingTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MeetingAttendanceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MeetingAttendance.class);
        MeetingAttendance meetingAttendance1 = getMeetingAttendanceSample1();
        MeetingAttendance meetingAttendance2 = new MeetingAttendance();
        assertThat(meetingAttendance1).isNotEqualTo(meetingAttendance2);

        meetingAttendance2.setId(meetingAttendance1.getId());
        assertThat(meetingAttendance1).isEqualTo(meetingAttendance2);

        meetingAttendance2 = getMeetingAttendanceSample2();
        assertThat(meetingAttendance1).isNotEqualTo(meetingAttendance2);
    }

    @Test
    void meetingTest() {
        MeetingAttendance meetingAttendance = getMeetingAttendanceRandomSampleGenerator();
        Meeting meetingBack = getMeetingRandomSampleGenerator();

        meetingAttendance.setMeeting(meetingBack);
        assertThat(meetingAttendance.getMeeting()).isEqualTo(meetingBack);

        meetingAttendance.meeting(null);
        assertThat(meetingAttendance.getMeeting()).isNull();
    }
}
