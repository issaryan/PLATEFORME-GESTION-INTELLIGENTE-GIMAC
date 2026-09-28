package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.DecisionTestSamples.*;
import static cm.gimac.des.pilotage.domain.DecisionTestSamples.*;
import static cm.gimac.des.pilotage.domain.MeetingTestSamples.*;
import static cm.gimac.des.pilotage.domain.TaskTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DecisionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Decision.class);
        Decision decision1 = getDecisionSample1();
        Decision decision2 = new Decision();
        assertThat(decision1).isNotEqualTo(decision2);

        decision2.setId(decision1.getId());
        assertThat(decision1).isEqualTo(decision2);

        decision2 = getDecisionSample2();
        assertThat(decision1).isNotEqualTo(decision2);
    }

    @Test
    void taskTest() {
        Decision decision = getDecisionRandomSampleGenerator();
        Task taskBack = getTaskRandomSampleGenerator();

        decision.setTask(taskBack);
        assertThat(decision.getTask()).isEqualTo(taskBack);

        decision.task(null);
        assertThat(decision.getTask()).isNull();
    }

    @Test
    void meetingTest() {
        Decision decision = getDecisionRandomSampleGenerator();
        Meeting meetingBack = getMeetingRandomSampleGenerator();

        decision.setMeeting(meetingBack);
        assertThat(decision.getMeeting()).isEqualTo(meetingBack);

        decision.meeting(null);
        assertThat(decision.getMeeting()).isNull();
    }

    @Test
    void rectifiesTest() {
        Decision decision = getDecisionRandomSampleGenerator();
        Decision decisionBack = getDecisionRandomSampleGenerator();

        decision.setRectifies(decisionBack);
        assertThat(decision.getRectifies()).isEqualTo(decisionBack);

        decision.rectifies(null);
        assertThat(decision.getRectifies()).isNull();
    }
}
