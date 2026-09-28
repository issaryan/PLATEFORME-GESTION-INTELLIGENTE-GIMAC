package cm.gimac.des.notification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsolidationWindowDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConsolidationWindowDTO.class);
        ConsolidationWindowDTO consolidationWindowDTO1 = new ConsolidationWindowDTO();
        consolidationWindowDTO1.setId(UUID.randomUUID());
        ConsolidationWindowDTO consolidationWindowDTO2 = new ConsolidationWindowDTO();
        assertThat(consolidationWindowDTO1).isNotEqualTo(consolidationWindowDTO2);
        consolidationWindowDTO2.setId(consolidationWindowDTO1.getId());
        assertThat(consolidationWindowDTO1).isEqualTo(consolidationWindowDTO2);
        consolidationWindowDTO2.setId(UUID.randomUUID());
        assertThat(consolidationWindowDTO1).isNotEqualTo(consolidationWindowDTO2);
        consolidationWindowDTO1.setId(null);
        assertThat(consolidationWindowDTO1).isNotEqualTo(consolidationWindowDTO2);
    }
}
