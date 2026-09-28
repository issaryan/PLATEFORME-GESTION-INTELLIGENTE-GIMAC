package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataExportDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DataExportDTO.class);
        DataExportDTO dataExportDTO1 = new DataExportDTO();
        dataExportDTO1.setId(UUID.randomUUID());
        DataExportDTO dataExportDTO2 = new DataExportDTO();
        assertThat(dataExportDTO1).isNotEqualTo(dataExportDTO2);
        dataExportDTO2.setId(dataExportDTO1.getId());
        assertThat(dataExportDTO1).isEqualTo(dataExportDTO2);
        dataExportDTO2.setId(UUID.randomUUID());
        assertThat(dataExportDTO1).isNotEqualTo(dataExportDTO2);
        dataExportDTO1.setId(null);
        assertThat(dataExportDTO1).isNotEqualTo(dataExportDTO2);
    }
}
