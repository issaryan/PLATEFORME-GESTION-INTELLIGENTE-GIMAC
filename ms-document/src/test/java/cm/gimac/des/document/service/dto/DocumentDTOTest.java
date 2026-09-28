package cm.gimac.des.document.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DocumentDTO.class);
        DocumentDTO documentDTO1 = new DocumentDTO();
        documentDTO1.setId(UUID.randomUUID());
        DocumentDTO documentDTO2 = new DocumentDTO();
        assertThat(documentDTO1).isNotEqualTo(documentDTO2);
        documentDTO2.setId(documentDTO1.getId());
        assertThat(documentDTO1).isEqualTo(documentDTO2);
        documentDTO2.setId(UUID.randomUUID());
        assertThat(documentDTO1).isNotEqualTo(documentDTO2);
        documentDTO1.setId(null);
        assertThat(documentDTO1).isNotEqualTo(documentDTO2);
    }
}
