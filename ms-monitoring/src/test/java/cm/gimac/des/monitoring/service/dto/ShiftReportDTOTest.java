package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftReportDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ShiftReportDTO.class);
        ShiftReportDTO shiftReportDTO1 = new ShiftReportDTO();
        shiftReportDTO1.setId(UUID.randomUUID());
        ShiftReportDTO shiftReportDTO2 = new ShiftReportDTO();
        assertThat(shiftReportDTO1).isNotEqualTo(shiftReportDTO2);
        shiftReportDTO2.setId(shiftReportDTO1.getId());
        assertThat(shiftReportDTO1).isEqualTo(shiftReportDTO2);
        shiftReportDTO2.setId(UUID.randomUUID());
        assertThat(shiftReportDTO1).isNotEqualTo(shiftReportDTO2);
        shiftReportDTO1.setId(null);
        assertThat(shiftReportDTO1).isNotEqualTo(shiftReportDTO2);
    }
}
