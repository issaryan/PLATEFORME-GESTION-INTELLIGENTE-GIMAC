package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MilestoneDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MilestoneDTO.class);
        MilestoneDTO milestoneDTO1 = new MilestoneDTO();
        milestoneDTO1.setId(UUID.randomUUID());
        MilestoneDTO milestoneDTO2 = new MilestoneDTO();
        assertThat(milestoneDTO1).isNotEqualTo(milestoneDTO2);
        milestoneDTO2.setId(milestoneDTO1.getId());
        assertThat(milestoneDTO1).isEqualTo(milestoneDTO2);
        milestoneDTO2.setId(UUID.randomUUID());
        assertThat(milestoneDTO1).isNotEqualTo(milestoneDTO2);
        milestoneDTO1.setId(null);
        assertThat(milestoneDTO1).isNotEqualTo(milestoneDTO2);
    }
}
