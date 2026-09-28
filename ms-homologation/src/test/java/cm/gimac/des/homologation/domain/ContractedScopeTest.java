package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ContractedScopeTestSamples.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ContractedScopeTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ContractedScope.class);
        ContractedScope contractedScope1 = getContractedScopeSample1();
        ContractedScope contractedScope2 = new ContractedScope();
        assertThat(contractedScope1).isNotEqualTo(contractedScope2);

        contractedScope2.setId(contractedScope1.getId());
        assertThat(contractedScope1).isEqualTo(contractedScope2);

        contractedScope2 = getContractedScopeSample2();
        assertThat(contractedScope1).isNotEqualTo(contractedScope2);
    }

    @Test
    void liaisonTest() {
        ContractedScope contractedScope = getContractedScopeRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        contractedScope.setLiaison(liaisonBack);
        assertThat(contractedScope.getLiaison()).isEqualTo(liaisonBack);

        contractedScope.liaison(null);
        assertThat(contractedScope.getLiaison()).isNull();
    }
}
