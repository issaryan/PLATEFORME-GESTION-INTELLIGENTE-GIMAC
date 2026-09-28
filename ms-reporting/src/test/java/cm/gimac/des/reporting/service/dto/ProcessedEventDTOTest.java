package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessedEventDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ProcessedEventDTO.class);
        ProcessedEventDTO processedEventDTO1 = new ProcessedEventDTO();
        processedEventDTO1.setId(UUID.randomUUID());
        ProcessedEventDTO processedEventDTO2 = new ProcessedEventDTO();
        assertThat(processedEventDTO1).isNotEqualTo(processedEventDTO2);
        processedEventDTO2.setId(processedEventDTO1.getId());
        assertThat(processedEventDTO1).isEqualTo(processedEventDTO2);
        processedEventDTO2.setId(UUID.randomUUID());
        assertThat(processedEventDTO1).isNotEqualTo(processedEventDTO2);
        processedEventDTO1.setId(null);
        assertThat(processedEventDTO1).isNotEqualTo(processedEventDTO2);
    }
}
