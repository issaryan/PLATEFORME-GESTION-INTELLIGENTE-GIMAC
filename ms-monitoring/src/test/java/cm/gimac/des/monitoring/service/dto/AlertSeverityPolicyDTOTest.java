package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertSeverityPolicyDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AlertSeverityPolicyDTO.class);
        AlertSeverityPolicyDTO alertSeverityPolicyDTO1 = new AlertSeverityPolicyDTO();
        alertSeverityPolicyDTO1.setId(UUID.randomUUID());
        AlertSeverityPolicyDTO alertSeverityPolicyDTO2 = new AlertSeverityPolicyDTO();
        assertThat(alertSeverityPolicyDTO1).isNotEqualTo(alertSeverityPolicyDTO2);
        alertSeverityPolicyDTO2.setId(alertSeverityPolicyDTO1.getId());
        assertThat(alertSeverityPolicyDTO1).isEqualTo(alertSeverityPolicyDTO2);
        alertSeverityPolicyDTO2.setId(UUID.randomUUID());
        assertThat(alertSeverityPolicyDTO1).isNotEqualTo(alertSeverityPolicyDTO2);
        alertSeverityPolicyDTO1.setId(null);
        assertThat(alertSeverityPolicyDTO1).isNotEqualTo(alertSeverityPolicyDTO2);
    }
}
