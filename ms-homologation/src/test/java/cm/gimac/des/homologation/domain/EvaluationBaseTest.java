package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.EvaluationBaseTestSamples.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EvaluationBaseTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(EvaluationBase.class);
        EvaluationBase evaluationBase1 = getEvaluationBaseSample1();
        EvaluationBase evaluationBase2 = new EvaluationBase();
        assertThat(evaluationBase1).isNotEqualTo(evaluationBase2);

        evaluationBase2.setId(evaluationBase1.getId());
        assertThat(evaluationBase1).isEqualTo(evaluationBase2);

        evaluationBase2 = getEvaluationBaseSample2();
        assertThat(evaluationBase1).isNotEqualTo(evaluationBase2);
    }

    @Test
    void liaisonTest() {
        EvaluationBase evaluationBase = getEvaluationBaseRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        evaluationBase.setLiaison(liaisonBack);
        assertThat(evaluationBase.getLiaison()).isEqualTo(liaisonBack);

        evaluationBase.liaison(null);
        assertThat(evaluationBase.getLiaison()).isNull();
    }
}
