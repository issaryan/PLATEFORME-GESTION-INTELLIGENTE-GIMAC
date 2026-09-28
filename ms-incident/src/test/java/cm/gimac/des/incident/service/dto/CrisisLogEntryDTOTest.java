package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrisisLogEntryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CrisisLogEntryDTO.class);
        CrisisLogEntryDTO crisisLogEntryDTO1 = new CrisisLogEntryDTO();
        crisisLogEntryDTO1.setId(UUID.randomUUID());
        CrisisLogEntryDTO crisisLogEntryDTO2 = new CrisisLogEntryDTO();
        assertThat(crisisLogEntryDTO1).isNotEqualTo(crisisLogEntryDTO2);
        crisisLogEntryDTO2.setId(crisisLogEntryDTO1.getId());
        assertThat(crisisLogEntryDTO1).isEqualTo(crisisLogEntryDTO2);
        crisisLogEntryDTO2.setId(UUID.randomUUID());
        assertThat(crisisLogEntryDTO1).isNotEqualTo(crisisLogEntryDTO2);
        crisisLogEntryDTO1.setId(null);
        assertThat(crisisLogEntryDTO1).isNotEqualTo(crisisLogEntryDTO2);
    }
}
