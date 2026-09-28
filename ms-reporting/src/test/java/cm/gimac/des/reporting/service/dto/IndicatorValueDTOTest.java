package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IndicatorValueDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(IndicatorValueDTO.class);
        IndicatorValueDTO indicatorValueDTO1 = new IndicatorValueDTO();
        indicatorValueDTO1.setId(UUID.randomUUID());
        IndicatorValueDTO indicatorValueDTO2 = new IndicatorValueDTO();
        assertThat(indicatorValueDTO1).isNotEqualTo(indicatorValueDTO2);
        indicatorValueDTO2.setId(indicatorValueDTO1.getId());
        assertThat(indicatorValueDTO1).isEqualTo(indicatorValueDTO2);
        indicatorValueDTO2.setId(UUID.randomUUID());
        assertThat(indicatorValueDTO1).isNotEqualTo(indicatorValueDTO2);
        indicatorValueDTO1.setId(null);
        assertThat(indicatorValueDTO1).isNotEqualTo(indicatorValueDTO2);
    }
}
