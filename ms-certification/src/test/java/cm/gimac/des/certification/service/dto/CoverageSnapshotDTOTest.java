package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoverageSnapshotDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CoverageSnapshotDTO.class);
        CoverageSnapshotDTO coverageSnapshotDTO1 = new CoverageSnapshotDTO();
        coverageSnapshotDTO1.setId(UUID.randomUUID());
        CoverageSnapshotDTO coverageSnapshotDTO2 = new CoverageSnapshotDTO();
        assertThat(coverageSnapshotDTO1).isNotEqualTo(coverageSnapshotDTO2);
        coverageSnapshotDTO2.setId(coverageSnapshotDTO1.getId());
        assertThat(coverageSnapshotDTO1).isEqualTo(coverageSnapshotDTO2);
        coverageSnapshotDTO2.setId(UUID.randomUUID());
        assertThat(coverageSnapshotDTO1).isNotEqualTo(coverageSnapshotDTO2);
        coverageSnapshotDTO1.setId(null);
        assertThat(coverageSnapshotDTO1).isNotEqualTo(coverageSnapshotDTO2);
    }
}
