package cm.gimac.des.notification.domain;

import static cm.gimac.des.notification.domain.NotificationSubscriptionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class NotificationSubscriptionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(NotificationSubscription.class);
        NotificationSubscription notificationSubscription1 = getNotificationSubscriptionSample1();
        NotificationSubscription notificationSubscription2 = new NotificationSubscription();
        assertThat(notificationSubscription1).isNotEqualTo(notificationSubscription2);

        notificationSubscription2.setId(notificationSubscription1.getId());
        assertThat(notificationSubscription1).isEqualTo(notificationSubscription2);

        notificationSubscription2 = getNotificationSubscriptionSample2();
        assertThat(notificationSubscription1).isNotEqualTo(notificationSubscription2);
    }
}
