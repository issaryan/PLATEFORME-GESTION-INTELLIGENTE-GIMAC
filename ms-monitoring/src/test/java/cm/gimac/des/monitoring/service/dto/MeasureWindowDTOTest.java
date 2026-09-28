package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeasureWindowDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MeasureWindowDTO.class);
        MeasureWindowDTO measureWindowDTO1 = new MeasureWindowDTO();
        measureWindowDTO1.setId(UUID.randomUUID());
        MeasureWindowDTO measureWindowDTO2 = new MeasureWindowDTO();
        assertThat(measureWindowDTO1).isNotEqualTo(measureWindowDTO2);
        measureWindowDTO2.setId(measureWindowDTO1.getId());
        assertThat(measureWindowDTO1).isEqualTo(measureWindowDTO2);
        measureWindowDTO2.setId(UUID.randomUUID());
        assertThat(measureWindowDTO1).isNotEqualTo(measureWindowDTO2);
        measureWindowDTO1.setId(null);
        assertThat(measureWindowDTO1).isNotEqualTo(measureWindowDTO2);
    }
}
