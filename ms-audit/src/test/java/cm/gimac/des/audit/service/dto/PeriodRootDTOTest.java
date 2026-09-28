package cm.gimac.des.audit.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PeriodRootDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PeriodRootDTO.class);
        PeriodRootDTO periodRootDTO1 = new PeriodRootDTO();
        periodRootDTO1.setId(UUID.randomUUID());
        PeriodRootDTO periodRootDTO2 = new PeriodRootDTO();
        assertThat(periodRootDTO1).isNotEqualTo(periodRootDTO2);
        periodRootDTO2.setId(periodRootDTO1.getId());
        assertThat(periodRootDTO1).isEqualTo(periodRootDTO2);
        periodRootDTO2.setId(UUID.randomUUID());
        assertThat(periodRootDTO1).isNotEqualTo(periodRootDTO2);
        periodRootDTO1.setId(null);
        assertThat(periodRootDTO1).isNotEqualTo(periodRootDTO2);
    }
}
