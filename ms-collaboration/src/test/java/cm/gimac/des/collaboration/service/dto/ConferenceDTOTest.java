package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConferenceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConferenceDTO.class);
        ConferenceDTO conferenceDTO1 = new ConferenceDTO();
        conferenceDTO1.setId(UUID.randomUUID());
        ConferenceDTO conferenceDTO2 = new ConferenceDTO();
        assertThat(conferenceDTO1).isNotEqualTo(conferenceDTO2);
        conferenceDTO2.setId(conferenceDTO1.getId());
        assertThat(conferenceDTO1).isEqualTo(conferenceDTO2);
        conferenceDTO2.setId(UUID.randomUUID());
        assertThat(conferenceDTO1).isNotEqualTo(conferenceDTO2);
        conferenceDTO1.setId(null);
        assertThat(conferenceDTO1).isNotEqualTo(conferenceDTO2);
    }
}
