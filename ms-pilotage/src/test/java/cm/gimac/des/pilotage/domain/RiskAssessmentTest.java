package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.RiskAssessmentTestSamples.*;
import static cm.gimac.des.pilotage.domain.RiskTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RiskAssessmentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RiskAssessment.class);
        RiskAssessment riskAssessment1 = getRiskAssessmentSample1();
        RiskAssessment riskAssessment2 = new RiskAssessment();
        assertThat(riskAssessment1).isNotEqualTo(riskAssessment2);

        riskAssessment2.setId(riskAssessment1.getId());
        assertThat(riskAssessment1).isEqualTo(riskAssessment2);

        riskAssessment2 = getRiskAssessmentSample2();
        assertThat(riskAssessment1).isNotEqualTo(riskAssessment2);
    }

    @Test
    void riskTest() {
        RiskAssessment riskAssessment = getRiskAssessmentRandomSampleGenerator();
        Risk riskBack = getRiskRandomSampleGenerator();

        riskAssessment.setRisk(riskBack);
        assertThat(riskAssessment.getRisk()).isEqualTo(riskBack);

        riskAssessment.risk(null);
        assertThat(riskAssessment.getRisk()).isNull();
    }
}
