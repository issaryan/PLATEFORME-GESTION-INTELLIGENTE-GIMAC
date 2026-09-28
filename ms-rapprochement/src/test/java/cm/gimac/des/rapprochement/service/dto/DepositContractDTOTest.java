package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DepositContractDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DepositContractDTO.class);
        DepositContractDTO depositContractDTO1 = new DepositContractDTO();
        depositContractDTO1.setId(UUID.randomUUID());
        DepositContractDTO depositContractDTO2 = new DepositContractDTO();
        assertThat(depositContractDTO1).isNotEqualTo(depositContractDTO2);
        depositContractDTO2.setId(depositContractDTO1.getId());
        assertThat(depositContractDTO1).isEqualTo(depositContractDTO2);
        depositContractDTO2.setId(UUID.randomUUID());
        assertThat(depositContractDTO1).isNotEqualTo(depositContractDTO2);
        depositContractDTO1.setId(null);
        assertThat(depositContractDTO1).isNotEqualTo(depositContractDTO2);
    }
}
