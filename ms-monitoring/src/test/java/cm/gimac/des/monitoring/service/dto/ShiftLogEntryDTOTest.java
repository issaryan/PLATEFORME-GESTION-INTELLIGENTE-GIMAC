package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftLogEntryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ShiftLogEntryDTO.class);
        ShiftLogEntryDTO shiftLogEntryDTO1 = new ShiftLogEntryDTO();
        shiftLogEntryDTO1.setId(UUID.randomUUID());
        ShiftLogEntryDTO shiftLogEntryDTO2 = new ShiftLogEntryDTO();
        assertThat(shiftLogEntryDTO1).isNotEqualTo(shiftLogEntryDTO2);
        shiftLogEntryDTO2.setId(shiftLogEntryDTO1.getId());
        assertThat(shiftLogEntryDTO1).isEqualTo(shiftLogEntryDTO2);
        shiftLogEntryDTO2.setId(UUID.randomUUID());
        assertThat(shiftLogEntryDTO1).isNotEqualTo(shiftLogEntryDTO2);
        shiftLogEntryDTO1.setId(null);
        assertThat(shiftLogEntryDTO1).isNotEqualTo(shiftLogEntryDTO2);
    }
}
