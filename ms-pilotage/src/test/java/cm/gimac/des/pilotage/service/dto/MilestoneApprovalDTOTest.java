package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MilestoneApprovalDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MilestoneApprovalDTO.class);
        MilestoneApprovalDTO milestoneApprovalDTO1 = new MilestoneApprovalDTO();
        milestoneApprovalDTO1.setId(UUID.randomUUID());
        MilestoneApprovalDTO milestoneApprovalDTO2 = new MilestoneApprovalDTO();
        assertThat(milestoneApprovalDTO1).isNotEqualTo(milestoneApprovalDTO2);
        milestoneApprovalDTO2.setId(milestoneApprovalDTO1.getId());
        assertThat(milestoneApprovalDTO1).isEqualTo(milestoneApprovalDTO2);
        milestoneApprovalDTO2.setId(UUID.randomUUID());
        assertThat(milestoneApprovalDTO1).isNotEqualTo(milestoneApprovalDTO2);
        milestoneApprovalDTO1.setId(null);
        assertThat(milestoneApprovalDTO1).isNotEqualTo(milestoneApprovalDTO2);
    }
}
