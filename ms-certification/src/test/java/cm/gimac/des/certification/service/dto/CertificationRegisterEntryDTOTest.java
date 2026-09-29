package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CertificationRegisterEntryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CertificationRegisterEntryDTO.class);
        CertificationRegisterEntryDTO certificationRegisterEntryDTO1 = new CertificationRegisterEntryDTO();
        certificationRegisterEntryDTO1.setId(UUID.randomUUID());
        CertificationRegisterEntryDTO certificationRegisterEntryDTO2 = new CertificationRegisterEntryDTO();
        assertThat(certificationRegisterEntryDTO1).isNotEqualTo(certificationRegisterEntryDTO2);
        certificationRegisterEntryDTO2.setId(certificationRegisterEntryDTO1.getId());
        assertThat(certificationRegisterEntryDTO1).isEqualTo(certificationRegisterEntryDTO2);
        certificationRegisterEntryDTO2.setId(UUID.randomUUID());
        assertThat(certificationRegisterEntryDTO1).isNotEqualTo(certificationRegisterEntryDTO2);
        certificationRegisterEntryDTO1.setId(null);
        assertThat(certificationRegisterEntryDTO1).isNotEqualTo(certificationRegisterEntryDTO2);
    }
}
