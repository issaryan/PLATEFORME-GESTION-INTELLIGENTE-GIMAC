package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimestampTokenDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(TimestampTokenDTO.class);
        TimestampTokenDTO timestampTokenDTO1 = new TimestampTokenDTO();
        timestampTokenDTO1.setId(UUID.randomUUID());
        TimestampTokenDTO timestampTokenDTO2 = new TimestampTokenDTO();
        assertThat(timestampTokenDTO1).isNotEqualTo(timestampTokenDTO2);
        timestampTokenDTO2.setId(timestampTokenDTO1.getId());
        assertThat(timestampTokenDTO1).isEqualTo(timestampTokenDTO2);
        timestampTokenDTO2.setId(UUID.randomUUID());
        assertThat(timestampTokenDTO1).isNotEqualTo(timestampTokenDTO2);
        timestampTokenDTO1.setId(null);
        assertThat(timestampTokenDTO1).isNotEqualTo(timestampTokenDTO2);
    }
}
