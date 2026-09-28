package cm.gimac.des.audit.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InclusionProofDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(InclusionProofDTO.class);
        InclusionProofDTO inclusionProofDTO1 = new InclusionProofDTO();
        inclusionProofDTO1.setId(UUID.randomUUID());
        InclusionProofDTO inclusionProofDTO2 = new InclusionProofDTO();
        assertThat(inclusionProofDTO1).isNotEqualTo(inclusionProofDTO2);
        inclusionProofDTO2.setId(inclusionProofDTO1.getId());
        assertThat(inclusionProofDTO1).isEqualTo(inclusionProofDTO2);
        inclusionProofDTO2.setId(UUID.randomUUID());
        assertThat(inclusionProofDTO1).isNotEqualTo(inclusionProofDTO2);
        inclusionProofDTO1.setId(null);
        assertThat(inclusionProofDTO1).isNotEqualTo(inclusionProofDTO2);
    }
}
