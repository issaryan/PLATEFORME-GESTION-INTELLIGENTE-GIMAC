package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RiskAssessmentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RiskAssessmentDTO.class);
        RiskAssessmentDTO riskAssessmentDTO1 = new RiskAssessmentDTO();
        riskAssessmentDTO1.setId(UUID.randomUUID());
        RiskAssessmentDTO riskAssessmentDTO2 = new RiskAssessmentDTO();
        assertThat(riskAssessmentDTO1).isNotEqualTo(riskAssessmentDTO2);
        riskAssessmentDTO2.setId(riskAssessmentDTO1.getId());
        assertThat(riskAssessmentDTO1).isEqualTo(riskAssessmentDTO2);
        riskAssessmentDTO2.setId(UUID.randomUUID());
        assertThat(riskAssessmentDTO1).isNotEqualTo(riskAssessmentDTO2);
        riskAssessmentDTO1.setId(null);
        assertThat(riskAssessmentDTO1).isNotEqualTo(riskAssessmentDTO2);
    }
}
