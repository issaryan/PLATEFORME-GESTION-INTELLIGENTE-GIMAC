package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.EvidenceReferenceTestSamples.*;
import static cm.gimac.des.homologation.domain.OperationInstanceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EvidenceReferenceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(EvidenceReference.class);
        EvidenceReference evidenceReference1 = getEvidenceReferenceSample1();
        EvidenceReference evidenceReference2 = new EvidenceReference();
        assertThat(evidenceReference1).isNotEqualTo(evidenceReference2);

        evidenceReference2.setId(evidenceReference1.getId());
        assertThat(evidenceReference1).isEqualTo(evidenceReference2);

        evidenceReference2 = getEvidenceReferenceSample2();
        assertThat(evidenceReference1).isNotEqualTo(evidenceReference2);
    }

    @Test
    void operationTest() {
        EvidenceReference evidenceReference = getEvidenceReferenceRandomSampleGenerator();
        OperationInstance operationInstanceBack = getOperationInstanceRandomSampleGenerator();

        evidenceReference.setOperation(operationInstanceBack);
        assertThat(evidenceReference.getOperation()).isEqualTo(operationInstanceBack);

        evidenceReference.operation(null);
        assertThat(evidenceReference.getOperation()).isNull();
    }
}
