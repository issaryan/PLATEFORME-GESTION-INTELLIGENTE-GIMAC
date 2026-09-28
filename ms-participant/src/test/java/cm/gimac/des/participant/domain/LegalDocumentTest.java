package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static cm.gimac.des.participant.domain.LegalDocumentTestSamples.*;
import static cm.gimac.des.participant.domain.LegalDocumentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LegalDocumentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(LegalDocument.class);
        LegalDocument legalDocument1 = getLegalDocumentSample1();
        LegalDocument legalDocument2 = new LegalDocument();
        assertThat(legalDocument1).isNotEqualTo(legalDocument2);

        legalDocument2.setId(legalDocument1.getId());
        assertThat(legalDocument1).isEqualTo(legalDocument2);

        legalDocument2 = getLegalDocumentSample2();
        assertThat(legalDocument1).isNotEqualTo(legalDocument2);
    }

    @Test
    void previousDocumentTest() {
        LegalDocument legalDocument = getLegalDocumentRandomSampleGenerator();
        LegalDocument legalDocumentBack = getLegalDocumentRandomSampleGenerator();

        legalDocument.setPreviousDocument(legalDocumentBack);
        assertThat(legalDocument.getPreviousDocument()).isEqualTo(legalDocumentBack);

        legalDocument.previousDocument(null);
        assertThat(legalDocument.getPreviousDocument()).isNull();
    }

    @Test
    void institutionTest() {
        LegalDocument legalDocument = getLegalDocumentRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        legalDocument.setInstitution(institutionBack);
        assertThat(legalDocument.getInstitution()).isEqualTo(institutionBack);

        legalDocument.institution(null);
        assertThat(legalDocument.getInstitution()).isNull();
    }

    @Test
    void legalDocumentTest() {
        LegalDocument legalDocument = getLegalDocumentRandomSampleGenerator();
        LegalDocument legalDocumentBack = getLegalDocumentRandomSampleGenerator();

        legalDocument.setLegalDocument(legalDocumentBack);
        assertThat(legalDocument.getLegalDocument()).isEqualTo(legalDocumentBack);
        assertThat(legalDocumentBack.getPreviousDocument()).isEqualTo(legalDocument);

        legalDocument.legalDocument(null);
        assertThat(legalDocument.getLegalDocument()).isNull();
        assertThat(legalDocumentBack.getPreviousDocument()).isNull();
    }
}
