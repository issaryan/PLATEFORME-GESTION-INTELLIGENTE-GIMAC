package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceFileExportDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EvidenceFileExportDTO.class);
        EvidenceFileExportDTO evidenceFileExportDTO1 = new EvidenceFileExportDTO();
        evidenceFileExportDTO1.setId(UUID.randomUUID());
        EvidenceFileExportDTO evidenceFileExportDTO2 = new EvidenceFileExportDTO();
        assertThat(evidenceFileExportDTO1).isNotEqualTo(evidenceFileExportDTO2);
        evidenceFileExportDTO2.setId(evidenceFileExportDTO1.getId());
        assertThat(evidenceFileExportDTO1).isEqualTo(evidenceFileExportDTO2);
        evidenceFileExportDTO2.setId(UUID.randomUUID());
        assertThat(evidenceFileExportDTO1).isNotEqualTo(evidenceFileExportDTO2);
        evidenceFileExportDTO1.setId(null);
        assertThat(evidenceFileExportDTO1).isNotEqualTo(evidenceFileExportDTO2);
    }
}
