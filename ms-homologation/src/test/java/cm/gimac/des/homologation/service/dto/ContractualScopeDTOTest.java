package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContractualScopeDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ContractualScopeDTO.class);
        ContractualScopeDTO contractualScopeDTO1 = new ContractualScopeDTO();
        contractualScopeDTO1.setId(UUID.randomUUID());
        ContractualScopeDTO contractualScopeDTO2 = new ContractualScopeDTO();
        assertThat(contractualScopeDTO1).isNotEqualTo(contractualScopeDTO2);
        contractualScopeDTO2.setId(contractualScopeDTO1.getId());
        assertThat(contractualScopeDTO1).isEqualTo(contractualScopeDTO2);
        contractualScopeDTO2.setId(UUID.randomUUID());
        assertThat(contractualScopeDTO1).isNotEqualTo(contractualScopeDTO2);
        contractualScopeDTO1.setId(null);
        assertThat(contractualScopeDTO1).isNotEqualTo(contractualScopeDTO2);
    }
}
