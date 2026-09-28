package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.DepositContractTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DepositContractTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DepositContract.class);
        DepositContract depositContract1 = getDepositContractSample1();
        DepositContract depositContract2 = new DepositContract();
        assertThat(depositContract1).isNotEqualTo(depositContract2);

        depositContract2.setId(depositContract1.getId());
        assertThat(depositContract1).isEqualTo(depositContract2);

        depositContract2 = getDepositContractSample2();
        assertThat(depositContract1).isNotEqualTo(depositContract2);
    }
}
