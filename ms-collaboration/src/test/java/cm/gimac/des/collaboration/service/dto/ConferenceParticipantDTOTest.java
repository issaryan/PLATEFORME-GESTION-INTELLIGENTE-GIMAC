package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConferenceParticipantDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConferenceParticipantDTO.class);
        ConferenceParticipantDTO conferenceParticipantDTO1 = new ConferenceParticipantDTO();
        conferenceParticipantDTO1.setId(UUID.randomUUID());
        ConferenceParticipantDTO conferenceParticipantDTO2 = new ConferenceParticipantDTO();
        assertThat(conferenceParticipantDTO1).isNotEqualTo(conferenceParticipantDTO2);
        conferenceParticipantDTO2.setId(conferenceParticipantDTO1.getId());
        assertThat(conferenceParticipantDTO1).isEqualTo(conferenceParticipantDTO2);
        conferenceParticipantDTO2.setId(UUID.randomUUID());
        assertThat(conferenceParticipantDTO1).isNotEqualTo(conferenceParticipantDTO2);
        conferenceParticipantDTO1.setId(null);
        assertThat(conferenceParticipantDTO1).isNotEqualTo(conferenceParticipantDTO2);
    }
}
