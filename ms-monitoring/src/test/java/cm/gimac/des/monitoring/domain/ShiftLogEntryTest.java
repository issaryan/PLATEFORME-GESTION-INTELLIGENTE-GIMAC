package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.ShiftLogEntryTestSamples.*;
import static cm.gimac.des.monitoring.domain.ShiftLogEntryTestSamples.*;
import static cm.gimac.des.monitoring.domain.ShiftTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ShiftLogEntryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ShiftLogEntry.class);
        ShiftLogEntry shiftLogEntry1 = getShiftLogEntrySample1();
        ShiftLogEntry shiftLogEntry2 = new ShiftLogEntry();
        assertThat(shiftLogEntry1).isNotEqualTo(shiftLogEntry2);

        shiftLogEntry2.setId(shiftLogEntry1.getId());
        assertThat(shiftLogEntry1).isEqualTo(shiftLogEntry2);

        shiftLogEntry2 = getShiftLogEntrySample2();
        assertThat(shiftLogEntry1).isNotEqualTo(shiftLogEntry2);
    }

    @Test
    void shiftTest() {
        ShiftLogEntry shiftLogEntry = getShiftLogEntryRandomSampleGenerator();
        Shift shiftBack = getShiftRandomSampleGenerator();

        shiftLogEntry.setShift(shiftBack);
        assertThat(shiftLogEntry.getShift()).isEqualTo(shiftBack);

        shiftLogEntry.shift(null);
        assertThat(shiftLogEntry.getShift()).isNull();
    }

    @Test
    void correctsTest() {
        ShiftLogEntry shiftLogEntry = getShiftLogEntryRandomSampleGenerator();
        ShiftLogEntry shiftLogEntryBack = getShiftLogEntryRandomSampleGenerator();

        shiftLogEntry.setCorrects(shiftLogEntryBack);
        assertThat(shiftLogEntry.getCorrects()).isEqualTo(shiftLogEntryBack);

        shiftLogEntry.corrects(null);
        assertThat(shiftLogEntry.getCorrects()).isNull();
    }
}
