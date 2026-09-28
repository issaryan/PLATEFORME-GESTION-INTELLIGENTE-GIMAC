package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.CorrespondentTestSamples.*;
import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CorrespondentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Correspondent.class);
        Correspondent correspondent1 = getCorrespondentSample1();
        Correspondent correspondent2 = new Correspondent();
        assertThat(correspondent1).isNotEqualTo(correspondent2);

        correspondent2.setId(correspondent1.getId());
        assertThat(correspondent1).isEqualTo(correspondent2);

        correspondent2 = getCorrespondentSample2();
        assertThat(correspondent1).isNotEqualTo(correspondent2);
    }

    @Test
    void institutionTest() {
        Correspondent correspondent = getCorrespondentRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        correspondent.setInstitution(institutionBack);
        assertThat(correspondent.getInstitution()).isEqualTo(institutionBack);

        correspondent.institution(null);
        assertThat(correspondent.getInstitution()).isNull();
    }
}
