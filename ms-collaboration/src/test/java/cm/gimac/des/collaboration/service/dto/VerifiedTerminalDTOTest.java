package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerifiedTerminalDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(VerifiedTerminalDTO.class);
        VerifiedTerminalDTO verifiedTerminalDTO1 = new VerifiedTerminalDTO();
        verifiedTerminalDTO1.setId(UUID.randomUUID());
        VerifiedTerminalDTO verifiedTerminalDTO2 = new VerifiedTerminalDTO();
        assertThat(verifiedTerminalDTO1).isNotEqualTo(verifiedTerminalDTO2);
        verifiedTerminalDTO2.setId(verifiedTerminalDTO1.getId());
        assertThat(verifiedTerminalDTO1).isEqualTo(verifiedTerminalDTO2);
        verifiedTerminalDTO2.setId(UUID.randomUUID());
        assertThat(verifiedTerminalDTO1).isNotEqualTo(verifiedTerminalDTO2);
        verifiedTerminalDTO1.setId(null);
        assertThat(verifiedTerminalDTO1).isNotEqualTo(verifiedTerminalDTO2);
    }
}
