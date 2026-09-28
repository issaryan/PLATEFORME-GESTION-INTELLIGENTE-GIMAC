package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.AlertTestSamples.*;
import static cm.gimac.des.monitoring.domain.AlertTestSamples.*;
import static cm.gimac.des.monitoring.domain.SupervisedLinkTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AlertTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Alert.class);
        Alert alert1 = getAlertSample1();
        Alert alert2 = new Alert();
        assertThat(alert1).isNotEqualTo(alert2);

        alert2.setId(alert1.getId());
        assertThat(alert1).isEqualTo(alert2);

        alert2 = getAlertSample2();
        assertThat(alert1).isNotEqualTo(alert2);
    }

    @Test
    void linkTest() {
        Alert alert = getAlertRandomSampleGenerator();
        SupervisedLink supervisedLinkBack = getSupervisedLinkRandomSampleGenerator();

        alert.setLink(supervisedLinkBack);
        assertThat(alert.getLink()).isEqualTo(supervisedLinkBack);

        alert.link(null);
        assertThat(alert.getLink()).isNull();
    }

    @Test
    void correlationRootTest() {
        Alert alert = getAlertRandomSampleGenerator();
        Alert alertBack = getAlertRandomSampleGenerator();

        alert.setCorrelationRoot(alertBack);
        assertThat(alert.getCorrelationRoot()).isEqualTo(alertBack);

        alert.correlationRoot(null);
        assertThat(alert.getCorrelationRoot()).isNull();
    }
}
