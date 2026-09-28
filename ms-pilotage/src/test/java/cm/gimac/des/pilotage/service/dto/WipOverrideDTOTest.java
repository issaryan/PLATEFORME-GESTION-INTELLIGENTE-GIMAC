package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WipOverrideDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(WipOverrideDTO.class);
        WipOverrideDTO wipOverrideDTO1 = new WipOverrideDTO();
        wipOverrideDTO1.setId(UUID.randomUUID());
        WipOverrideDTO wipOverrideDTO2 = new WipOverrideDTO();
        assertThat(wipOverrideDTO1).isNotEqualTo(wipOverrideDTO2);
        wipOverrideDTO2.setId(wipOverrideDTO1.getId());
        assertThat(wipOverrideDTO1).isEqualTo(wipOverrideDTO2);
        wipOverrideDTO2.setId(UUID.randomUUID());
        assertThat(wipOverrideDTO1).isNotEqualTo(wipOverrideDTO2);
        wipOverrideDTO1.setId(null);
        assertThat(wipOverrideDTO1).isNotEqualTo(wipOverrideDTO2);
    }
}
