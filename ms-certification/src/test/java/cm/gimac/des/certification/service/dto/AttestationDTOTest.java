package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttestationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AttestationDTO.class);
        AttestationDTO attestationDTO1 = new AttestationDTO();
        attestationDTO1.setId(UUID.randomUUID());
        AttestationDTO attestationDTO2 = new AttestationDTO();
        assertThat(attestationDTO1).isNotEqualTo(attestationDTO2);
        attestationDTO2.setId(attestationDTO1.getId());
        assertThat(attestationDTO1).isEqualTo(attestationDTO2);
        attestationDTO2.setId(UUID.randomUUID());
        assertThat(attestationDTO1).isNotEqualTo(attestationDTO2);
        attestationDTO1.setId(null);
        assertThat(attestationDTO1).isNotEqualTo(attestationDTO2);
    }
}
