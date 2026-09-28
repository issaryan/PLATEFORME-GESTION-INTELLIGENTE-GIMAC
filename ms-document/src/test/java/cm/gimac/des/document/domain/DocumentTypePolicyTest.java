package cm.gimac.des.document.domain;

import static cm.gimac.des.document.domain.DocumentTypePolicyTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DocumentTypePolicyTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DocumentTypePolicy.class);
        DocumentTypePolicy documentTypePolicy1 = getDocumentTypePolicySample1();
        DocumentTypePolicy documentTypePolicy2 = new DocumentTypePolicy();
        assertThat(documentTypePolicy1).isNotEqualTo(documentTypePolicy2);

        documentTypePolicy2.setId(documentTypePolicy1.getId());
        assertThat(documentTypePolicy1).isEqualTo(documentTypePolicy2);

        documentTypePolicy2 = getDocumentTypePolicySample2();
        assertThat(documentTypePolicy1).isNotEqualTo(documentTypePolicy2);
    }
}
