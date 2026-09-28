package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.CrisisCellTestSamples.*;
import static cm.gimac.des.incident.domain.CrisisLogEntryTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CrisisLogEntryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CrisisLogEntry.class);
        CrisisLogEntry crisisLogEntry1 = getCrisisLogEntrySample1();
        CrisisLogEntry crisisLogEntry2 = new CrisisLogEntry();
        assertThat(crisisLogEntry1).isNotEqualTo(crisisLogEntry2);

        crisisLogEntry2.setId(crisisLogEntry1.getId());
        assertThat(crisisLogEntry1).isEqualTo(crisisLogEntry2);

        crisisLogEntry2 = getCrisisLogEntrySample2();
        assertThat(crisisLogEntry1).isNotEqualTo(crisisLogEntry2);
    }

    @Test
    void cellTest() {
        CrisisLogEntry crisisLogEntry = getCrisisLogEntryRandomSampleGenerator();
        CrisisCell crisisCellBack = getCrisisCellRandomSampleGenerator();

        crisisLogEntry.setCell(crisisCellBack);
        assertThat(crisisLogEntry.getCell()).isEqualTo(crisisCellBack);

        crisisLogEntry.cell(null);
        assertThat(crisisLogEntry.getCell()).isNull();
    }
}
