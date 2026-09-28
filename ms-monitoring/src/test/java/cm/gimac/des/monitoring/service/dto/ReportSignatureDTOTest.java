package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportSignatureDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReportSignatureDTO.class);
        ReportSignatureDTO reportSignatureDTO1 = new ReportSignatureDTO();
        reportSignatureDTO1.setId(UUID.randomUUID());
        ReportSignatureDTO reportSignatureDTO2 = new ReportSignatureDTO();
        assertThat(reportSignatureDTO1).isNotEqualTo(reportSignatureDTO2);
        reportSignatureDTO2.setId(reportSignatureDTO1.getId());
        assertThat(reportSignatureDTO1).isEqualTo(reportSignatureDTO2);
        reportSignatureDTO2.setId(UUID.randomUUID());
        assertThat(reportSignatureDTO1).isNotEqualTo(reportSignatureDTO2);
        reportSignatureDTO1.setId(null);
        assertThat(reportSignatureDTO1).isNotEqualTo(reportSignatureDTO2);
    }
}
