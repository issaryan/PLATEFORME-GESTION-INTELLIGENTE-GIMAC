package cm.gimac.des.document.domain;

import static cm.gimac.des.document.domain.ConsultationTestSamples.*;
import static cm.gimac.des.document.domain.DocumentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ConsultationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Consultation.class);
        Consultation consultation1 = getConsultationSample1();
        Consultation consultation2 = new Consultation();
        assertThat(consultation1).isNotEqualTo(consultation2);

        consultation2.setId(consultation1.getId());
        assertThat(consultation1).isEqualTo(consultation2);

        consultation2 = getConsultationSample2();
        assertThat(consultation1).isNotEqualTo(consultation2);
    }

    @Test
    void documentTest() {
        Consultation consultation = getConsultationRandomSampleGenerator();
        Document documentBack = getDocumentRandomSampleGenerator();

        consultation.setDocument(documentBack);
        assertThat(consultation.getDocument()).isEqualTo(documentBack);

        consultation.document(null);
        assertThat(consultation.getDocument()).isNull();
    }
}
