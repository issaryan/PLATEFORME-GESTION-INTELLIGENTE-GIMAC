package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrisisDecisionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CrisisDecisionDTO.class);
        CrisisDecisionDTO crisisDecisionDTO1 = new CrisisDecisionDTO();
        crisisDecisionDTO1.setId(UUID.randomUUID());
        CrisisDecisionDTO crisisDecisionDTO2 = new CrisisDecisionDTO();
        assertThat(crisisDecisionDTO1).isNotEqualTo(crisisDecisionDTO2);
        crisisDecisionDTO2.setId(crisisDecisionDTO1.getId());
        assertThat(crisisDecisionDTO1).isEqualTo(crisisDecisionDTO2);
        crisisDecisionDTO2.setId(UUID.randomUUID());
        assertThat(crisisDecisionDTO1).isNotEqualTo(crisisDecisionDTO2);
        crisisDecisionDTO1.setId(null);
        assertThat(crisisDecisionDTO1).isNotEqualTo(crisisDecisionDTO2);
    }
}
