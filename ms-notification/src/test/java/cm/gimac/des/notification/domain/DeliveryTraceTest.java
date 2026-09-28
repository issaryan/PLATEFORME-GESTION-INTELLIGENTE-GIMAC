package cm.gimac.des.notification.domain;

import static cm.gimac.des.notification.domain.DeliveryTraceTestSamples.*;
import static cm.gimac.des.notification.domain.MessageTemplateTestSamples.*;
import static cm.gimac.des.notification.domain.NotificationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DeliveryTraceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DeliveryTrace.class);
        DeliveryTrace deliveryTrace1 = getDeliveryTraceSample1();
        DeliveryTrace deliveryTrace2 = new DeliveryTrace();
        assertThat(deliveryTrace1).isNotEqualTo(deliveryTrace2);

        deliveryTrace2.setId(deliveryTrace1.getId());
        assertThat(deliveryTrace1).isEqualTo(deliveryTrace2);

        deliveryTrace2 = getDeliveryTraceSample2();
        assertThat(deliveryTrace1).isNotEqualTo(deliveryTrace2);
    }

    @Test
    void notificationTest() {
        DeliveryTrace deliveryTrace = getDeliveryTraceRandomSampleGenerator();
        Notification notificationBack = getNotificationRandomSampleGenerator();

        deliveryTrace.setNotification(notificationBack);
        assertThat(deliveryTrace.getNotification()).isEqualTo(notificationBack);

        deliveryTrace.notification(null);
        assertThat(deliveryTrace.getNotification()).isNull();
    }

    @Test
    void templateTest() {
        DeliveryTrace deliveryTrace = getDeliveryTraceRandomSampleGenerator();
        MessageTemplate messageTemplateBack = getMessageTemplateRandomSampleGenerator();

        deliveryTrace.setTemplate(messageTemplateBack);
        assertThat(deliveryTrace.getTemplate()).isEqualTo(messageTemplateBack);

        deliveryTrace.template(null);
        assertThat(deliveryTrace.getTemplate()).isNull();
    }
}
