package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.MilestoneApprovalTestSamples.*;
import static cm.gimac.des.pilotage.domain.MilestoneTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MilestoneApprovalTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MilestoneApproval.class);
        MilestoneApproval milestoneApproval1 = getMilestoneApprovalSample1();
        MilestoneApproval milestoneApproval2 = new MilestoneApproval();
        assertThat(milestoneApproval1).isNotEqualTo(milestoneApproval2);

        milestoneApproval2.setId(milestoneApproval1.getId());
        assertThat(milestoneApproval1).isEqualTo(milestoneApproval2);

        milestoneApproval2 = getMilestoneApprovalSample2();
        assertThat(milestoneApproval1).isNotEqualTo(milestoneApproval2);
    }

    @Test
    void milestoneTest() {
        MilestoneApproval milestoneApproval = getMilestoneApprovalRandomSampleGenerator();
        Milestone milestoneBack = getMilestoneRandomSampleGenerator();

        milestoneApproval.setMilestone(milestoneBack);
        assertThat(milestoneApproval.getMilestone()).isEqualTo(milestoneBack);

        milestoneApproval.milestone(null);
        assertThat(milestoneApproval.getMilestone()).isNull();
    }
}
