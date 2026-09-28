package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgingRuleDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AgingRuleDTO.class);
        AgingRuleDTO agingRuleDTO1 = new AgingRuleDTO();
        agingRuleDTO1.setId(UUID.randomUUID());
        AgingRuleDTO agingRuleDTO2 = new AgingRuleDTO();
        assertThat(agingRuleDTO1).isNotEqualTo(agingRuleDTO2);
        agingRuleDTO2.setId(agingRuleDTO1.getId());
        assertThat(agingRuleDTO1).isEqualTo(agingRuleDTO2);
        agingRuleDTO2.setId(UUID.randomUUID());
        assertThat(agingRuleDTO1).isNotEqualTo(agingRuleDTO2);
        agingRuleDTO1.setId(null);
        assertThat(agingRuleDTO1).isNotEqualTo(agingRuleDTO2);
    }
}
