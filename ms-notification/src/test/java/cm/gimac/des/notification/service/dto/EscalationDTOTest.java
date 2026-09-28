package cm.gimac.des.notification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EscalationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EscalationDTO.class);
        EscalationDTO escalationDTO1 = new EscalationDTO();
        escalationDTO1.setId(UUID.randomUUID());
        EscalationDTO escalationDTO2 = new EscalationDTO();
        assertThat(escalationDTO1).isNotEqualTo(escalationDTO2);
        escalationDTO2.setId(escalationDTO1.getId());
        assertThat(escalationDTO1).isEqualTo(escalationDTO2);
        escalationDTO2.setId(UUID.randomUUID());
        assertThat(escalationDTO1).isNotEqualTo(escalationDTO2);
        escalationDTO1.setId(null);
        assertThat(escalationDTO1).isNotEqualTo(escalationDTO2);
    }
}
