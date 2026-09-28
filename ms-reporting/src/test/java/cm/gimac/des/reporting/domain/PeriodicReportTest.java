package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.PeriodicReportTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PeriodicReportTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PeriodicReport.class);
        PeriodicReport periodicReport1 = getPeriodicReportSample1();
        PeriodicReport periodicReport2 = new PeriodicReport();
        assertThat(periodicReport1).isNotEqualTo(periodicReport2);

        periodicReport2.setId(periodicReport1.getId());
        assertThat(periodicReport1).isEqualTo(periodicReport2);

        periodicReport2 = getPeriodicReportSample2();
        assertThat(periodicReport1).isNotEqualTo(periodicReport2);
    }
}
