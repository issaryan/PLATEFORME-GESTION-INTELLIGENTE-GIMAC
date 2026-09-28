package cm.gimac.des.iam.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DelegationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DelegationDTO.class);
        DelegationDTO delegationDTO1 = new DelegationDTO();
        delegationDTO1.setId(UUID.randomUUID());
        DelegationDTO delegationDTO2 = new DelegationDTO();
        assertThat(delegationDTO1).isNotEqualTo(delegationDTO2);
        delegationDTO2.setId(delegationDTO1.getId());
        assertThat(delegationDTO1).isEqualTo(delegationDTO2);
        delegationDTO2.setId(UUID.randomUUID());
        assertThat(delegationDTO1).isNotEqualTo(delegationDTO2);
        delegationDTO1.setId(null);
        assertThat(delegationDTO1).isNotEqualTo(delegationDTO2);
    }
}
