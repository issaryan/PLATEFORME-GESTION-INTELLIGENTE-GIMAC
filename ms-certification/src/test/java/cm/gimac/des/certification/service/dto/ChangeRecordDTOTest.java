package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangeRecordDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ChangeRecordDTO.class);
        ChangeRecordDTO changeRecordDTO1 = new ChangeRecordDTO();
        changeRecordDTO1.setId(UUID.randomUUID());
        ChangeRecordDTO changeRecordDTO2 = new ChangeRecordDTO();
        assertThat(changeRecordDTO1).isNotEqualTo(changeRecordDTO2);
        changeRecordDTO2.setId(changeRecordDTO1.getId());
        assertThat(changeRecordDTO1).isEqualTo(changeRecordDTO2);
        changeRecordDTO2.setId(UUID.randomUUID());
        assertThat(changeRecordDTO1).isNotEqualTo(changeRecordDTO2);
        changeRecordDTO1.setId(null);
        assertThat(changeRecordDTO1).isNotEqualTo(changeRecordDTO2);
    }
}
