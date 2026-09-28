package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CockpitViewDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CockpitViewDTO.class);
        CockpitViewDTO cockpitViewDTO1 = new CockpitViewDTO();
        cockpitViewDTO1.setId(UUID.randomUUID());
        CockpitViewDTO cockpitViewDTO2 = new CockpitViewDTO();
        assertThat(cockpitViewDTO1).isNotEqualTo(cockpitViewDTO2);
        cockpitViewDTO2.setId(cockpitViewDTO1.getId());
        assertThat(cockpitViewDTO1).isEqualTo(cockpitViewDTO2);
        cockpitViewDTO2.setId(UUID.randomUUID());
        assertThat(cockpitViewDTO1).isNotEqualTo(cockpitViewDTO2);
        cockpitViewDTO1.setId(null);
        assertThat(cockpitViewDTO1).isNotEqualTo(cockpitViewDTO2);
    }
}
