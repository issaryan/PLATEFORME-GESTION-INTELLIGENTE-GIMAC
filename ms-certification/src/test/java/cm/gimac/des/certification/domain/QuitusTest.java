package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.CertificationCircuitTestSamples.*;
import static cm.gimac.des.certification.domain.QuitusTestSamples.*;
import static cm.gimac.des.certification.domain.SignedDecisionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class QuitusTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Quitus.class);
        Quitus quitus1 = getQuitusSample1();
        Quitus quitus2 = new Quitus();
        assertThat(quitus1).isNotEqualTo(quitus2);

        quitus2.setId(quitus1.getId());
        assertThat(quitus1).isEqualTo(quitus2);

        quitus2 = getQuitusSample2();
        assertThat(quitus1).isNotEqualTo(quitus2);
    }

    @Test
    void circuitTest() {
        Quitus quitus = getQuitusRandomSampleGenerator();
        CertificationCircuit certificationCircuitBack = getCertificationCircuitRandomSampleGenerator();

        quitus.setCircuit(certificationCircuitBack);
        assertThat(quitus.getCircuit()).isEqualTo(certificationCircuitBack);

        quitus.circuit(null);
        assertThat(quitus.getCircuit()).isNull();
    }

    @Test
    void signedDecisionTest() {
        Quitus quitus = getQuitusRandomSampleGenerator();
        SignedDecision signedDecisionBack = getSignedDecisionRandomSampleGenerator();

        quitus.setSignedDecision(signedDecisionBack);
        assertThat(quitus.getSignedDecision()).isEqualTo(signedDecisionBack);
        assertThat(signedDecisionBack.getQuitus()).isEqualTo(quitus);

        quitus.signedDecision(null);
        assertThat(quitus.getSignedDecision()).isNull();
        assertThat(signedDecisionBack.getQuitus()).isNull();
    }
}
