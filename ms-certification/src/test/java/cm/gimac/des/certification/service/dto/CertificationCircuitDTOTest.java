package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CertificationCircuitDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CertificationCircuitDTO.class);
        CertificationCircuitDTO certificationCircuitDTO1 = new CertificationCircuitDTO();
        certificationCircuitDTO1.setId(UUID.randomUUID());
        CertificationCircuitDTO certificationCircuitDTO2 = new CertificationCircuitDTO();
        assertThat(certificationCircuitDTO1).isNotEqualTo(certificationCircuitDTO2);
        certificationCircuitDTO2.setId(certificationCircuitDTO1.getId());
        assertThat(certificationCircuitDTO1).isEqualTo(certificationCircuitDTO2);
        certificationCircuitDTO2.setId(UUID.randomUUID());
        assertThat(certificationCircuitDTO1).isNotEqualTo(certificationCircuitDTO2);
        certificationCircuitDTO1.setId(null);
        assertThat(certificationCircuitDTO1).isNotEqualTo(certificationCircuitDTO2);
    }
}
