package cm.gimac.des.document.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegrityCheckDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(IntegrityCheckDTO.class);
        IntegrityCheckDTO integrityCheckDTO1 = new IntegrityCheckDTO();
        integrityCheckDTO1.setId(UUID.randomUUID());
        IntegrityCheckDTO integrityCheckDTO2 = new IntegrityCheckDTO();
        assertThat(integrityCheckDTO1).isNotEqualTo(integrityCheckDTO2);
        integrityCheckDTO2.setId(integrityCheckDTO1.getId());
        assertThat(integrityCheckDTO1).isEqualTo(integrityCheckDTO2);
        integrityCheckDTO2.setId(UUID.randomUUID());
        assertThat(integrityCheckDTO1).isNotEqualTo(integrityCheckDTO2);
        integrityCheckDTO1.setId(null);
        assertThat(integrityCheckDTO1).isNotEqualTo(integrityCheckDTO2);
    }
}
