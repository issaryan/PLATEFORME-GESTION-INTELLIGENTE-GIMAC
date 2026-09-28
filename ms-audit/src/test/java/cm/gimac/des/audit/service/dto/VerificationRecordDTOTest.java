package cm.gimac.des.audit.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerificationRecordDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(VerificationRecordDTO.class);
        VerificationRecordDTO verificationRecordDTO1 = new VerificationRecordDTO();
        verificationRecordDTO1.setId(UUID.randomUUID());
        VerificationRecordDTO verificationRecordDTO2 = new VerificationRecordDTO();
        assertThat(verificationRecordDTO1).isNotEqualTo(verificationRecordDTO2);
        verificationRecordDTO2.setId(verificationRecordDTO1.getId());
        assertThat(verificationRecordDTO1).isEqualTo(verificationRecordDTO2);
        verificationRecordDTO2.setId(UUID.randomUUID());
        assertThat(verificationRecordDTO1).isNotEqualTo(verificationRecordDTO2);
        verificationRecordDTO1.setId(null);
        assertThat(verificationRecordDTO1).isNotEqualTo(verificationRecordDTO2);
    }
}
