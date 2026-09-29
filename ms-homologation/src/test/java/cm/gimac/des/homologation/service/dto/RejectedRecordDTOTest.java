package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RejectedRecordDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RejectedRecordDTO.class);
        RejectedRecordDTO rejectedRecordDTO1 = new RejectedRecordDTO();
        rejectedRecordDTO1.setId(UUID.randomUUID());
        RejectedRecordDTO rejectedRecordDTO2 = new RejectedRecordDTO();
        assertThat(rejectedRecordDTO1).isNotEqualTo(rejectedRecordDTO2);
        rejectedRecordDTO2.setId(rejectedRecordDTO1.getId());
        assertThat(rejectedRecordDTO1).isEqualTo(rejectedRecordDTO2);
        rejectedRecordDTO2.setId(UUID.randomUUID());
        assertThat(rejectedRecordDTO1).isNotEqualTo(rejectedRecordDTO2);
        rejectedRecordDTO1.setId(null);
        assertThat(rejectedRecordDTO1).isNotEqualTo(rejectedRecordDTO2);
    }
}
