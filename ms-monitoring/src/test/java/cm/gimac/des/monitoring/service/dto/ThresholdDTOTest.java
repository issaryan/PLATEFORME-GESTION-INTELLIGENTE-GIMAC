package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ThresholdDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ThresholdDTO.class);
        ThresholdDTO thresholdDTO1 = new ThresholdDTO();
        thresholdDTO1.setId(UUID.randomUUID());
        ThresholdDTO thresholdDTO2 = new ThresholdDTO();
        assertThat(thresholdDTO1).isNotEqualTo(thresholdDTO2);
        thresholdDTO2.setId(thresholdDTO1.getId());
        assertThat(thresholdDTO1).isEqualTo(thresholdDTO2);
        thresholdDTO2.setId(UUID.randomUUID());
        assertThat(thresholdDTO1).isNotEqualTo(thresholdDTO2);
        thresholdDTO1.setId(null);
        assertThat(thresholdDTO1).isNotEqualTo(thresholdDTO2);
    }
}
