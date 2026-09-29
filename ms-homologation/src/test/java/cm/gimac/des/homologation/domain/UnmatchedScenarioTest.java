package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static cm.gimac.des.homologation.domain.UnmatchedScenarioTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class UnmatchedScenarioTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(UnmatchedScenario.class);
        UnmatchedScenario unmatchedScenario1 = getUnmatchedScenarioSample1();
        UnmatchedScenario unmatchedScenario2 = new UnmatchedScenario();
        assertThat(unmatchedScenario1).isNotEqualTo(unmatchedScenario2);

        unmatchedScenario2.setId(unmatchedScenario1.getId());
        assertThat(unmatchedScenario1).isEqualTo(unmatchedScenario2);

        unmatchedScenario2 = getUnmatchedScenarioSample2();
        assertThat(unmatchedScenario1).isNotEqualTo(unmatchedScenario2);
    }

    @Test
    void liaisonTest() {
        UnmatchedScenario unmatchedScenario = getUnmatchedScenarioRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        unmatchedScenario.setLiaison(liaisonBack);
        assertThat(unmatchedScenario.getLiaison()).isEqualTo(liaisonBack);

        unmatchedScenario.liaison(null);
        assertThat(unmatchedScenario.getLiaison()).isNull();
    }
}
