package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DimensionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DimensionDTO.class);
        DimensionDTO dimensionDTO1 = new DimensionDTO();
        dimensionDTO1.setId(UUID.randomUUID());
        DimensionDTO dimensionDTO2 = new DimensionDTO();
        assertThat(dimensionDTO1).isNotEqualTo(dimensionDTO2);
        dimensionDTO2.setId(dimensionDTO1.getId());
        assertThat(dimensionDTO1).isEqualTo(dimensionDTO2);
        dimensionDTO2.setId(UUID.randomUUID());
        assertThat(dimensionDTO1).isNotEqualTo(dimensionDTO2);
        dimensionDTO1.setId(null);
        assertThat(dimensionDTO1).isNotEqualTo(dimensionDTO2);
    }
}
