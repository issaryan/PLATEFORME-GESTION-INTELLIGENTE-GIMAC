package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AlertDTO.class);
        AlertDTO alertDTO1 = new AlertDTO();
        alertDTO1.setId(UUID.randomUUID());
        AlertDTO alertDTO2 = new AlertDTO();
        assertThat(alertDTO1).isNotEqualTo(alertDTO2);
        alertDTO2.setId(alertDTO1.getId());
        assertThat(alertDTO1).isEqualTo(alertDTO2);
        alertDTO2.setId(UUID.randomUUID());
        assertThat(alertDTO1).isNotEqualTo(alertDTO2);
        alertDTO1.setId(null);
        assertThat(alertDTO1).isNotEqualTo(alertDTO2);
    }
}
