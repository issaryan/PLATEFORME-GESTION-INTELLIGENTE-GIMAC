package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignSyncDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CampaignSyncDTO.class);
        CampaignSyncDTO campaignSyncDTO1 = new CampaignSyncDTO();
        campaignSyncDTO1.setId(UUID.randomUUID());
        CampaignSyncDTO campaignSyncDTO2 = new CampaignSyncDTO();
        assertThat(campaignSyncDTO1).isNotEqualTo(campaignSyncDTO2);
        campaignSyncDTO2.setId(campaignSyncDTO1.getId());
        assertThat(campaignSyncDTO1).isEqualTo(campaignSyncDTO2);
        campaignSyncDTO2.setId(UUID.randomUUID());
        assertThat(campaignSyncDTO1).isNotEqualTo(campaignSyncDTO2);
        campaignSyncDTO1.setId(null);
        assertThat(campaignSyncDTO1).isNotEqualTo(campaignSyncDTO2);
    }
}
