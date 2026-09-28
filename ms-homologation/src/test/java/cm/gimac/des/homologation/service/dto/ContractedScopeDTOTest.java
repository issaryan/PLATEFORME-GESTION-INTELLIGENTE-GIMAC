package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContractedScopeDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ContractedScopeDTO.class);
        ContractedScopeDTO contractedScopeDTO1 = new ContractedScopeDTO();
        contractedScopeDTO1.setId(UUID.randomUUID());
        ContractedScopeDTO contractedScopeDTO2 = new ContractedScopeDTO();
        assertThat(contractedScopeDTO1).isNotEqualTo(contractedScopeDTO2);
        contractedScopeDTO2.setId(contractedScopeDTO1.getId());
        assertThat(contractedScopeDTO1).isEqualTo(contractedScopeDTO2);
        contractedScopeDTO2.setId(UUID.randomUUID());
        assertThat(contractedScopeDTO1).isNotEqualTo(contractedScopeDTO2);
        contractedScopeDTO1.setId(null);
        assertThat(contractedScopeDTO1).isNotEqualTo(contractedScopeDTO2);
    }
}
