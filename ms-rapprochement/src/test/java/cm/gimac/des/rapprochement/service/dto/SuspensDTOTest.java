package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SuspensDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SuspensDTO.class);
        SuspensDTO suspensDTO1 = new SuspensDTO();
        suspensDTO1.setId(UUID.randomUUID());
        SuspensDTO suspensDTO2 = new SuspensDTO();
        assertThat(suspensDTO1).isNotEqualTo(suspensDTO2);
        suspensDTO2.setId(suspensDTO1.getId());
        assertThat(suspensDTO1).isEqualTo(suspensDTO2);
        suspensDTO2.setId(UUID.randomUUID());
        assertThat(suspensDTO1).isNotEqualTo(suspensDTO2);
        suspensDTO1.setId(null);
        assertThat(suspensDTO1).isNotEqualTo(suspensDTO2);
    }
}
