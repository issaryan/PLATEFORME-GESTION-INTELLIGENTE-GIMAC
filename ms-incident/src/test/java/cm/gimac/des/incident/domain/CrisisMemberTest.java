package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.CrisisCellTestSamples.*;
import static cm.gimac.des.incident.domain.CrisisMemberTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CrisisMemberTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CrisisMember.class);
        CrisisMember crisisMember1 = getCrisisMemberSample1();
        CrisisMember crisisMember2 = new CrisisMember();
        assertThat(crisisMember1).isNotEqualTo(crisisMember2);

        crisisMember2.setId(crisisMember1.getId());
        assertThat(crisisMember1).isEqualTo(crisisMember2);

        crisisMember2 = getCrisisMemberSample2();
        assertThat(crisisMember1).isNotEqualTo(crisisMember2);
    }

    @Test
    void cellTest() {
        CrisisMember crisisMember = getCrisisMemberRandomSampleGenerator();
        CrisisCell crisisCellBack = getCrisisCellRandomSampleGenerator();

        crisisMember.setCell(crisisCellBack);
        assertThat(crisisMember.getCell()).isEqualTo(crisisCellBack);

        crisisMember.cell(null);
        assertThat(crisisMember.getCell()).isNull();
    }
}
