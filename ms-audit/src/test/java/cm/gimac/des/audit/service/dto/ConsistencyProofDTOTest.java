package cm.gimac.des.audit.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsistencyProofDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConsistencyProofDTO.class);
        ConsistencyProofDTO consistencyProofDTO1 = new ConsistencyProofDTO();
        consistencyProofDTO1.setId(UUID.randomUUID());
        ConsistencyProofDTO consistencyProofDTO2 = new ConsistencyProofDTO();
        assertThat(consistencyProofDTO1).isNotEqualTo(consistencyProofDTO2);
        consistencyProofDTO2.setId(consistencyProofDTO1.getId());
        assertThat(consistencyProofDTO1).isEqualTo(consistencyProofDTO2);
        consistencyProofDTO2.setId(UUID.randomUUID());
        assertThat(consistencyProofDTO1).isNotEqualTo(consistencyProofDTO2);
        consistencyProofDTO1.setId(null);
        assertThat(consistencyProofDTO1).isNotEqualTo(consistencyProofDTO2);
    }
}
