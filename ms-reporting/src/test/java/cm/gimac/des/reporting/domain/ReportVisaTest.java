package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.PeriodicReportTestSamples.*;
import static cm.gimac.des.reporting.domain.ReportVisaTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReportVisaTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReportVisa.class);
        ReportVisa reportVisa1 = getReportVisaSample1();
        ReportVisa reportVisa2 = new ReportVisa();
        assertThat(reportVisa1).isNotEqualTo(reportVisa2);

        reportVisa2.setId(reportVisa1.getId());
        assertThat(reportVisa1).isEqualTo(reportVisa2);

        reportVisa2 = getReportVisaSample2();
        assertThat(reportVisa1).isNotEqualTo(reportVisa2);
    }

    @Test
    void reportTest() {
        ReportVisa reportVisa = getReportVisaRandomSampleGenerator();
        PeriodicReport periodicReportBack = getPeriodicReportRandomSampleGenerator();

        reportVisa.setReport(periodicReportBack);
        assertThat(reportVisa.getReport()).isEqualTo(periodicReportBack);

        reportVisa.report(null);
        assertThat(reportVisa.getReport()).isNull();
    }
}
