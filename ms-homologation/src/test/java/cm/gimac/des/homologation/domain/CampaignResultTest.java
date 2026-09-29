package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.CampaignResultTestSamples.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static cm.gimac.des.homologation.domain.OperationInstanceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CampaignResultTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CampaignResult.class);
        CampaignResult campaignResult1 = getCampaignResultSample1();
        CampaignResult campaignResult2 = new CampaignResult();
        assertThat(campaignResult1).isNotEqualTo(campaignResult2);

        campaignResult2.setId(campaignResult1.getId());
        assertThat(campaignResult1).isEqualTo(campaignResult2);

        campaignResult2 = getCampaignResultSample2();
        assertThat(campaignResult1).isNotEqualTo(campaignResult2);
    }

    @Test
    void liaisonTest() {
        CampaignResult campaignResult = getCampaignResultRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        campaignResult.setLiaison(liaisonBack);
        assertThat(campaignResult.getLiaison()).isEqualTo(liaisonBack);

        campaignResult.liaison(null);
        assertThat(campaignResult.getLiaison()).isNull();
    }

    @Test
    void operationTest() {
        CampaignResult campaignResult = getCampaignResultRandomSampleGenerator();
        OperationInstance operationInstanceBack = getOperationInstanceRandomSampleGenerator();

        campaignResult.setOperation(operationInstanceBack);
        assertThat(campaignResult.getOperation()).isEqualTo(operationInstanceBack);

        campaignResult.operation(null);
        assertThat(campaignResult.getOperation()).isNull();
    }
}
