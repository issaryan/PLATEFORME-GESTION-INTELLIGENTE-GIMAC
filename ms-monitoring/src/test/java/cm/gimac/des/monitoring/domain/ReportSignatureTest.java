package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.ReportSignatureTestSamples.*;
import static cm.gimac.des.monitoring.domain.ShiftReportTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReportSignatureTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReportSignature.class);
        ReportSignature reportSignature1 = getReportSignatureSample1();
        ReportSignature reportSignature2 = new ReportSignature();
        assertThat(reportSignature1).isNotEqualTo(reportSignature2);

        reportSignature2.setId(reportSignature1.getId());
        assertThat(reportSignature1).isEqualTo(reportSignature2);

        reportSignature2 = getReportSignatureSample2();
        assertThat(reportSignature1).isNotEqualTo(reportSignature2);
    }

    @Test
    void reportTest() {
        ReportSignature reportSignature = getReportSignatureRandomSampleGenerator();
        ShiftReport shiftReportBack = getShiftReportRandomSampleGenerator();

        reportSignature.setReport(shiftReportBack);
        assertThat(reportSignature.getReport()).isEqualTo(shiftReportBack);

        reportSignature.report(null);
        assertThat(reportSignature.getReport()).isNull();
    }
}
