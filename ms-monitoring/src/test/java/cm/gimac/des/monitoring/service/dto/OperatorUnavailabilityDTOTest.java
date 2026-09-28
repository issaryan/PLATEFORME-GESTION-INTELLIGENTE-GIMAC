package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperatorUnavailabilityDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OperatorUnavailabilityDTO.class);
        OperatorUnavailabilityDTO operatorUnavailabilityDTO1 = new OperatorUnavailabilityDTO();
        operatorUnavailabilityDTO1.setId(UUID.randomUUID());
        OperatorUnavailabilityDTO operatorUnavailabilityDTO2 = new OperatorUnavailabilityDTO();
        assertThat(operatorUnavailabilityDTO1).isNotEqualTo(operatorUnavailabilityDTO2);
        operatorUnavailabilityDTO2.setId(operatorUnavailabilityDTO1.getId());
        assertThat(operatorUnavailabilityDTO1).isEqualTo(operatorUnavailabilityDTO2);
        operatorUnavailabilityDTO2.setId(UUID.randomUUID());
        assertThat(operatorUnavailabilityDTO1).isNotEqualTo(operatorUnavailabilityDTO2);
        operatorUnavailabilityDTO1.setId(null);
        assertThat(operatorUnavailabilityDTO1).isNotEqualTo(operatorUnavailabilityDTO2);
    }
}
