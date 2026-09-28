package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.AgingNoticeTestSamples.*;
import static cm.gimac.des.rapprochement.domain.AgingRuleTestSamples.*;
import static cm.gimac.des.rapprochement.domain.SuspensTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AgingNoticeTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AgingNotice.class);
        AgingNotice agingNotice1 = getAgingNoticeSample1();
        AgingNotice agingNotice2 = new AgingNotice();
        assertThat(agingNotice1).isNotEqualTo(agingNotice2);

        agingNotice2.setId(agingNotice1.getId());
        assertThat(agingNotice1).isEqualTo(agingNotice2);

        agingNotice2 = getAgingNoticeSample2();
        assertThat(agingNotice1).isNotEqualTo(agingNotice2);
    }

    @Test
    void suspensTest() {
        AgingNotice agingNotice = getAgingNoticeRandomSampleGenerator();
        Suspens suspensBack = getSuspensRandomSampleGenerator();

        agingNotice.setSuspens(suspensBack);
        assertThat(agingNotice.getSuspens()).isEqualTo(suspensBack);

        agingNotice.suspens(null);
        assertThat(agingNotice.getSuspens()).isNull();
    }

    @Test
    void ruleTest() {
        AgingNotice agingNotice = getAgingNoticeRandomSampleGenerator();
        AgingRule agingRuleBack = getAgingRuleRandomSampleGenerator();

        agingNotice.setRule(agingRuleBack);
        assertThat(agingNotice.getRule()).isEqualTo(agingRuleBack);

        agingNotice.rule(null);
        assertThat(agingNotice.getRule()).isNull();
    }
}
