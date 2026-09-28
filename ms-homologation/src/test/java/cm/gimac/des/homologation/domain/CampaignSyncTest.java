package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.CampaignSyncTestSamples.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CampaignSyncTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CampaignSync.class);
        CampaignSync campaignSync1 = getCampaignSyncSample1();
        CampaignSync campaignSync2 = new CampaignSync();
        assertThat(campaignSync1).isNotEqualTo(campaignSync2);

        campaignSync2.setId(campaignSync1.getId());
        assertThat(campaignSync1).isEqualTo(campaignSync2);

        campaignSync2 = getCampaignSyncSample2();
        assertThat(campaignSync1).isNotEqualTo(campaignSync2);
    }

    @Test
    void liaisonTest() {
        CampaignSync campaignSync = getCampaignSyncRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        campaignSync.setLiaison(liaisonBack);
        assertThat(campaignSync.getLiaison()).isEqualTo(liaisonBack);

        campaignSync.liaison(null);
        assertThat(campaignSync.getLiaison()).isNull();
    }
}
