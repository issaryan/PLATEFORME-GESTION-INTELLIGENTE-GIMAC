package cm.gimac.des.participant.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeadlineDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DeadlineDTO.class);
        DeadlineDTO deadlineDTO1 = new DeadlineDTO();
        deadlineDTO1.setId(UUID.randomUUID());
        DeadlineDTO deadlineDTO2 = new DeadlineDTO();
        assertThat(deadlineDTO1).isNotEqualTo(deadlineDTO2);
        deadlineDTO2.setId(deadlineDTO1.getId());
        assertThat(deadlineDTO1).isEqualTo(deadlineDTO2);
        deadlineDTO2.setId(UUID.randomUUID());
        assertThat(deadlineDTO1).isNotEqualTo(deadlineDTO2);
        deadlineDTO1.setId(null);
        assertThat(deadlineDTO1).isNotEqualTo(deadlineDTO2);
    }
}
