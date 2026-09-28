package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PeriodicReportDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PeriodicReportDTO.class);
        PeriodicReportDTO periodicReportDTO1 = new PeriodicReportDTO();
        periodicReportDTO1.setId(UUID.randomUUID());
        PeriodicReportDTO periodicReportDTO2 = new PeriodicReportDTO();
        assertThat(periodicReportDTO1).isNotEqualTo(periodicReportDTO2);
        periodicReportDTO2.setId(periodicReportDTO1.getId());
        assertThat(periodicReportDTO1).isEqualTo(periodicReportDTO2);
        periodicReportDTO2.setId(UUID.randomUUID());
        assertThat(periodicReportDTO1).isNotEqualTo(periodicReportDTO2);
        periodicReportDTO1.setId(null);
        assertThat(periodicReportDTO1).isNotEqualTo(periodicReportDTO2);
    }
}
