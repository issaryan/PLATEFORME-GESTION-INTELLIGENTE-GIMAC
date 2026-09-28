package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.OnCallRosterTestSamples.*;
import static cm.gimac.des.monitoring.domain.ShiftReportTestSamples.*;
import static cm.gimac.des.monitoring.domain.ShiftTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ShiftTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Shift.class);
        Shift shift1 = getShiftSample1();
        Shift shift2 = new Shift();
        assertThat(shift1).isNotEqualTo(shift2);

        shift2.setId(shift1.getId());
        assertThat(shift1).isEqualTo(shift2);

        shift2 = getShiftSample2();
        assertThat(shift1).isNotEqualTo(shift2);
    }

    @Test
    void rosterTest() {
        Shift shift = getShiftRandomSampleGenerator();
        OnCallRoster onCallRosterBack = getOnCallRosterRandomSampleGenerator();

        shift.setRoster(onCallRosterBack);
        assertThat(shift.getRoster()).isEqualTo(onCallRosterBack);

        shift.roster(null);
        assertThat(shift.getRoster()).isNull();
    }

    @Test
    void shiftReportTest() {
        Shift shift = getShiftRandomSampleGenerator();
        ShiftReport shiftReportBack = getShiftReportRandomSampleGenerator();

        shift.setShiftReport(shiftReportBack);
        assertThat(shift.getShiftReport()).isEqualTo(shiftReportBack);
        assertThat(shiftReportBack.getShift()).isEqualTo(shift);

        shift.shiftReport(null);
        assertThat(shift.getShiftReport()).isNull();
        assertThat(shiftReportBack.getShift()).isNull();
    }
}
