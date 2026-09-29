package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ReferenceOperationTestSamples.*;
import static cm.gimac.des.homologation.domain.ReferentialVersionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReferenceOperationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReferenceOperation.class);
        ReferenceOperation referenceOperation1 = getReferenceOperationSample1();
        ReferenceOperation referenceOperation2 = new ReferenceOperation();
        assertThat(referenceOperation1).isNotEqualTo(referenceOperation2);

        referenceOperation2.setId(referenceOperation1.getId());
        assertThat(referenceOperation1).isEqualTo(referenceOperation2);

        referenceOperation2 = getReferenceOperationSample2();
        assertThat(referenceOperation1).isNotEqualTo(referenceOperation2);
    }

    @Test
    void versionTest() {
        ReferenceOperation referenceOperation = getReferenceOperationRandomSampleGenerator();
        ReferentialVersion referentialVersionBack = getReferentialVersionRandomSampleGenerator();

        referenceOperation.setVersion(referentialVersionBack);
        assertThat(referenceOperation.getVersion()).isEqualTo(referentialVersionBack);

        referenceOperation.version(null);
        assertThat(referenceOperation.getVersion()).isNull();
    }
}
