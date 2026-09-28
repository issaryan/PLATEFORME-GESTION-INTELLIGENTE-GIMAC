package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OperationDTO.class);
        OperationDTO operationDTO1 = new OperationDTO();
        operationDTO1.setId(UUID.randomUUID());
        OperationDTO operationDTO2 = new OperationDTO();
        assertThat(operationDTO1).isNotEqualTo(operationDTO2);
        operationDTO2.setId(operationDTO1.getId());
        assertThat(operationDTO1).isEqualTo(operationDTO2);
        operationDTO2.setId(UUID.randomUUID());
        assertThat(operationDTO1).isNotEqualTo(operationDTO2);
        operationDTO1.setId(null);
        assertThat(operationDTO1).isNotEqualTo(operationDTO2);
    }
}
