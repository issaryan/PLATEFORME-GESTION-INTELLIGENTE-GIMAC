package cm.gimac.des.participant.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EscalationContactDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EscalationContactDTO.class);
        EscalationContactDTO escalationContactDTO1 = new EscalationContactDTO();
        escalationContactDTO1.setId(UUID.randomUUID());
        EscalationContactDTO escalationContactDTO2 = new EscalationContactDTO();
        assertThat(escalationContactDTO1).isNotEqualTo(escalationContactDTO2);
        escalationContactDTO2.setId(escalationContactDTO1.getId());
        assertThat(escalationContactDTO1).isEqualTo(escalationContactDTO2);
        escalationContactDTO2.setId(UUID.randomUUID());
        assertThat(escalationContactDTO1).isNotEqualTo(escalationContactDTO2);
        escalationContactDTO1.setId(null);
        assertThat(escalationContactDTO1).isNotEqualTo(escalationContactDTO2);
    }
}
