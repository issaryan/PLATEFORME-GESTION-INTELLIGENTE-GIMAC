package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceReferenceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EvidenceReferenceDTO.class);
        EvidenceReferenceDTO evidenceReferenceDTO1 = new EvidenceReferenceDTO();
        evidenceReferenceDTO1.setId(UUID.randomUUID());
        EvidenceReferenceDTO evidenceReferenceDTO2 = new EvidenceReferenceDTO();
        assertThat(evidenceReferenceDTO1).isNotEqualTo(evidenceReferenceDTO2);
        evidenceReferenceDTO2.setId(evidenceReferenceDTO1.getId());
        assertThat(evidenceReferenceDTO1).isEqualTo(evidenceReferenceDTO2);
        evidenceReferenceDTO2.setId(UUID.randomUUID());
        assertThat(evidenceReferenceDTO1).isNotEqualTo(evidenceReferenceDTO2);
        evidenceReferenceDTO1.setId(null);
        assertThat(evidenceReferenceDTO1).isNotEqualTo(evidenceReferenceDTO2);
    }
}
