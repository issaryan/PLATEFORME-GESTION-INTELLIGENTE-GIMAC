package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.OnCallRosterTestSamples.*;
import static cm.gimac.des.monitoring.domain.OnCallRosterTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OnCallRosterTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OnCallRoster.class);
        OnCallRoster onCallRoster1 = getOnCallRosterSample1();
        OnCallRoster onCallRoster2 = new OnCallRoster();
        assertThat(onCallRoster1).isNotEqualTo(onCallRoster2);

        onCallRoster2.setId(onCallRoster1.getId());
        assertThat(onCallRoster1).isEqualTo(onCallRoster2);

        onCallRoster2 = getOnCallRosterSample2();
        assertThat(onCallRoster1).isNotEqualTo(onCallRoster2);
    }

    @Test
    void previousRevisionTest() {
        OnCallRoster onCallRoster = getOnCallRosterRandomSampleGenerator();
        OnCallRoster onCallRosterBack = getOnCallRosterRandomSampleGenerator();

        onCallRoster.setPreviousRevision(onCallRosterBack);
        assertThat(onCallRoster.getPreviousRevision()).isEqualTo(onCallRosterBack);

        onCallRoster.previousRevision(null);
        assertThat(onCallRoster.getPreviousRevision()).isNull();
    }
}
