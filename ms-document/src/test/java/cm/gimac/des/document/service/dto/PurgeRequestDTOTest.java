package cm.gimac.des.document.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurgeRequestDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PurgeRequestDTO.class);
        PurgeRequestDTO purgeRequestDTO1 = new PurgeRequestDTO();
        purgeRequestDTO1.setId(UUID.randomUUID());
        PurgeRequestDTO purgeRequestDTO2 = new PurgeRequestDTO();
        assertThat(purgeRequestDTO1).isNotEqualTo(purgeRequestDTO2);
        purgeRequestDTO2.setId(purgeRequestDTO1.getId());
        assertThat(purgeRequestDTO1).isEqualTo(purgeRequestDTO2);
        purgeRequestDTO2.setId(UUID.randomUUID());
        assertThat(purgeRequestDTO1).isNotEqualTo(purgeRequestDTO2);
        purgeRequestDTO1.setId(null);
        assertThat(purgeRequestDTO1).isNotEqualTo(purgeRequestDTO2);
    }
}
