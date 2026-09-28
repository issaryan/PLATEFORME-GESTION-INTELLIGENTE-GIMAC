package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IndicatorDefinitionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(IndicatorDefinitionDTO.class);
        IndicatorDefinitionDTO indicatorDefinitionDTO1 = new IndicatorDefinitionDTO();
        indicatorDefinitionDTO1.setId(UUID.randomUUID());
        IndicatorDefinitionDTO indicatorDefinitionDTO2 = new IndicatorDefinitionDTO();
        assertThat(indicatorDefinitionDTO1).isNotEqualTo(indicatorDefinitionDTO2);
        indicatorDefinitionDTO2.setId(indicatorDefinitionDTO1.getId());
        assertThat(indicatorDefinitionDTO1).isEqualTo(indicatorDefinitionDTO2);
        indicatorDefinitionDTO2.setId(UUID.randomUUID());
        assertThat(indicatorDefinitionDTO1).isNotEqualTo(indicatorDefinitionDTO2);
        indicatorDefinitionDTO1.setId(null);
        assertThat(indicatorDefinitionDTO1).isNotEqualTo(indicatorDefinitionDTO2);
    }
}
