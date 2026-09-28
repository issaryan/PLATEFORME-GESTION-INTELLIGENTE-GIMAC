package cm.gimac.des.document.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentTypePolicyDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DocumentTypePolicyDTO.class);
        DocumentTypePolicyDTO documentTypePolicyDTO1 = new DocumentTypePolicyDTO();
        documentTypePolicyDTO1.setId(UUID.randomUUID());
        DocumentTypePolicyDTO documentTypePolicyDTO2 = new DocumentTypePolicyDTO();
        assertThat(documentTypePolicyDTO1).isNotEqualTo(documentTypePolicyDTO2);
        documentTypePolicyDTO2.setId(documentTypePolicyDTO1.getId());
        assertThat(documentTypePolicyDTO1).isEqualTo(documentTypePolicyDTO2);
        documentTypePolicyDTO2.setId(UUID.randomUUID());
        assertThat(documentTypePolicyDTO1).isNotEqualTo(documentTypePolicyDTO2);
        documentTypePolicyDTO1.setId(null);
        assertThat(documentTypePolicyDTO1).isNotEqualTo(documentTypePolicyDTO2);
    }
}
