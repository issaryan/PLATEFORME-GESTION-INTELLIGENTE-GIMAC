package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.CrisisDecisionTestSamples.*;
import static cm.gimac.des.incident.domain.IncidentTestSamples.*;
import static cm.gimac.des.incident.domain.RemediationActionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RemediationActionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RemediationAction.class);
        RemediationAction remediationAction1 = getRemediationActionSample1();
        RemediationAction remediationAction2 = new RemediationAction();
        assertThat(remediationAction1).isNotEqualTo(remediationAction2);

        remediationAction2.setId(remediationAction1.getId());
        assertThat(remediationAction1).isEqualTo(remediationAction2);

        remediationAction2 = getRemediationActionSample2();
        assertThat(remediationAction1).isNotEqualTo(remediationAction2);
    }

    @Test
    void incidentTest() {
        RemediationAction remediationAction = getRemediationActionRandomSampleGenerator();
        Incident incidentBack = getIncidentRandomSampleGenerator();

        remediationAction.setIncident(incidentBack);
        assertThat(remediationAction.getIncident()).isEqualTo(incidentBack);

        remediationAction.incident(null);
        assertThat(remediationAction.getIncident()).isNull();
    }

    @Test
    void crisisDecisionTest() {
        RemediationAction remediationAction = getRemediationActionRandomSampleGenerator();
        CrisisDecision crisisDecisionBack = getCrisisDecisionRandomSampleGenerator();

        remediationAction.setCrisisDecision(crisisDecisionBack);
        assertThat(remediationAction.getCrisisDecision()).isEqualTo(crisisDecisionBack);

        remediationAction.crisisDecision(null);
        assertThat(remediationAction.getCrisisDecision()).isNull();
    }
}
