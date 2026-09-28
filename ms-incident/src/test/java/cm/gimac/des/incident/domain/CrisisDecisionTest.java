package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.CrisisCellTestSamples.*;
import static cm.gimac.des.incident.domain.CrisisDecisionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CrisisDecisionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CrisisDecision.class);
        CrisisDecision crisisDecision1 = getCrisisDecisionSample1();
        CrisisDecision crisisDecision2 = new CrisisDecision();
        assertThat(crisisDecision1).isNotEqualTo(crisisDecision2);

        crisisDecision2.setId(crisisDecision1.getId());
        assertThat(crisisDecision1).isEqualTo(crisisDecision2);

        crisisDecision2 = getCrisisDecisionSample2();
        assertThat(crisisDecision1).isNotEqualTo(crisisDecision2);
    }

    @Test
    void cellTest() {
        CrisisDecision crisisDecision = getCrisisDecisionRandomSampleGenerator();
        CrisisCell crisisCellBack = getCrisisCellRandomSampleGenerator();

        crisisDecision.setCell(crisisCellBack);
        assertThat(crisisDecision.getCell()).isEqualTo(crisisCellBack);

        crisisDecision.cell(null);
        assertThat(crisisDecision.getCell()).isNull();
    }
}
