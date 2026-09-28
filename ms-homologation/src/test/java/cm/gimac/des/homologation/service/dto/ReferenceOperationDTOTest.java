package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReferenceOperationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReferenceOperationDTO.class);
        ReferenceOperationDTO referenceOperationDTO1 = new ReferenceOperationDTO();
        referenceOperationDTO1.setId(UUID.randomUUID());
        ReferenceOperationDTO referenceOperationDTO2 = new ReferenceOperationDTO();
        assertThat(referenceOperationDTO1).isNotEqualTo(referenceOperationDTO2);
        referenceOperationDTO2.setId(referenceOperationDTO1.getId());
        assertThat(referenceOperationDTO1).isEqualTo(referenceOperationDTO2);
        referenceOperationDTO2.setId(UUID.randomUUID());
        assertThat(referenceOperationDTO1).isNotEqualTo(referenceOperationDTO2);
        referenceOperationDTO1.setId(null);
        assertThat(referenceOperationDTO1).isNotEqualTo(referenceOperationDTO2);
    }
}
