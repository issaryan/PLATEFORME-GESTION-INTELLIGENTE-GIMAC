package cm.gimac.des.notification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationSubscriptionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(NotificationSubscriptionDTO.class);
        NotificationSubscriptionDTO notificationSubscriptionDTO1 = new NotificationSubscriptionDTO();
        notificationSubscriptionDTO1.setId(UUID.randomUUID());
        NotificationSubscriptionDTO notificationSubscriptionDTO2 = new NotificationSubscriptionDTO();
        assertThat(notificationSubscriptionDTO1).isNotEqualTo(notificationSubscriptionDTO2);
        notificationSubscriptionDTO2.setId(notificationSubscriptionDTO1.getId());
        assertThat(notificationSubscriptionDTO1).isEqualTo(notificationSubscriptionDTO2);
        notificationSubscriptionDTO2.setId(UUID.randomUUID());
        assertThat(notificationSubscriptionDTO1).isNotEqualTo(notificationSubscriptionDTO2);
        notificationSubscriptionDTO1.setId(null);
        assertThat(notificationSubscriptionDTO1).isNotEqualTo(notificationSubscriptionDTO2);
    }
}
