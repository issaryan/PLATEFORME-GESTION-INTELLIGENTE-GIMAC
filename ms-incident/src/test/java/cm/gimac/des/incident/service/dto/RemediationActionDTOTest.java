package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RemediationActionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RemediationActionDTO.class);
        RemediationActionDTO remediationActionDTO1 = new RemediationActionDTO();
        remediationActionDTO1.setId(UUID.randomUUID());
        RemediationActionDTO remediationActionDTO2 = new RemediationActionDTO();
        assertThat(remediationActionDTO1).isNotEqualTo(remediationActionDTO2);
        remediationActionDTO2.setId(remediationActionDTO1.getId());
        assertThat(remediationActionDTO1).isEqualTo(remediationActionDTO2);
        remediationActionDTO2.setId(UUID.randomUUID());
        assertThat(remediationActionDTO1).isNotEqualTo(remediationActionDTO2);
        remediationActionDTO1.setId(null);
        assertThat(remediationActionDTO1).isNotEqualTo(remediationActionDTO2);
    }
}
