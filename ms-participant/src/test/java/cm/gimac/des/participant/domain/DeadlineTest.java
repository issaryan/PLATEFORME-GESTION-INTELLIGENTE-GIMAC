package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.ConventionTestSamples.*;
import static cm.gimac.des.participant.domain.DeadlineTestSamples.*;
import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static cm.gimac.des.participant.domain.LegalDocumentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DeadlineTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Deadline.class);
        Deadline deadline1 = getDeadlineSample1();
        Deadline deadline2 = new Deadline();
        assertThat(deadline1).isNotEqualTo(deadline2);

        deadline2.setId(deadline1.getId());
        assertThat(deadline1).isEqualTo(deadline2);

        deadline2 = getDeadlineSample2();
        assertThat(deadline1).isNotEqualTo(deadline2);
    }

    @Test
    void institutionTest() {
        Deadline deadline = getDeadlineRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        deadline.setInstitution(institutionBack);
        assertThat(deadline.getInstitution()).isEqualTo(institutionBack);

        deadline.institution(null);
        assertThat(deadline.getInstitution()).isNull();
    }

    @Test
    void legalDocumentTest() {
        Deadline deadline = getDeadlineRandomSampleGenerator();
        LegalDocument legalDocumentBack = getLegalDocumentRandomSampleGenerator();

        deadline.setLegalDocument(legalDocumentBack);
        assertThat(deadline.getLegalDocument()).isEqualTo(legalDocumentBack);

        deadline.legalDocument(null);
        assertThat(deadline.getLegalDocument()).isNull();
    }

    @Test
    void conventionTest() {
        Deadline deadline = getDeadlineRandomSampleGenerator();
        Convention conventionBack = getConventionRandomSampleGenerator();

        deadline.setConvention(conventionBack);
        assertThat(deadline.getConvention()).isEqualTo(conventionBack);

        deadline.convention(null);
        assertThat(deadline.getConvention()).isNull();
    }
}
