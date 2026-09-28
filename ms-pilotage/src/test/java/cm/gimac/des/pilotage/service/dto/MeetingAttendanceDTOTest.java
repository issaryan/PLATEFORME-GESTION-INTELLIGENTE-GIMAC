package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeetingAttendanceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MeetingAttendanceDTO.class);
        MeetingAttendanceDTO meetingAttendanceDTO1 = new MeetingAttendanceDTO();
        meetingAttendanceDTO1.setId(UUID.randomUUID());
        MeetingAttendanceDTO meetingAttendanceDTO2 = new MeetingAttendanceDTO();
        assertThat(meetingAttendanceDTO1).isNotEqualTo(meetingAttendanceDTO2);
        meetingAttendanceDTO2.setId(meetingAttendanceDTO1.getId());
        assertThat(meetingAttendanceDTO1).isEqualTo(meetingAttendanceDTO2);
        meetingAttendanceDTO2.setId(UUID.randomUUID());
        assertThat(meetingAttendanceDTO1).isNotEqualTo(meetingAttendanceDTO2);
        meetingAttendanceDTO1.setId(null);
        assertThat(meetingAttendanceDTO1).isNotEqualTo(meetingAttendanceDTO2);
    }
}
