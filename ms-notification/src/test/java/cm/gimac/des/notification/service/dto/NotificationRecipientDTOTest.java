package cm.gimac.des.notification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationRecipientDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(NotificationRecipientDTO.class);
        NotificationRecipientDTO notificationRecipientDTO1 = new NotificationRecipientDTO();
        notificationRecipientDTO1.setId(UUID.randomUUID());
        NotificationRecipientDTO notificationRecipientDTO2 = new NotificationRecipientDTO();
        assertThat(notificationRecipientDTO1).isNotEqualTo(notificationRecipientDTO2);
        notificationRecipientDTO2.setId(notificationRecipientDTO1.getId());
        assertThat(notificationRecipientDTO1).isEqualTo(notificationRecipientDTO2);
        notificationRecipientDTO2.setId(UUID.randomUUID());
        assertThat(notificationRecipientDTO1).isNotEqualTo(notificationRecipientDTO2);
        notificationRecipientDTO1.setId(null);
        assertThat(notificationRecipientDTO1).isNotEqualTo(notificationRecipientDTO2);
    }
}
