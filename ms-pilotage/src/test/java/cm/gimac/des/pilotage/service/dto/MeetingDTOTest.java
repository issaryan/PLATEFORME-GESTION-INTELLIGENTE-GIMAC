package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeetingDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MeetingDTO.class);
        MeetingDTO meetingDTO1 = new MeetingDTO();
        meetingDTO1.setId(UUID.randomUUID());
        MeetingDTO meetingDTO2 = new MeetingDTO();
        assertThat(meetingDTO1).isNotEqualTo(meetingDTO2);
        meetingDTO2.setId(meetingDTO1.getId());
        assertThat(meetingDTO1).isEqualTo(meetingDTO2);
        meetingDTO2.setId(UUID.randomUUID());
        assertThat(meetingDTO1).isNotEqualTo(meetingDTO2);
        meetingDTO1.setId(null);
        assertThat(meetingDTO1).isNotEqualTo(meetingDTO2);
    }
}
