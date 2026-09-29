package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GoLiveReportDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(GoLiveReportDTO.class);
        GoLiveReportDTO goLiveReportDTO1 = new GoLiveReportDTO();
        goLiveReportDTO1.setId(UUID.randomUUID());
        GoLiveReportDTO goLiveReportDTO2 = new GoLiveReportDTO();
        assertThat(goLiveReportDTO1).isNotEqualTo(goLiveReportDTO2);
        goLiveReportDTO2.setId(goLiveReportDTO1.getId());
        assertThat(goLiveReportDTO1).isEqualTo(goLiveReportDTO2);
        goLiveReportDTO2.setId(UUID.randomUUID());
        assertThat(goLiveReportDTO1).isNotEqualTo(goLiveReportDTO2);
        goLiveReportDTO1.setId(null);
        assertThat(goLiveReportDTO1).isNotEqualTo(goLiveReportDTO2);
    }
}
