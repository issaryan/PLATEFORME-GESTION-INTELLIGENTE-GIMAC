package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangeRecordSnapshotDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ChangeRecordSnapshotDTO.class);
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO1 = new ChangeRecordSnapshotDTO();
        changeRecordSnapshotDTO1.setId(UUID.randomUUID());
        ChangeRecordSnapshotDTO changeRecordSnapshotDTO2 = new ChangeRecordSnapshotDTO();
        assertThat(changeRecordSnapshotDTO1).isNotEqualTo(changeRecordSnapshotDTO2);
        changeRecordSnapshotDTO2.setId(changeRecordSnapshotDTO1.getId());
        assertThat(changeRecordSnapshotDTO1).isEqualTo(changeRecordSnapshotDTO2);
        changeRecordSnapshotDTO2.setId(UUID.randomUUID());
        assertThat(changeRecordSnapshotDTO1).isNotEqualTo(changeRecordSnapshotDTO2);
        changeRecordSnapshotDTO1.setId(null);
        assertThat(changeRecordSnapshotDTO1).isNotEqualTo(changeRecordSnapshotDTO2);
    }
}
