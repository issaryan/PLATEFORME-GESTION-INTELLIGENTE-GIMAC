package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClosureRecordDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ClosureRecordDTO.class);
        ClosureRecordDTO closureRecordDTO1 = new ClosureRecordDTO();
        closureRecordDTO1.setId(UUID.randomUUID());
        ClosureRecordDTO closureRecordDTO2 = new ClosureRecordDTO();
        assertThat(closureRecordDTO1).isNotEqualTo(closureRecordDTO2);
        closureRecordDTO2.setId(closureRecordDTO1.getId());
        assertThat(closureRecordDTO1).isEqualTo(closureRecordDTO2);
        closureRecordDTO2.setId(UUID.randomUUID());
        assertThat(closureRecordDTO1).isNotEqualTo(closureRecordDTO2);
        closureRecordDTO1.setId(null);
        assertThat(closureRecordDTO1).isNotEqualTo(closureRecordDTO2);
    }
}
