package cm.gimac.des.notification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcknowledgementPolicyDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AcknowledgementPolicyDTO.class);
        AcknowledgementPolicyDTO acknowledgementPolicyDTO1 = new AcknowledgementPolicyDTO();
        acknowledgementPolicyDTO1.setId(UUID.randomUUID());
        AcknowledgementPolicyDTO acknowledgementPolicyDTO2 = new AcknowledgementPolicyDTO();
        assertThat(acknowledgementPolicyDTO1).isNotEqualTo(acknowledgementPolicyDTO2);
        acknowledgementPolicyDTO2.setId(acknowledgementPolicyDTO1.getId());
        assertThat(acknowledgementPolicyDTO1).isEqualTo(acknowledgementPolicyDTO2);
        acknowledgementPolicyDTO2.setId(UUID.randomUUID());
        assertThat(acknowledgementPolicyDTO1).isNotEqualTo(acknowledgementPolicyDTO2);
        acknowledgementPolicyDTO1.setId(null);
        assertThat(acknowledgementPolicyDTO1).isNotEqualTo(acknowledgementPolicyDTO2);
    }
}
