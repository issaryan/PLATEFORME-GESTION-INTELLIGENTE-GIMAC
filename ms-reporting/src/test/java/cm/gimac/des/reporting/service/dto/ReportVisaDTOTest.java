package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportVisaDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReportVisaDTO.class);
        ReportVisaDTO reportVisaDTO1 = new ReportVisaDTO();
        reportVisaDTO1.setId(UUID.randomUUID());
        ReportVisaDTO reportVisaDTO2 = new ReportVisaDTO();
        assertThat(reportVisaDTO1).isNotEqualTo(reportVisaDTO2);
        reportVisaDTO2.setId(reportVisaDTO1.getId());
        assertThat(reportVisaDTO1).isEqualTo(reportVisaDTO2);
        reportVisaDTO2.setId(UUID.randomUUID());
        assertThat(reportVisaDTO1).isNotEqualTo(reportVisaDTO2);
        reportVisaDTO1.setId(null);
        assertThat(reportVisaDTO1).isNotEqualTo(reportVisaDTO2);
    }
}
