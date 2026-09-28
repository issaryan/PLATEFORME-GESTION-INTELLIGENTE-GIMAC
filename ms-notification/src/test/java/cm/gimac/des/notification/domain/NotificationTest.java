package cm.gimac.des.notification.domain;

import static cm.gimac.des.notification.domain.ConsolidationWindowTestSamples.*;
import static cm.gimac.des.notification.domain.NotificationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Notification.class);
        Notification notification1 = getNotificationSample1();
        Notification notification2 = new Notification();
        assertThat(notification1).isNotEqualTo(notification2);

        notification2.setId(notification1.getId());
        assertThat(notification1).isEqualTo(notification2);

        notification2 = getNotificationSample2();
        assertThat(notification1).isNotEqualTo(notification2);
    }

    @Test
    void consolidationWindowTest() {
        Notification notification = getNotificationRandomSampleGenerator();
        ConsolidationWindow consolidationWindowBack = getConsolidationWindowRandomSampleGenerator();

        notification.setConsolidationWindow(consolidationWindowBack);
        assertThat(notification.getConsolidationWindow()).isEqualTo(consolidationWindowBack);

        notification.consolidationWindow(null);
        assertThat(notification.getConsolidationWindow()).isNull();
    }

    @Test
    void summarizedWindowTest() {
        Notification notification = getNotificationRandomSampleGenerator();
        ConsolidationWindow consolidationWindowBack = getConsolidationWindowRandomSampleGenerator();

        notification.setSummarizedWindow(consolidationWindowBack);
        assertThat(notification.getSummarizedWindow()).isEqualTo(consolidationWindowBack);
        assertThat(consolidationWindowBack.getSummaryNotification()).isEqualTo(notification);

        notification.summarizedWindow(null);
        assertThat(notification.getSummarizedWindow()).isNull();
        assertThat(consolidationWindowBack.getSummaryNotification()).isNull();
    }
}
