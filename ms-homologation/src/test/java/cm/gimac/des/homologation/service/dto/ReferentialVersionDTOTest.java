package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReferentialVersionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReferentialVersionDTO.class);
        ReferentialVersionDTO referentialVersionDTO1 = new ReferentialVersionDTO();
        referentialVersionDTO1.setId(UUID.randomUUID());
        ReferentialVersionDTO referentialVersionDTO2 = new ReferentialVersionDTO();
        assertThat(referentialVersionDTO1).isNotEqualTo(referentialVersionDTO2);
        referentialVersionDTO2.setId(referentialVersionDTO1.getId());
        assertThat(referentialVersionDTO1).isEqualTo(referentialVersionDTO2);
        referentialVersionDTO2.setId(UUID.randomUUID());
        assertThat(referentialVersionDTO1).isNotEqualTo(referentialVersionDTO2);
        referentialVersionDTO1.setId(null);
        assertThat(referentialVersionDTO1).isNotEqualTo(referentialVersionDTO2);
    }
}
