package cm.gimac.des.document.domain;

import static cm.gimac.des.document.domain.DocumentLineageTestSamples.*;
import static cm.gimac.des.document.domain.DocumentTestSamples.*;
import static cm.gimac.des.document.domain.DocumentTypePolicyTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DocumentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Document.class);
        Document document1 = getDocumentSample1();
        Document document2 = new Document();
        assertThat(document1).isNotEqualTo(document2);

        document2.setId(document1.getId());
        assertThat(document1).isEqualTo(document2);

        document2 = getDocumentSample2();
        assertThat(document1).isNotEqualTo(document2);
    }

    @Test
    void lineageTest() {
        Document document = getDocumentRandomSampleGenerator();
        DocumentLineage documentLineageBack = getDocumentLineageRandomSampleGenerator();

        document.setLineage(documentLineageBack);
        assertThat(document.getLineage()).isEqualTo(documentLineageBack);

        document.lineage(null);
        assertThat(document.getLineage()).isNull();
    }

    @Test
    void policyTest() {
        Document document = getDocumentRandomSampleGenerator();
        DocumentTypePolicy documentTypePolicyBack = getDocumentTypePolicyRandomSampleGenerator();

        document.setPolicy(documentTypePolicyBack);
        assertThat(document.getPolicy()).isEqualTo(documentTypePolicyBack);

        document.policy(null);
        assertThat(document.getPolicy()).isNull();
    }

    @Test
    void documentLineageTest() {
        Document document = getDocumentRandomSampleGenerator();
        DocumentLineage documentLineageBack = getDocumentLineageRandomSampleGenerator();

        document.setDocumentLineage(documentLineageBack);
        assertThat(document.getDocumentLineage()).isEqualTo(documentLineageBack);
        assertThat(documentLineageBack.getCurrentDocument()).isEqualTo(document);

        document.documentLineage(null);
        assertThat(document.getDocumentLineage()).isNull();
        assertThat(documentLineageBack.getCurrentDocument()).isNull();
    }
}
