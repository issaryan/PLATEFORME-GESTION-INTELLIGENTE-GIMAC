package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.QuitusTestSamples.*;
import static cm.gimac.des.certification.domain.SignedDecisionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SignedDecisionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SignedDecision.class);
        SignedDecision signedDecision1 = getSignedDecisionSample1();
        SignedDecision signedDecision2 = new SignedDecision();
        assertThat(signedDecision1).isNotEqualTo(signedDecision2);

        signedDecision2.setId(signedDecision1.getId());
        assertThat(signedDecision1).isEqualTo(signedDecision2);

        signedDecision2 = getSignedDecisionSample2();
        assertThat(signedDecision1).isNotEqualTo(signedDecision2);
    }

    @Test
    void quitusTest() {
        SignedDecision signedDecision = getSignedDecisionRandomSampleGenerator();
        Quitus quitusBack = getQuitusRandomSampleGenerator();

        signedDecision.setQuitus(quitusBack);
        assertThat(signedDecision.getQuitus()).isEqualTo(quitusBack);

        signedDecision.quitus(null);
        assertThat(signedDecision.getQuitus()).isNull();
    }
}
