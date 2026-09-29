package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScopeItemDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ScopeItemDTO.class);
        ScopeItemDTO scopeItemDTO1 = new ScopeItemDTO();
        scopeItemDTO1.setId(UUID.randomUUID());
        ScopeItemDTO scopeItemDTO2 = new ScopeItemDTO();
        assertThat(scopeItemDTO1).isNotEqualTo(scopeItemDTO2);
        scopeItemDTO2.setId(scopeItemDTO1.getId());
        assertThat(scopeItemDTO1).isEqualTo(scopeItemDTO2);
        scopeItemDTO2.setId(UUID.randomUUID());
        assertThat(scopeItemDTO1).isNotEqualTo(scopeItemDTO2);
        scopeItemDTO1.setId(null);
        assertThat(scopeItemDTO1).isNotEqualTo(scopeItemDTO2);
    }
}
