package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.AlertObservationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AlertObservationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AlertObservation.class);
        AlertObservation alertObservation1 = getAlertObservationSample1();
        AlertObservation alertObservation2 = new AlertObservation();
        assertThat(alertObservation1).isNotEqualTo(alertObservation2);

        alertObservation2.setId(alertObservation1.getId());
        assertThat(alertObservation1).isEqualTo(alertObservation2);

        alertObservation2 = getAlertObservationSample2();
        assertThat(alertObservation1).isNotEqualTo(alertObservation2);
    }
}
