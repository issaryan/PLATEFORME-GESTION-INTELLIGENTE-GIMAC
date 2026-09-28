package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.AlertSourceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AlertSourceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AlertSource.class);
        AlertSource alertSource1 = getAlertSourceSample1();
        AlertSource alertSource2 = new AlertSource();
        assertThat(alertSource1).isNotEqualTo(alertSource2);

        alertSource2.setId(alertSource1.getId());
        assertThat(alertSource1).isEqualTo(alertSource2);

        alertSource2 = getAlertSourceSample2();
        assertThat(alertSource1).isNotEqualTo(alertSource2);
    }
}
