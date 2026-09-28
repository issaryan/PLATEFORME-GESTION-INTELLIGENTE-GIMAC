package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoverageDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CoverageDTO.class);
        CoverageDTO coverageDTO1 = new CoverageDTO();
        coverageDTO1.setId(UUID.randomUUID());
        CoverageDTO coverageDTO2 = new CoverageDTO();
        assertThat(coverageDTO1).isNotEqualTo(coverageDTO2);
        coverageDTO2.setId(coverageDTO1.getId());
        assertThat(coverageDTO1).isEqualTo(coverageDTO2);
        coverageDTO2.setId(UUID.randomUUID());
        assertThat(coverageDTO1).isNotEqualTo(coverageDTO2);
        coverageDTO1.setId(null);
        assertThat(coverageDTO1).isNotEqualTo(coverageDTO2);
    }
}
