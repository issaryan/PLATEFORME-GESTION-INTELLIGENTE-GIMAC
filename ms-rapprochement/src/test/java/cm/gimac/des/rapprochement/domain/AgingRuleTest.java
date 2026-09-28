package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.AgingRuleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AgingRuleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AgingRule.class);
        AgingRule agingRule1 = getAgingRuleSample1();
        AgingRule agingRule2 = new AgingRule();
        assertThat(agingRule1).isNotEqualTo(agingRule2);

        agingRule2.setId(agingRule1.getId());
        assertThat(agingRule1).isEqualTo(agingRule2);

        agingRule2 = getAgingRuleSample2();
        assertThat(agingRule1).isNotEqualTo(agingRule2);
    }
}
