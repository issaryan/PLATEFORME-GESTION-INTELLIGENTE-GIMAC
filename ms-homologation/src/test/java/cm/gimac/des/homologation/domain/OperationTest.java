package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static cm.gimac.des.homologation.domain.OperationTestSamples.*;
import static cm.gimac.des.homologation.domain.ReferenceOperationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OperationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Operation.class);
        Operation operation1 = getOperationSample1();
        Operation operation2 = new Operation();
        assertThat(operation1).isNotEqualTo(operation2);

        operation2.setId(operation1.getId());
        assertThat(operation1).isEqualTo(operation2);

        operation2 = getOperationSample2();
        assertThat(operation1).isNotEqualTo(operation2);
    }

    @Test
    void liaisonTest() {
        Operation operation = getOperationRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        operation.setLiaison(liaisonBack);
        assertThat(operation.getLiaison()).isEqualTo(liaisonBack);

        operation.liaison(null);
        assertThat(operation.getLiaison()).isNull();
    }

    @Test
    void referenceTest() {
        Operation operation = getOperationRandomSampleGenerator();
        ReferenceOperation referenceOperationBack = getReferenceOperationRandomSampleGenerator();

        operation.setReference(referenceOperationBack);
        assertThat(operation.getReference()).isEqualTo(referenceOperationBack);

        operation.reference(null);
        assertThat(operation.getReference()).isNull();
    }
}
