package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConciliationEntryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConciliationEntryDTO.class);
        ConciliationEntryDTO conciliationEntryDTO1 = new ConciliationEntryDTO();
        conciliationEntryDTO1.setId(UUID.randomUUID());
        ConciliationEntryDTO conciliationEntryDTO2 = new ConciliationEntryDTO();
        assertThat(conciliationEntryDTO1).isNotEqualTo(conciliationEntryDTO2);
        conciliationEntryDTO2.setId(conciliationEntryDTO1.getId());
        assertThat(conciliationEntryDTO1).isEqualTo(conciliationEntryDTO2);
        conciliationEntryDTO2.setId(UUID.randomUUID());
        assertThat(conciliationEntryDTO1).isNotEqualTo(conciliationEntryDTO2);
        conciliationEntryDTO1.setId(null);
        assertThat(conciliationEntryDTO1).isNotEqualTo(conciliationEntryDTO2);
    }
}
