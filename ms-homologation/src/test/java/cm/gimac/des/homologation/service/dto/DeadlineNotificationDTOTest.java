package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeadlineNotificationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DeadlineNotificationDTO.class);
        DeadlineNotificationDTO deadlineNotificationDTO1 = new DeadlineNotificationDTO();
        deadlineNotificationDTO1.setId(UUID.randomUUID());
        DeadlineNotificationDTO deadlineNotificationDTO2 = new DeadlineNotificationDTO();
        assertThat(deadlineNotificationDTO1).isNotEqualTo(deadlineNotificationDTO2);
        deadlineNotificationDTO2.setId(deadlineNotificationDTO1.getId());
        assertThat(deadlineNotificationDTO1).isEqualTo(deadlineNotificationDTO2);
        deadlineNotificationDTO2.setId(UUID.randomUUID());
        assertThat(deadlineNotificationDTO1).isNotEqualTo(deadlineNotificationDTO2);
        deadlineNotificationDTO1.setId(null);
        assertThat(deadlineNotificationDTO1).isNotEqualTo(deadlineNotificationDTO2);
    }
}
