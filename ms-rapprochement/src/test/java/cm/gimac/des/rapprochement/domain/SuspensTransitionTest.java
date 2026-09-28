package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.SuspensTestSamples.*;
import static cm.gimac.des.rapprochement.domain.SuspensTransitionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SuspensTransitionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SuspensTransition.class);
        SuspensTransition suspensTransition1 = getSuspensTransitionSample1();
        SuspensTransition suspensTransition2 = new SuspensTransition();
        assertThat(suspensTransition1).isNotEqualTo(suspensTransition2);

        suspensTransition2.setId(suspensTransition1.getId());
        assertThat(suspensTransition1).isEqualTo(suspensTransition2);

        suspensTransition2 = getSuspensTransitionSample2();
        assertThat(suspensTransition1).isNotEqualTo(suspensTransition2);
    }

    @Test
    void suspensTest() {
        SuspensTransition suspensTransition = getSuspensTransitionRandomSampleGenerator();
        Suspens suspensBack = getSuspensRandomSampleGenerator();

        suspensTransition.setSuspens(suspensBack);
        assertThat(suspensTransition.getSuspens()).isEqualTo(suspensBack);

        suspensTransition.suspens(null);
        assertThat(suspensTransition.getSuspens()).isNull();
    }
}
