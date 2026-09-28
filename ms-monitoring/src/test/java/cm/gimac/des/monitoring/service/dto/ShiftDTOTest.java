package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ShiftDTO.class);
        ShiftDTO shiftDTO1 = new ShiftDTO();
        shiftDTO1.setId(UUID.randomUUID());
        ShiftDTO shiftDTO2 = new ShiftDTO();
        assertThat(shiftDTO1).isNotEqualTo(shiftDTO2);
        shiftDTO2.setId(shiftDTO1.getId());
        assertThat(shiftDTO1).isEqualTo(shiftDTO2);
        shiftDTO2.setId(UUID.randomUUID());
        assertThat(shiftDTO1).isNotEqualTo(shiftDTO2);
        shiftDTO1.setId(null);
        assertThat(shiftDTO1).isNotEqualTo(shiftDTO2);
    }
}
