package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ContractualScopeTestSamples.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static cm.gimac.des.homologation.domain.ReferentialVersionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LiaisonTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Liaison.class);
        Liaison liaison1 = getLiaisonSample1();
        Liaison liaison2 = new Liaison();
        assertThat(liaison1).isNotEqualTo(liaison2);

        liaison2.setId(liaison1.getId());
        assertThat(liaison1).isEqualTo(liaison2);

        liaison2 = getLiaisonSample2();
        assertThat(liaison1).isNotEqualTo(liaison2);
    }

    @Test
    void versionTest() {
        Liaison liaison = getLiaisonRandomSampleGenerator();
        ReferentialVersion referentialVersionBack = getReferentialVersionRandomSampleGenerator();

        liaison.setVersion(referentialVersionBack);
        assertThat(liaison.getVersion()).isEqualTo(referentialVersionBack);

        liaison.version(null);
        assertThat(liaison.getVersion()).isNull();
    }

    @Test
    void contractualScopeTest() {
        Liaison liaison = getLiaisonRandomSampleGenerator();
        ContractualScope contractualScopeBack = getContractualScopeRandomSampleGenerator();

        liaison.setContractualScope(contractualScopeBack);
        assertThat(liaison.getContractualScope()).isEqualTo(contractualScopeBack);
        assertThat(contractualScopeBack.getLiaison()).isEqualTo(liaison);

        liaison.contractualScope(null);
        assertThat(liaison.getContractualScope()).isNull();
        assertThat(contractualScopeBack.getLiaison()).isNull();
    }
}
