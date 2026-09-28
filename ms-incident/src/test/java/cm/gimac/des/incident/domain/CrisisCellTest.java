package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.CrisisCellTestSamples.*;
import static cm.gimac.des.incident.domain.IncidentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CrisisCellTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CrisisCell.class);
        CrisisCell crisisCell1 = getCrisisCellSample1();
        CrisisCell crisisCell2 = new CrisisCell();
        assertThat(crisisCell1).isNotEqualTo(crisisCell2);

        crisisCell2.setId(crisisCell1.getId());
        assertThat(crisisCell1).isEqualTo(crisisCell2);

        crisisCell2 = getCrisisCellSample2();
        assertThat(crisisCell1).isNotEqualTo(crisisCell2);
    }

    @Test
    void incidentTest() {
        CrisisCell crisisCell = getCrisisCellRandomSampleGenerator();
        Incident incidentBack = getIncidentRandomSampleGenerator();

        crisisCell.setIncident(incidentBack);
        assertThat(crisisCell.getIncident()).isEqualTo(incidentBack);

        crisisCell.incident(null);
        assertThat(crisisCell.getIncident()).isNull();
    }
}
