package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperationInstanceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OperationInstanceDTO.class);
        OperationInstanceDTO operationInstanceDTO1 = new OperationInstanceDTO();
        operationInstanceDTO1.setId(UUID.randomUUID());
        OperationInstanceDTO operationInstanceDTO2 = new OperationInstanceDTO();
        assertThat(operationInstanceDTO1).isNotEqualTo(operationInstanceDTO2);
        operationInstanceDTO2.setId(operationInstanceDTO1.getId());
        assertThat(operationInstanceDTO1).isEqualTo(operationInstanceDTO2);
        operationInstanceDTO2.setId(UUID.randomUUID());
        assertThat(operationInstanceDTO1).isNotEqualTo(operationInstanceDTO2);
        operationInstanceDTO1.setId(null);
        assertThat(operationInstanceDTO1).isNotEqualTo(operationInstanceDTO2);
    }
}
