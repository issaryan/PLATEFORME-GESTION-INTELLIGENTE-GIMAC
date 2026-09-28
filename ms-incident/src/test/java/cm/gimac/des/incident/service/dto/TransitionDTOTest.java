package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransitionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(TransitionDTO.class);
        TransitionDTO transitionDTO1 = new TransitionDTO();
        transitionDTO1.setId(UUID.randomUUID());
        TransitionDTO transitionDTO2 = new TransitionDTO();
        assertThat(transitionDTO1).isNotEqualTo(transitionDTO2);
        transitionDTO2.setId(transitionDTO1.getId());
        assertThat(transitionDTO1).isEqualTo(transitionDTO2);
        transitionDTO2.setId(UUID.randomUUID());
        assertThat(transitionDTO1).isNotEqualTo(transitionDTO2);
        transitionDTO1.setId(null);
        assertThat(transitionDTO1).isNotEqualTo(transitionDTO2);
    }
}
