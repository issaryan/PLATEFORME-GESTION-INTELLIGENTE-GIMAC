package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EngagementFreezeDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EngagementFreezeDTO.class);
        EngagementFreezeDTO engagementFreezeDTO1 = new EngagementFreezeDTO();
        engagementFreezeDTO1.setId(UUID.randomUUID());
        EngagementFreezeDTO engagementFreezeDTO2 = new EngagementFreezeDTO();
        assertThat(engagementFreezeDTO1).isNotEqualTo(engagementFreezeDTO2);
        engagementFreezeDTO2.setId(engagementFreezeDTO1.getId());
        assertThat(engagementFreezeDTO1).isEqualTo(engagementFreezeDTO2);
        engagementFreezeDTO2.setId(UUID.randomUUID());
        assertThat(engagementFreezeDTO1).isNotEqualTo(engagementFreezeDTO2);
        engagementFreezeDTO1.setId(null);
        assertThat(engagementFreezeDTO1).isNotEqualTo(engagementFreezeDTO2);
    }
}
