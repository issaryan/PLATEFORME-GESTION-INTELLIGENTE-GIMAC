package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SuspensTransitionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SuspensTransitionDTO.class);
        SuspensTransitionDTO suspensTransitionDTO1 = new SuspensTransitionDTO();
        suspensTransitionDTO1.setId(UUID.randomUUID());
        SuspensTransitionDTO suspensTransitionDTO2 = new SuspensTransitionDTO();
        assertThat(suspensTransitionDTO1).isNotEqualTo(suspensTransitionDTO2);
        suspensTransitionDTO2.setId(suspensTransitionDTO1.getId());
        assertThat(suspensTransitionDTO1).isEqualTo(suspensTransitionDTO2);
        suspensTransitionDTO2.setId(UUID.randomUUID());
        assertThat(suspensTransitionDTO1).isNotEqualTo(suspensTransitionDTO2);
        suspensTransitionDTO1.setId(null);
        assertThat(suspensTransitionDTO1).isNotEqualTo(suspensTransitionDTO2);
    }
}
