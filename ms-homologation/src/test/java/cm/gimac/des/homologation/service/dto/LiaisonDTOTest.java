package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LiaisonDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(LiaisonDTO.class);
        LiaisonDTO liaisonDTO1 = new LiaisonDTO();
        liaisonDTO1.setId(UUID.randomUUID());
        LiaisonDTO liaisonDTO2 = new LiaisonDTO();
        assertThat(liaisonDTO1).isNotEqualTo(liaisonDTO2);
        liaisonDTO2.setId(liaisonDTO1.getId());
        assertThat(liaisonDTO1).isEqualTo(liaisonDTO2);
        liaisonDTO2.setId(UUID.randomUUID());
        assertThat(liaisonDTO1).isNotEqualTo(liaisonDTO2);
        liaisonDTO1.setId(null);
        assertThat(liaisonDTO1).isNotEqualTo(liaisonDTO2);
    }
}
