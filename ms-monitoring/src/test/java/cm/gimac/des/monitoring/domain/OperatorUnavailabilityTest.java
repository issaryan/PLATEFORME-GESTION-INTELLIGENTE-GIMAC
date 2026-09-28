package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.OperatorUnavailabilityTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OperatorUnavailabilityTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OperatorUnavailability.class);
        OperatorUnavailability operatorUnavailability1 = getOperatorUnavailabilitySample1();
        OperatorUnavailability operatorUnavailability2 = new OperatorUnavailability();
        assertThat(operatorUnavailability1).isNotEqualTo(operatorUnavailability2);

        operatorUnavailability2.setId(operatorUnavailability1.getId());
        assertThat(operatorUnavailability1).isEqualTo(operatorUnavailability2);

        operatorUnavailability2 = getOperatorUnavailabilitySample2();
        assertThat(operatorUnavailability1).isNotEqualTo(operatorUnavailability2);
    }
}
