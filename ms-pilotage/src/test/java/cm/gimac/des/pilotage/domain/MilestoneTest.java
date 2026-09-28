package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.MilestoneTestSamples.*;
import static cm.gimac.des.pilotage.domain.MilestoneTestSamples.*;
import static cm.gimac.des.pilotage.domain.ProjectTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MilestoneTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Milestone.class);
        Milestone milestone1 = getMilestoneSample1();
        Milestone milestone2 = new Milestone();
        assertThat(milestone1).isNotEqualTo(milestone2);

        milestone2.setId(milestone1.getId());
        assertThat(milestone1).isEqualTo(milestone2);

        milestone2 = getMilestoneSample2();
        assertThat(milestone1).isNotEqualTo(milestone2);
    }

    @Test
    void projectTest() {
        Milestone milestone = getMilestoneRandomSampleGenerator();
        Project projectBack = getProjectRandomSampleGenerator();

        milestone.setProject(projectBack);
        assertThat(milestone.getProject()).isEqualTo(projectBack);

        milestone.project(null);
        assertThat(milestone.getProject()).isNull();
    }

    @Test
    void predecessorsTest() {
        Milestone milestone = getMilestoneRandomSampleGenerator();
        Milestone milestoneBack = getMilestoneRandomSampleGenerator();

        milestone.addPredecessors(milestoneBack);
        assertThat(milestone.getPredecessors()).containsOnly(milestoneBack);

        milestone.removePredecessors(milestoneBack);
        assertThat(milestone.getPredecessors()).doesNotContain(milestoneBack);

        milestone.predecessors(new HashSet<>(Set.of(milestoneBack)));
        assertThat(milestone.getPredecessors()).containsOnly(milestoneBack);

        milestone.setPredecessors(new HashSet<>());
        assertThat(milestone.getPredecessors()).doesNotContain(milestoneBack);
    }

    @Test
    void successorsTest() {
        Milestone milestone = getMilestoneRandomSampleGenerator();
        Milestone milestoneBack = getMilestoneRandomSampleGenerator();

        milestone.addSuccessors(milestoneBack);
        assertThat(milestone.getSuccessors()).containsOnly(milestoneBack);
        assertThat(milestoneBack.getPredecessors()).containsOnly(milestone);

        milestone.removeSuccessors(milestoneBack);
        assertThat(milestone.getSuccessors()).doesNotContain(milestoneBack);
        assertThat(milestoneBack.getPredecessors()).doesNotContain(milestone);

        milestone.successors(new HashSet<>(Set.of(milestoneBack)));
        assertThat(milestone.getSuccessors()).containsOnly(milestoneBack);
        assertThat(milestoneBack.getPredecessors()).containsOnly(milestone);

        milestone.setSuccessors(new HashSet<>());
        assertThat(milestone.getSuccessors()).doesNotContain(milestoneBack);
        assertThat(milestoneBack.getPredecessors()).doesNotContain(milestone);
    }
}
