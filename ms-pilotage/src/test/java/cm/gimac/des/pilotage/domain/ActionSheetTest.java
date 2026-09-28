package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.ActionSheetTestSamples.*;
import static cm.gimac.des.pilotage.domain.RiskTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ActionSheetTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ActionSheet.class);
        ActionSheet actionSheet1 = getActionSheetSample1();
        ActionSheet actionSheet2 = new ActionSheet();
        assertThat(actionSheet1).isNotEqualTo(actionSheet2);

        actionSheet2.setId(actionSheet1.getId());
        assertThat(actionSheet1).isEqualTo(actionSheet2);

        actionSheet2 = getActionSheetSample2();
        assertThat(actionSheet1).isNotEqualTo(actionSheet2);
    }

    @Test
    void riskTest() {
        ActionSheet actionSheet = getActionSheetRandomSampleGenerator();
        Risk riskBack = getRiskRandomSampleGenerator();

        actionSheet.setRisk(riskBack);
        assertThat(actionSheet.getRisk()).isEqualTo(riskBack);

        actionSheet.risk(null);
        assertThat(actionSheet.getRisk()).isNull();
    }
}
