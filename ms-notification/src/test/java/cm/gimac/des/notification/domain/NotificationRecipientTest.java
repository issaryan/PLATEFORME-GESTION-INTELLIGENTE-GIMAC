package cm.gimac.des.notification.domain;

import static cm.gimac.des.notification.domain.NotificationRecipientTestSamples.*;
import static cm.gimac.des.notification.domain.NotificationSubscriptionTestSamples.*;
import static cm.gimac.des.notification.domain.NotificationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class NotificationRecipientTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(NotificationRecipient.class);
        NotificationRecipient notificationRecipient1 = getNotificationRecipientSample1();
        NotificationRecipient notificationRecipient2 = new NotificationRecipient();
        assertThat(notificationRecipient1).isNotEqualTo(notificationRecipient2);

        notificationRecipient2.setId(notificationRecipient1.getId());
        assertThat(notificationRecipient1).isEqualTo(notificationRecipient2);

        notificationRecipient2 = getNotificationRecipientSample2();
        assertThat(notificationRecipient1).isNotEqualTo(notificationRecipient2);
    }

    @Test
    void notificationTest() {
        NotificationRecipient notificationRecipient = getNotificationRecipientRandomSampleGenerator();
        Notification notificationBack = getNotificationRandomSampleGenerator();

        notificationRecipient.setNotification(notificationBack);
        assertThat(notificationRecipient.getNotification()).isEqualTo(notificationBack);

        notificationRecipient.notification(null);
        assertThat(notificationRecipient.getNotification()).isNull();
    }

    @Test
    void sourceSubscriptionTest() {
        NotificationRecipient notificationRecipient = getNotificationRecipientRandomSampleGenerator();
        NotificationSubscription notificationSubscriptionBack = getNotificationSubscriptionRandomSampleGenerator();

        notificationRecipient.setSourceSubscription(notificationSubscriptionBack);
        assertThat(notificationRecipient.getSourceSubscription()).isEqualTo(notificationSubscriptionBack);

        notificationRecipient.sourceSubscription(null);
        assertThat(notificationRecipient.getSourceSubscription()).isNull();
    }
}
