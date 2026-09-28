package cm.gimac.des.document.domain;

import static cm.gimac.des.document.domain.DocumentLineageTestSamples.*;
import static cm.gimac.des.document.domain.DocumentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DocumentLineageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DocumentLineage.class);
        DocumentLineage documentLineage1 = getDocumentLineageSample1();
        DocumentLineage documentLineage2 = new DocumentLineage();
        assertThat(documentLineage1).isNotEqualTo(documentLineage2);

        documentLineage2.setId(documentLineage1.getId());
        assertThat(documentLineage1).isEqualTo(documentLineage2);

        documentLineage2 = getDocumentLineageSample2();
        assertThat(documentLineage1).isNotEqualTo(documentLineage2);
    }

    @Test
    void currentDocumentTest() {
        DocumentLineage documentLineage = getDocumentLineageRandomSampleGenerator();
        Document documentBack = getDocumentRandomSampleGenerator();

        documentLineage.setCurrentDocument(documentBack);
        assertThat(documentLineage.getCurrentDocument()).isEqualTo(documentBack);

        documentLineage.currentDocument(null);
        assertThat(documentLineage.getCurrentDocument()).isNull();
    }
}
