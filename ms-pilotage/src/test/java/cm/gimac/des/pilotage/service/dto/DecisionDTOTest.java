package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DecisionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DecisionDTO.class);
        DecisionDTO decisionDTO1 = new DecisionDTO();
        decisionDTO1.setId(UUID.randomUUID());
        DecisionDTO decisionDTO2 = new DecisionDTO();
        assertThat(decisionDTO1).isNotEqualTo(decisionDTO2);
        decisionDTO2.setId(decisionDTO1.getId());
        assertThat(decisionDTO1).isEqualTo(decisionDTO2);
        decisionDTO2.setId(UUID.randomUUID());
        assertThat(decisionDTO1).isNotEqualTo(decisionDTO2);
        decisionDTO1.setId(null);
        assertThat(decisionDTO1).isNotEqualTo(decisionDTO2);
    }
}
