package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.AlertSeverityPolicyTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AlertSeverityPolicyTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AlertSeverityPolicy.class);
        AlertSeverityPolicy alertSeverityPolicy1 = getAlertSeverityPolicySample1();
        AlertSeverityPolicy alertSeverityPolicy2 = new AlertSeverityPolicy();
        assertThat(alertSeverityPolicy1).isNotEqualTo(alertSeverityPolicy2);

        alertSeverityPolicy2.setId(alertSeverityPolicy1.getId());
        assertThat(alertSeverityPolicy1).isEqualTo(alertSeverityPolicy2);

        alertSeverityPolicy2 = getAlertSeverityPolicySample2();
        assertThat(alertSeverityPolicy1).isNotEqualTo(alertSeverityPolicy2);
    }
}
