package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.ProjectTestSamples.*;
import static cm.gimac.des.pilotage.domain.RiskTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RiskTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Risk.class);
        Risk risk1 = getRiskSample1();
        Risk risk2 = new Risk();
        assertThat(risk1).isNotEqualTo(risk2);

        risk2.setId(risk1.getId());
        assertThat(risk1).isEqualTo(risk2);

        risk2 = getRiskSample2();
        assertThat(risk1).isNotEqualTo(risk2);
    }

    @Test
    void projectTest() {
        Risk risk = getRiskRandomSampleGenerator();
        Project projectBack = getProjectRandomSampleGenerator();

        risk.setProject(projectBack);
        assertThat(risk.getProject()).isEqualTo(projectBack);

        risk.project(null);
        assertThat(risk.getProject()).isNull();
    }
}
