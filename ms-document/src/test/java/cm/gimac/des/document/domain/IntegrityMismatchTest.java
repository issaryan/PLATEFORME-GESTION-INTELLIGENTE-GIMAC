package cm.gimac.des.document.domain;

import static cm.gimac.des.document.domain.DocumentTestSamples.*;
import static cm.gimac.des.document.domain.IntegrityCheckTestSamples.*;
import static cm.gimac.des.document.domain.IntegrityMismatchTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class IntegrityMismatchTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(IntegrityMismatch.class);
        IntegrityMismatch integrityMismatch1 = getIntegrityMismatchSample1();
        IntegrityMismatch integrityMismatch2 = new IntegrityMismatch();
        assertThat(integrityMismatch1).isNotEqualTo(integrityMismatch2);

        integrityMismatch2.setId(integrityMismatch1.getId());
        assertThat(integrityMismatch1).isEqualTo(integrityMismatch2);

        integrityMismatch2 = getIntegrityMismatchSample2();
        assertThat(integrityMismatch1).isNotEqualTo(integrityMismatch2);
    }

    @Test
    void integrityCheckTest() {
        IntegrityMismatch integrityMismatch = getIntegrityMismatchRandomSampleGenerator();
        IntegrityCheck integrityCheckBack = getIntegrityCheckRandomSampleGenerator();

        integrityMismatch.setIntegrityCheck(integrityCheckBack);
        assertThat(integrityMismatch.getIntegrityCheck()).isEqualTo(integrityCheckBack);

        integrityMismatch.integrityCheck(null);
        assertThat(integrityMismatch.getIntegrityCheck()).isNull();
    }

    @Test
    void documentTest() {
        IntegrityMismatch integrityMismatch = getIntegrityMismatchRandomSampleGenerator();
        Document documentBack = getDocumentRandomSampleGenerator();

        integrityMismatch.setDocument(documentBack);
        assertThat(integrityMismatch.getDocument()).isEqualTo(documentBack);

        integrityMismatch.document(null);
        assertThat(integrityMismatch.getDocument()).isNull();
    }
}
