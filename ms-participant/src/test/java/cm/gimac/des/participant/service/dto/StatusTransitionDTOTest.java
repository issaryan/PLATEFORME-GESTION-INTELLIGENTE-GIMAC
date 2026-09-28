package cm.gimac.des.participant.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StatusTransitionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(StatusTransitionDTO.class);
        StatusTransitionDTO statusTransitionDTO1 = new StatusTransitionDTO();
        statusTransitionDTO1.setId(UUID.randomUUID());
        StatusTransitionDTO statusTransitionDTO2 = new StatusTransitionDTO();
        assertThat(statusTransitionDTO1).isNotEqualTo(statusTransitionDTO2);
        statusTransitionDTO2.setId(statusTransitionDTO1.getId());
        assertThat(statusTransitionDTO1).isEqualTo(statusTransitionDTO2);
        statusTransitionDTO2.setId(UUID.randomUUID());
        assertThat(statusTransitionDTO1).isNotEqualTo(statusTransitionDTO2);
        statusTransitionDTO1.setId(null);
        assertThat(statusTransitionDTO1).isNotEqualTo(statusTransitionDTO2);
    }
}
