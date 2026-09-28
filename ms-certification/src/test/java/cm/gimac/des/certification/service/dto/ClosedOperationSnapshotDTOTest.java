package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClosedOperationSnapshotDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ClosedOperationSnapshotDTO.class);
        ClosedOperationSnapshotDTO closedOperationSnapshotDTO1 = new ClosedOperationSnapshotDTO();
        closedOperationSnapshotDTO1.setId(UUID.randomUUID());
        ClosedOperationSnapshotDTO closedOperationSnapshotDTO2 = new ClosedOperationSnapshotDTO();
        assertThat(closedOperationSnapshotDTO1).isNotEqualTo(closedOperationSnapshotDTO2);
        closedOperationSnapshotDTO2.setId(closedOperationSnapshotDTO1.getId());
        assertThat(closedOperationSnapshotDTO1).isEqualTo(closedOperationSnapshotDTO2);
        closedOperationSnapshotDTO2.setId(UUID.randomUUID());
        assertThat(closedOperationSnapshotDTO1).isNotEqualTo(closedOperationSnapshotDTO2);
        closedOperationSnapshotDTO1.setId(null);
        assertThat(closedOperationSnapshotDTO1).isNotEqualTo(closedOperationSnapshotDTO2);
    }
}
