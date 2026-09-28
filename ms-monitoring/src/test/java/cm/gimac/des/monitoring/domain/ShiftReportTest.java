package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.ShiftReportTestSamples.*;
import static cm.gimac.des.monitoring.domain.ShiftReportTestSamples.*;
import static cm.gimac.des.monitoring.domain.ShiftTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ShiftReportTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ShiftReport.class);
        ShiftReport shiftReport1 = getShiftReportSample1();
        ShiftReport shiftReport2 = new ShiftReport();
        assertThat(shiftReport1).isNotEqualTo(shiftReport2);

        shiftReport2.setId(shiftReport1.getId());
        assertThat(shiftReport1).isEqualTo(shiftReport2);

        shiftReport2 = getShiftReportSample2();
        assertThat(shiftReport1).isNotEqualTo(shiftReport2);
    }

    @Test
    void shiftTest() {
        ShiftReport shiftReport = getShiftReportRandomSampleGenerator();
        Shift shiftBack = getShiftRandomSampleGenerator();

        shiftReport.setShift(shiftBack);
        assertThat(shiftReport.getShift()).isEqualTo(shiftBack);

        shiftReport.shift(null);
        assertThat(shiftReport.getShift()).isNull();
    }

    @Test
    void supersedesTest() {
        ShiftReport shiftReport = getShiftReportRandomSampleGenerator();
        ShiftReport shiftReportBack = getShiftReportRandomSampleGenerator();

        shiftReport.setSupersedes(shiftReportBack);
        assertThat(shiftReport.getSupersedes()).isEqualTo(shiftReportBack);

        shiftReport.supersedes(null);
        assertThat(shiftReport.getSupersedes()).isNull();
    }

    @Test
    void shiftReportTest() {
        ShiftReport shiftReport = getShiftReportRandomSampleGenerator();
        ShiftReport shiftReportBack = getShiftReportRandomSampleGenerator();

        shiftReport.setShiftReport(shiftReportBack);
        assertThat(shiftReport.getShiftReport()).isEqualTo(shiftReportBack);
        assertThat(shiftReportBack.getSupersedes()).isEqualTo(shiftReport);

        shiftReport.shiftReport(null);
        assertThat(shiftReport.getShiftReport()).isNull();
        assertThat(shiftReportBack.getSupersedes()).isNull();
    }
}
