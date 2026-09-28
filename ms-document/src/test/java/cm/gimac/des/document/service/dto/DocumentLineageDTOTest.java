package cm.gimac.des.document.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentLineageDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DocumentLineageDTO.class);
        DocumentLineageDTO documentLineageDTO1 = new DocumentLineageDTO();
        documentLineageDTO1.setId(UUID.randomUUID());
        DocumentLineageDTO documentLineageDTO2 = new DocumentLineageDTO();
        assertThat(documentLineageDTO1).isNotEqualTo(documentLineageDTO2);
        documentLineageDTO2.setId(documentLineageDTO1.getId());
        assertThat(documentLineageDTO1).isEqualTo(documentLineageDTO2);
        documentLineageDTO2.setId(UUID.randomUUID());
        assertThat(documentLineageDTO1).isNotEqualTo(documentLineageDTO2);
        documentLineageDTO1.setId(null);
        assertThat(documentLineageDTO1).isNotEqualTo(documentLineageDTO2);
    }
}
