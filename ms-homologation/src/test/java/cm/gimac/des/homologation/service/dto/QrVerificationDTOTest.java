package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QrVerificationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(QrVerificationDTO.class);
        QrVerificationDTO qrVerificationDTO1 = new QrVerificationDTO();
        qrVerificationDTO1.setId(UUID.randomUUID());
        QrVerificationDTO qrVerificationDTO2 = new QrVerificationDTO();
        assertThat(qrVerificationDTO1).isNotEqualTo(qrVerificationDTO2);
        qrVerificationDTO2.setId(qrVerificationDTO1.getId());
        assertThat(qrVerificationDTO1).isEqualTo(qrVerificationDTO2);
        qrVerificationDTO2.setId(UUID.randomUUID());
        assertThat(qrVerificationDTO1).isNotEqualTo(qrVerificationDTO2);
        qrVerificationDTO1.setId(null);
        assertThat(qrVerificationDTO1).isNotEqualTo(qrVerificationDTO2);
    }
}
