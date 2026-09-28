package cm.gimac.des.notification.domain;

import static cm.gimac.des.notification.domain.AcknowledgementPolicyTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AcknowledgementPolicyTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AcknowledgementPolicy.class);
        AcknowledgementPolicy acknowledgementPolicy1 = getAcknowledgementPolicySample1();
        AcknowledgementPolicy acknowledgementPolicy2 = new AcknowledgementPolicy();
        assertThat(acknowledgementPolicy1).isNotEqualTo(acknowledgementPolicy2);

        acknowledgementPolicy2.setId(acknowledgementPolicy1.getId());
        assertThat(acknowledgementPolicy1).isEqualTo(acknowledgementPolicy2);

        acknowledgementPolicy2 = getAcknowledgementPolicySample2();
        assertThat(acknowledgementPolicy1).isNotEqualTo(acknowledgementPolicy2);
    }
}
