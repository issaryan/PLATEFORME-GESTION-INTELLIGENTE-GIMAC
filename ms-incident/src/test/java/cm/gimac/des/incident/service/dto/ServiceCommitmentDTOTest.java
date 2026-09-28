package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServiceCommitmentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ServiceCommitmentDTO.class);
        ServiceCommitmentDTO serviceCommitmentDTO1 = new ServiceCommitmentDTO();
        serviceCommitmentDTO1.setId(UUID.randomUUID());
        ServiceCommitmentDTO serviceCommitmentDTO2 = new ServiceCommitmentDTO();
        assertThat(serviceCommitmentDTO1).isNotEqualTo(serviceCommitmentDTO2);
        serviceCommitmentDTO2.setId(serviceCommitmentDTO1.getId());
        assertThat(serviceCommitmentDTO1).isEqualTo(serviceCommitmentDTO2);
        serviceCommitmentDTO2.setId(UUID.randomUUID());
        assertThat(serviceCommitmentDTO1).isNotEqualTo(serviceCommitmentDTO2);
        serviceCommitmentDTO1.setId(null);
        assertThat(serviceCommitmentDTO1).isNotEqualTo(serviceCommitmentDTO2);
    }
}
