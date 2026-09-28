package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.AcceptanceCriterionTestSamples.*;
import static cm.gimac.des.pilotage.domain.TaskTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AcceptanceCriterionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AcceptanceCriterion.class);
        AcceptanceCriterion acceptanceCriterion1 = getAcceptanceCriterionSample1();
        AcceptanceCriterion acceptanceCriterion2 = new AcceptanceCriterion();
        assertThat(acceptanceCriterion1).isNotEqualTo(acceptanceCriterion2);

        acceptanceCriterion2.setId(acceptanceCriterion1.getId());
        assertThat(acceptanceCriterion1).isEqualTo(acceptanceCriterion2);

        acceptanceCriterion2 = getAcceptanceCriterionSample2();
        assertThat(acceptanceCriterion1).isNotEqualTo(acceptanceCriterion2);
    }

    @Test
    void taskTest() {
        AcceptanceCriterion acceptanceCriterion = getAcceptanceCriterionRandomSampleGenerator();
        Task taskBack = getTaskRandomSampleGenerator();

        acceptanceCriterion.setTask(taskBack);
        assertThat(acceptanceCriterion.getTask()).isEqualTo(taskBack);

        acceptanceCriterion.task(null);
        assertThat(acceptanceCriterion.getTask()).isNull();
    }
}
