package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.CampaignSyncTestSamples.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static cm.gimac.des.homologation.domain.ReferenceCatalogTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LiaisonTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Liaison.class);
        Liaison liaison1 = getLiaisonSample1();
        Liaison liaison2 = new Liaison();
        assertThat(liaison1).isNotEqualTo(liaison2);

        liaison2.setId(liaison1.getId());
        assertThat(liaison1).isEqualTo(liaison2);

        liaison2 = getLiaisonSample2();
        assertThat(liaison1).isNotEqualTo(liaison2);
    }

    @Test
    void catalogTest() {
        Liaison liaison = getLiaisonRandomSampleGenerator();
        ReferenceCatalog referenceCatalogBack = getReferenceCatalogRandomSampleGenerator();

        liaison.setCatalog(referenceCatalogBack);
        assertThat(liaison.getCatalog()).isEqualTo(referenceCatalogBack);

        liaison.catalog(null);
        assertThat(liaison.getCatalog()).isNull();
    }

    @Test
    void campaignSyncTest() {
        Liaison liaison = getLiaisonRandomSampleGenerator();
        CampaignSync campaignSyncBack = getCampaignSyncRandomSampleGenerator();

        liaison.setCampaignSync(campaignSyncBack);
        assertThat(liaison.getCampaignSync()).isEqualTo(campaignSyncBack);
        assertThat(campaignSyncBack.getLiaison()).isEqualTo(liaison);

        liaison.campaignSync(null);
        assertThat(liaison.getCampaignSync()).isNull();
        assertThat(campaignSyncBack.getLiaison()).isNull();
    }
}
