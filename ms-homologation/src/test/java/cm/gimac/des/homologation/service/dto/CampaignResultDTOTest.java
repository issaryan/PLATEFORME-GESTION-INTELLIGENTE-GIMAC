package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignResultDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CampaignResultDTO.class);
        CampaignResultDTO campaignResultDTO1 = new CampaignResultDTO();
        campaignResultDTO1.setId(UUID.randomUUID());
        CampaignResultDTO campaignResultDTO2 = new CampaignResultDTO();
        assertThat(campaignResultDTO1).isNotEqualTo(campaignResultDTO2);
        campaignResultDTO2.setId(campaignResultDTO1.getId());
        assertThat(campaignResultDTO1).isEqualTo(campaignResultDTO2);
        campaignResultDTO2.setId(UUID.randomUUID());
        assertThat(campaignResultDTO1).isNotEqualTo(campaignResultDTO2);
        campaignResultDTO1.setId(null);
        assertThat(campaignResultDTO1).isNotEqualTo(campaignResultDTO2);
    }
}
