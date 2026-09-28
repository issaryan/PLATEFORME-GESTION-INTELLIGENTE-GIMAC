package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CertificationAttestationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CertificationAttestationDTO.class);
        CertificationAttestationDTO certificationAttestationDTO1 = new CertificationAttestationDTO();
        certificationAttestationDTO1.setId(UUID.randomUUID());
        CertificationAttestationDTO certificationAttestationDTO2 = new CertificationAttestationDTO();
        assertThat(certificationAttestationDTO1).isNotEqualTo(certificationAttestationDTO2);
        certificationAttestationDTO2.setId(certificationAttestationDTO1.getId());
        assertThat(certificationAttestationDTO1).isEqualTo(certificationAttestationDTO2);
        certificationAttestationDTO2.setId(UUID.randomUUID());
        assertThat(certificationAttestationDTO1).isNotEqualTo(certificationAttestationDTO2);
        certificationAttestationDTO1.setId(null);
        assertThat(certificationAttestationDTO1).isNotEqualTo(certificationAttestationDTO2);
    }
}
