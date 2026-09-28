package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceItemDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EvidenceItemDTO.class);
        EvidenceItemDTO evidenceItemDTO1 = new EvidenceItemDTO();
        evidenceItemDTO1.setId(UUID.randomUUID());
        EvidenceItemDTO evidenceItemDTO2 = new EvidenceItemDTO();
        assertThat(evidenceItemDTO1).isNotEqualTo(evidenceItemDTO2);
        evidenceItemDTO2.setId(evidenceItemDTO1.getId());
        assertThat(evidenceItemDTO1).isEqualTo(evidenceItemDTO2);
        evidenceItemDTO2.setId(UUID.randomUUID());
        assertThat(evidenceItemDTO1).isNotEqualTo(evidenceItemDTO2);
        evidenceItemDTO1.setId(null);
        assertThat(evidenceItemDTO1).isNotEqualTo(evidenceItemDTO2);
    }
}
