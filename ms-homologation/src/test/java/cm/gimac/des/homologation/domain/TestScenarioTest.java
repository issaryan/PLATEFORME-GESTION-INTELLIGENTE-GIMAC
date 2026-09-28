package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static cm.gimac.des.homologation.domain.OperationTestSamples.*;
import static cm.gimac.des.homologation.domain.TestScenarioTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TestScenarioTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(TestScenario.class);
        TestScenario testScenario1 = getTestScenarioSample1();
        TestScenario testScenario2 = new TestScenario();
        assertThat(testScenario1).isNotEqualTo(testScenario2);

        testScenario2.setId(testScenario1.getId());
        assertThat(testScenario1).isEqualTo(testScenario2);

        testScenario2 = getTestScenarioSample2();
        assertThat(testScenario1).isNotEqualTo(testScenario2);
    }

    @Test
    void liaisonTest() {
        TestScenario testScenario = getTestScenarioRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        testScenario.setLiaison(liaisonBack);
        assertThat(testScenario.getLiaison()).isEqualTo(liaisonBack);

        testScenario.liaison(null);
        assertThat(testScenario.getLiaison()).isNull();
    }

    @Test
    void operationTest() {
        TestScenario testScenario = getTestScenarioRandomSampleGenerator();
        Operation operationBack = getOperationRandomSampleGenerator();

        testScenario.setOperation(operationBack);
        assertThat(testScenario.getOperation()).isEqualTo(operationBack);

        testScenario.operation(null);
        assertThat(testScenario.getOperation()).isNull();
    }
}
