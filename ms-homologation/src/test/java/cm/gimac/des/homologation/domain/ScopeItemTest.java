package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ContractualScopeTestSamples.*;
import static cm.gimac.des.homologation.domain.ScopeItemTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ScopeItemTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ScopeItem.class);
        ScopeItem scopeItem1 = getScopeItemSample1();
        ScopeItem scopeItem2 = new ScopeItem();
        assertThat(scopeItem1).isNotEqualTo(scopeItem2);

        scopeItem2.setId(scopeItem1.getId());
        assertThat(scopeItem1).isEqualTo(scopeItem2);

        scopeItem2 = getScopeItemSample2();
        assertThat(scopeItem1).isNotEqualTo(scopeItem2);
    }

    @Test
    void scopeTest() {
        ScopeItem scopeItem = getScopeItemRandomSampleGenerator();
        ContractualScope contractualScopeBack = getContractualScopeRandomSampleGenerator();

        scopeItem.setScope(contractualScopeBack);
        assertThat(scopeItem.getScope()).isEqualTo(contractualScopeBack);

        scopeItem.scope(null);
        assertThat(scopeItem.getScope()).isNull();
    }
}
