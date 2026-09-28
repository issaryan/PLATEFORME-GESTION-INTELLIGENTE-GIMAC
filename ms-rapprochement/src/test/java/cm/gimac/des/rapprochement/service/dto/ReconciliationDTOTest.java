package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReconciliationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReconciliationDTO.class);
        ReconciliationDTO reconciliationDTO1 = new ReconciliationDTO();
        reconciliationDTO1.setId(UUID.randomUUID());
        ReconciliationDTO reconciliationDTO2 = new ReconciliationDTO();
        assertThat(reconciliationDTO1).isNotEqualTo(reconciliationDTO2);
        reconciliationDTO2.setId(reconciliationDTO1.getId());
        assertThat(reconciliationDTO1).isEqualTo(reconciliationDTO2);
        reconciliationDTO2.setId(UUID.randomUUID());
        assertThat(reconciliationDTO1).isNotEqualTo(reconciliationDTO2);
        reconciliationDTO1.setId(null);
        assertThat(reconciliationDTO1).isNotEqualTo(reconciliationDTO2);
    }
}
