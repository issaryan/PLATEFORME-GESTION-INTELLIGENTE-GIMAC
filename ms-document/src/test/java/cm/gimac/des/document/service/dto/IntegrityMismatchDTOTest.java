package cm.gimac.des.document.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegrityMismatchDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(IntegrityMismatchDTO.class);
        IntegrityMismatchDTO integrityMismatchDTO1 = new IntegrityMismatchDTO();
        integrityMismatchDTO1.setId(UUID.randomUUID());
        IntegrityMismatchDTO integrityMismatchDTO2 = new IntegrityMismatchDTO();
        assertThat(integrityMismatchDTO1).isNotEqualTo(integrityMismatchDTO2);
        integrityMismatchDTO2.setId(integrityMismatchDTO1.getId());
        assertThat(integrityMismatchDTO1).isEqualTo(integrityMismatchDTO2);
        integrityMismatchDTO2.setId(UUID.randomUUID());
        assertThat(integrityMismatchDTO1).isNotEqualTo(integrityMismatchDTO2);
        integrityMismatchDTO1.setId(null);
        assertThat(integrityMismatchDTO1).isNotEqualTo(integrityMismatchDTO2);
    }
}
