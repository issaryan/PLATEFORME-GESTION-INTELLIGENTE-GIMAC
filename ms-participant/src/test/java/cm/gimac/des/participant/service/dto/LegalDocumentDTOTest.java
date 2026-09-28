package cm.gimac.des.participant.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LegalDocumentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(LegalDocumentDTO.class);
        LegalDocumentDTO legalDocumentDTO1 = new LegalDocumentDTO();
        legalDocumentDTO1.setId(UUID.randomUUID());
        LegalDocumentDTO legalDocumentDTO2 = new LegalDocumentDTO();
        assertThat(legalDocumentDTO1).isNotEqualTo(legalDocumentDTO2);
        legalDocumentDTO2.setId(legalDocumentDTO1.getId());
        assertThat(legalDocumentDTO1).isEqualTo(legalDocumentDTO2);
        legalDocumentDTO2.setId(UUID.randomUUID());
        assertThat(legalDocumentDTO1).isNotEqualTo(legalDocumentDTO2);
        legalDocumentDTO1.setId(null);
        assertThat(legalDocumentDTO1).isNotEqualTo(legalDocumentDTO2);
    }
}
