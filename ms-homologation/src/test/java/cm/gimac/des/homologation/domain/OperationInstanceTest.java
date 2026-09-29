package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static cm.gimac.des.homologation.domain.OperationInstanceTestSamples.*;
import static cm.gimac.des.homologation.domain.ReferenceOperationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OperationInstanceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OperationInstance.class);
        OperationInstance operationInstance1 = getOperationInstanceSample1();
        OperationInstance operationInstance2 = new OperationInstance();
        assertThat(operationInstance1).isNotEqualTo(operationInstance2);

        operationInstance2.setId(operationInstance1.getId());
        assertThat(operationInstance1).isEqualTo(operationInstance2);

        operationInstance2 = getOperationInstanceSample2();
        assertThat(operationInstance1).isNotEqualTo(operationInstance2);
    }

    @Test
    void liaisonTest() {
        OperationInstance operationInstance = getOperationInstanceRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        operationInstance.setLiaison(liaisonBack);
        assertThat(operationInstance.getLiaison()).isEqualTo(liaisonBack);

        operationInstance.liaison(null);
        assertThat(operationInstance.getLiaison()).isNull();
    }

    @Test
    void referenceOperationTest() {
        OperationInstance operationInstance = getOperationInstanceRandomSampleGenerator();
        ReferenceOperation referenceOperationBack = getReferenceOperationRandomSampleGenerator();

        operationInstance.setReferenceOperation(referenceOperationBack);
        assertThat(operationInstance.getReferenceOperation()).isEqualTo(referenceOperationBack);

        operationInstance.referenceOperation(null);
        assertThat(operationInstance.getReferenceOperation()).isNull();
    }
}
