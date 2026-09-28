package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.ConventionTestSamples.*;
import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ConventionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Convention.class);
        Convention convention1 = getConventionSample1();
        Convention convention2 = new Convention();
        assertThat(convention1).isNotEqualTo(convention2);

        convention2.setId(convention1.getId());
        assertThat(convention1).isEqualTo(convention2);

        convention2 = getConventionSample2();
        assertThat(convention1).isNotEqualTo(convention2);
    }

    @Test
    void institutionTest() {
        Convention convention = getConventionRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        convention.setInstitution(institutionBack);
        assertThat(convention.getInstitution()).isEqualTo(institutionBack);

        convention.institution(null);
        assertThat(convention.getInstitution()).isNull();
    }
}
