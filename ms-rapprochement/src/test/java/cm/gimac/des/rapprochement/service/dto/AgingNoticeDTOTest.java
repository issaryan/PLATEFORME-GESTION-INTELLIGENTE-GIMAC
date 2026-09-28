package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgingNoticeDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AgingNoticeDTO.class);
        AgingNoticeDTO agingNoticeDTO1 = new AgingNoticeDTO();
        agingNoticeDTO1.setId(UUID.randomUUID());
        AgingNoticeDTO agingNoticeDTO2 = new AgingNoticeDTO();
        assertThat(agingNoticeDTO1).isNotEqualTo(agingNoticeDTO2);
        agingNoticeDTO2.setId(agingNoticeDTO1.getId());
        assertThat(agingNoticeDTO1).isEqualTo(agingNoticeDTO2);
        agingNoticeDTO2.setId(UUID.randomUUID());
        assertThat(agingNoticeDTO1).isNotEqualTo(agingNoticeDTO2);
        agingNoticeDTO1.setId(null);
        assertThat(agingNoticeDTO1).isNotEqualTo(agingNoticeDTO2);
    }
}
