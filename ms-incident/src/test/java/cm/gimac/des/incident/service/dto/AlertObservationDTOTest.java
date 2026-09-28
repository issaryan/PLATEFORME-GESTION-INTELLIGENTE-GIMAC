package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertObservationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AlertObservationDTO.class);
        AlertObservationDTO alertObservationDTO1 = new AlertObservationDTO();
        alertObservationDTO1.setId(UUID.randomUUID());
        AlertObservationDTO alertObservationDTO2 = new AlertObservationDTO();
        assertThat(alertObservationDTO1).isNotEqualTo(alertObservationDTO2);
        alertObservationDTO2.setId(alertObservationDTO1.getId());
        assertThat(alertObservationDTO1).isEqualTo(alertObservationDTO2);
        alertObservationDTO2.setId(UUID.randomUUID());
        assertThat(alertObservationDTO1).isNotEqualTo(alertObservationDTO2);
        alertObservationDTO1.setId(null);
        assertThat(alertObservationDTO1).isNotEqualTo(alertObservationDTO2);
    }
}
