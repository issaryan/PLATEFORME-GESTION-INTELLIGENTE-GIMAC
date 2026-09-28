package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.OnCallAssignmentTestSamples.*;
import static cm.gimac.des.monitoring.domain.OnCallRosterTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OnCallAssignmentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OnCallAssignment.class);
        OnCallAssignment onCallAssignment1 = getOnCallAssignmentSample1();
        OnCallAssignment onCallAssignment2 = new OnCallAssignment();
        assertThat(onCallAssignment1).isNotEqualTo(onCallAssignment2);

        onCallAssignment2.setId(onCallAssignment1.getId());
        assertThat(onCallAssignment1).isEqualTo(onCallAssignment2);

        onCallAssignment2 = getOnCallAssignmentSample2();
        assertThat(onCallAssignment1).isNotEqualTo(onCallAssignment2);
    }

    @Test
    void rosterTest() {
        OnCallAssignment onCallAssignment = getOnCallAssignmentRandomSampleGenerator();
        OnCallRoster onCallRosterBack = getOnCallRosterRandomSampleGenerator();

        onCallAssignment.setRoster(onCallRosterBack);
        assertThat(onCallAssignment.getRoster()).isEqualTo(onCallRosterBack);

        onCallAssignment.roster(null);
        assertThat(onCallAssignment.getRoster()).isNull();
    }
}
