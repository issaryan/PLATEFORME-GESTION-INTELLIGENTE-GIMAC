package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReplayCursorDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReplayCursorDTO.class);
        ReplayCursorDTO replayCursorDTO1 = new ReplayCursorDTO();
        replayCursorDTO1.setId(UUID.randomUUID());
        ReplayCursorDTO replayCursorDTO2 = new ReplayCursorDTO();
        assertThat(replayCursorDTO1).isNotEqualTo(replayCursorDTO2);
        replayCursorDTO2.setId(replayCursorDTO1.getId());
        assertThat(replayCursorDTO1).isEqualTo(replayCursorDTO2);
        replayCursorDTO2.setId(UUID.randomUUID());
        assertThat(replayCursorDTO1).isNotEqualTo(replayCursorDTO2);
        replayCursorDTO1.setId(null);
        assertThat(replayCursorDTO1).isNotEqualTo(replayCursorDTO2);
    }
}
