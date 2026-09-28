package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertSourceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AlertSourceDTO.class);
        AlertSourceDTO alertSourceDTO1 = new AlertSourceDTO();
        alertSourceDTO1.setId(UUID.randomUUID());
        AlertSourceDTO alertSourceDTO2 = new AlertSourceDTO();
        assertThat(alertSourceDTO1).isNotEqualTo(alertSourceDTO2);
        alertSourceDTO2.setId(alertSourceDTO1.getId());
        assertThat(alertSourceDTO1).isEqualTo(alertSourceDTO2);
        alertSourceDTO2.setId(UUID.randomUUID());
        assertThat(alertSourceDTO1).isNotEqualTo(alertSourceDTO2);
        alertSourceDTO1.setId(null);
        assertThat(alertSourceDTO1).isNotEqualTo(alertSourceDTO2);
    }
}
