package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FactDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(FactDTO.class);
        FactDTO factDTO1 = new FactDTO();
        factDTO1.setId(UUID.randomUUID());
        FactDTO factDTO2 = new FactDTO();
        assertThat(factDTO1).isNotEqualTo(factDTO2);
        factDTO2.setId(factDTO1.getId());
        assertThat(factDTO1).isEqualTo(factDTO2);
        factDTO2.setId(UUID.randomUUID());
        assertThat(factDTO1).isNotEqualTo(factDTO2);
        factDTO1.setId(null);
        assertThat(factDTO1).isNotEqualTo(factDTO2);
    }
}
