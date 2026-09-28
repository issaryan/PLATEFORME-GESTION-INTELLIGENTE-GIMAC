package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommissioningFileDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CommissioningFileDTO.class);
        CommissioningFileDTO commissioningFileDTO1 = new CommissioningFileDTO();
        commissioningFileDTO1.setId(UUID.randomUUID());
        CommissioningFileDTO commissioningFileDTO2 = new CommissioningFileDTO();
        assertThat(commissioningFileDTO1).isNotEqualTo(commissioningFileDTO2);
        commissioningFileDTO2.setId(commissioningFileDTO1.getId());
        assertThat(commissioningFileDTO1).isEqualTo(commissioningFileDTO2);
        commissioningFileDTO2.setId(UUID.randomUUID());
        assertThat(commissioningFileDTO1).isNotEqualTo(commissioningFileDTO2);
        commissioningFileDTO1.setId(null);
        assertThat(commissioningFileDTO1).isNotEqualTo(commissioningFileDTO2);
    }
}
