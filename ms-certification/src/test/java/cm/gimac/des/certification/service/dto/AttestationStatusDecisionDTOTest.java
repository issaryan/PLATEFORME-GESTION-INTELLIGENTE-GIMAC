package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttestationStatusDecisionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AttestationStatusDecisionDTO.class);
        AttestationStatusDecisionDTO attestationStatusDecisionDTO1 = new AttestationStatusDecisionDTO();
        attestationStatusDecisionDTO1.setId(UUID.randomUUID());
        AttestationStatusDecisionDTO attestationStatusDecisionDTO2 = new AttestationStatusDecisionDTO();
        assertThat(attestationStatusDecisionDTO1).isNotEqualTo(attestationStatusDecisionDTO2);
        attestationStatusDecisionDTO2.setId(attestationStatusDecisionDTO1.getId());
        assertThat(attestationStatusDecisionDTO1).isEqualTo(attestationStatusDecisionDTO2);
        attestationStatusDecisionDTO2.setId(UUID.randomUUID());
        assertThat(attestationStatusDecisionDTO1).isNotEqualTo(attestationStatusDecisionDTO2);
        attestationStatusDecisionDTO1.setId(null);
        assertThat(attestationStatusDecisionDTO1).isNotEqualTo(attestationStatusDecisionDTO2);
    }
}
