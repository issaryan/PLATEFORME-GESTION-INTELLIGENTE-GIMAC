package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ContractualScopeTestSamples.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ContractualScopeTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ContractualScope.class);
        ContractualScope contractualScope1 = getContractualScopeSample1();
        ContractualScope contractualScope2 = new ContractualScope();
        assertThat(contractualScope1).isNotEqualTo(contractualScope2);

        contractualScope2.setId(contractualScope1.getId());
        assertThat(contractualScope1).isEqualTo(contractualScope2);

        contractualScope2 = getContractualScopeSample2();
        assertThat(contractualScope1).isNotEqualTo(contractualScope2);
    }

    @Test
    void liaisonTest() {
        ContractualScope contractualScope = getContractualScopeRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        contractualScope.setLiaison(liaisonBack);
        assertThat(contractualScope.getLiaison()).isEqualTo(liaisonBack);

        contractualScope.liaison(null);
        assertThat(contractualScope.getLiaison()).isNull();
    }
}
